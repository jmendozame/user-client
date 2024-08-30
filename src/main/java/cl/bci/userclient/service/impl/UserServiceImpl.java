package cl.bci.userclient.service.impl;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.bci.userclient.constantes.UserStatus;
import cl.bci.userclient.dto.PhoneDTO;
import cl.bci.userclient.dto.UserDTO;
import cl.bci.userclient.dto.UserDTOWithoutToken;
import cl.bci.userclient.model.Phone;
import cl.bci.userclient.model.User;
import cl.bci.userclient.repository.IPhoneRepository;
import cl.bci.userclient.repository.IUserRepository;
import cl.bci.userclient.service.IUserService;

@Service
public class UserServiceImpl implements IUserService {

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private IPhoneRepository phoneRepository;

	@Value("${password.regex}")
	private String passwordRegex;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Value("${url.login}")
	private String oauthTokenUrl;

	@Value("${client.credentials}")
	private String clientCredentials;

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, Object>> findByActive() {
		Map<String, Object> response = new HashMap<>();
		List<User> userList = userRepository.findByActive(UserStatus.ON);
		if (!userList.isEmpty()) {
			List<UserDTOWithoutToken> usersDTO = userList.stream().map(this::convertToDTOWithoutToken)
					.collect(Collectors.toList());
			response.put("usuarios", usersDTO);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} else {
			response.put("mensaje", "¡No hay usuarios ingresados!");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@Override
	public ResponseEntity<Map<String, Object>> create(@Valid User user, BindingResult result) {
		Map<String, Object> response = new HashMap<>();
		User newUser;

		if (result.hasErrors()) {
			handleValidationErrors(result, response);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		if (userRepository.existsByEmailAndActive(user.getEmail(), UserStatus.ON)) {
			response.put("mensaje", "El correo electrónico ya está registrado");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		// Verificar si el correo ya está registrado pero inactivo
		User existingUser = userRepository.findByEmail(user.getEmail());
		if (existingUser != null && existingUser.getActive() == UserStatus.OFF) {
			existingUser.setActive(UserStatus.ON);
			existingUser.setName(user.getName());
			existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
			updatePhones(user, existingUser); // Actualizar los teléfonos
			existingUser.setModificationDate(new Date());

			newUser = userRepository.save(existingUser);
		} else {
			// Crear un nuevo usuario si no existe
			String unencryptedPassword = user.getPassword();
			newUser = saveUser(user);
			user.setToken(callOAuthTokenEndpoint(newUser.getEmail(), unencryptedPassword));
			newUser = userRepository.save(user);
		}

		response.put("mensaje", "¡El usuario ha sido creado con éxito!");
		response.put("usuario", convertToDTO(newUser));
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@Transactional
	public User saveUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		if (user.getPhones() != null && !user.getPhones().isEmpty()) {
			for (Phone phone : user.getPhones()) {
				phoneRepository.save(phone);
			}
		}
		User newUser = userRepository.save(user);
		return newUser;
	}

	private void handleValidationErrors(BindingResult result, Map<String, Object> response) {
		Map<String, String> errores = result.getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
		response.put("mensaje", errores);
	}

	// TODO Se le puede agregar un bad request
	@Override
	@Transactional
	public ResponseEntity<Map<String, Object>> update(@Valid User user, BindingResult result) {
		Map<String, Object> response = new HashMap<>();
		User userUpdate;
		try {
			if (result.hasErrors()) {
				handleValidationErrors(result, response);
				return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
			}
			userUpdate = updateUser(user);
			if (userUpdate == null) {
				response.put("mensaje", "Usuario no encontrado");
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			response.put("mensaje", "Error al actualizar en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("mensaje", "¡El usuario ha sido actualizado con éxito!");
		response.put("usuario", convertToDTOWithoutToken(userUpdate));
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	private User updateUser(User user) {
		User userUpdate = userRepository.findByIdAndActive(user.getId(), UserStatus.ON);
		if (userUpdate != null) {
			userUpdate.setName(user.getName());
			userUpdate.setEmail(user.getEmail());
			updatePhones(user, userUpdate);
			userUpdate.setModificationDate(new Date());
			userUpdate = userRepository.save(userUpdate);
		}
		return userUpdate;
	}

	private void updatePhones(User user, User userUpdate) {
		List<Phone> updatedPhones = user.getPhones();
		if (updatedPhones != null && !updatedPhones.isEmpty()) {
			userUpdate.getPhones().clear();
			userUpdate.getPhones()
					.addAll(updatedPhones.stream().map(phoneRepository::save).collect(Collectors.toList()));
		}
	}

	@Override
	public ResponseEntity<Map<String, Object>> updatePassword(Long userId, String newPassword) {
		Map<String, Object> response = new HashMap<>();
		User userUpdatePass;
		if (!newPassword.matches(passwordRegex)) {
			response.put("mensaje", "El formato de la nueva contraseña no es válido");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		try {
			userUpdatePass = updateIdPassword(userId, newPassword);
			if (userUpdatePass == null) {
				response.put("mensaje", "Usuario no encontrado");
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			response.put("mensaje", "¡Error al actualizar la contraseña en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("mensaje", "¡Contraseña actualizada con éxito!");
		response.put("usuario", convertToDTOWithoutToken(userUpdatePass));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private User updateIdPassword(Long userId, String newPassword) {
		User user = userRepository.findByIdAndActive(userId, UserStatus.ON);
		if (user != null) {
			user.setPassword(passwordEncoder.encode(newPassword));
			user.setModificationDate(new Date());
			userRepository.save(user);
		}
		return user;
	}

	@Override
	public ResponseEntity<Map<String, Object>> delete(Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			User user = userRepository.findByIdAndActive(id, UserStatus.ON);
			if (user != null) {
				user.setActive(UserStatus.OFF);
				user.setModificationDate(new Date());
				userRepository.save(user);
				response.put("mensaje", "¡El usuario ha sido eliminado con éxito!");
			} else {
				response.put("mensaje", "El usuario no existe");
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
			response.put("mensaje", "Error al eliminar lógicamente el usuario");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}

	// Métodos de conversión entre DTOs y entidades
	private UserDTO convertToDTO(User user) {
		UserDTO userDTO = new UserDTO();
		userDTO.setId(user.getId());
		userDTO.setNombre(user.getName());
		userDTO.setCorreo(user.getEmail());
		userDTO.setContraseña(user.getPassword());
		userDTO.setTelefonos(convertPhonesToDTO(user.getPhones()));
		userDTO.setToken(user.getToken());
		return userDTO;
	}

	private UserDTOWithoutToken convertToDTOWithoutToken(User user) {
		UserDTOWithoutToken userDTO = new UserDTOWithoutToken();
		userDTO.setId(user.getId());
		userDTO.setNombre(user.getName());
		userDTO.setCorreo(user.getEmail());
		userDTO.setContraseña(user.getPassword());
		userDTO.setTelefonos(convertPhonesToDTO(user.getPhones()));
		return userDTO;
	}

	private List<PhoneDTO> convertPhonesToDTO(List<Phone> phones) {
		return phones.stream().map(phone -> {
			PhoneDTO phoneDTO = new PhoneDTO();
			phoneDTO.setNumero(phone.getPhoneNumber());
			phoneDTO.setCodigoCiudad(phone.getCityCode());
			phoneDTO.setCodigoPais(phone.getCountryCode());
			return phoneDTO;
		}).collect(Collectors.toList());
	}

	private String callOAuthTokenEndpoint(String email, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// Configura los parámetros en el cuerpo de la solicitud
		MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
		String encodedCredentials = Base64.getEncoder().encodeToString(clientCredentials.getBytes());
		headers.set("Authorization", "Basic " + encodedCredentials);
		bodyParams.add("username", email);
		bodyParams.add("password", password);
		bodyParams.add("grant_type", "password");

		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyParams, headers);

		// Realiza la llamada al endpoint
		ResponseEntity<String> responseEntity = restTemplate.postForEntity(oauthTokenUrl, requestEntity, String.class);
		return extractAccessToken(responseEntity.getBody());

	}

	private String extractAccessToken(String responseBody) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(responseBody);
			return jsonNode.get("access_token").asText();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return "";
		}
	}

}
