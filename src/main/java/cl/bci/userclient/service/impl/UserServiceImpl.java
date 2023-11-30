package cl.bci.userclient.service.impl;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		List<User> userList = (List<User>) userRepository.findByActive(UserStatus.ON);
		if (!userList.isEmpty()) {
			response.put("usuarios", userList);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
		} else {
			response.put("mensaje", "¡No hay usuarios ingresados!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);

		}
	}

	@Override
	public ResponseEntity<Map<String, Object>> create(@Valid User user, BindingResult result) {
		Map<String, Object> response = new HashMap<>();
		if (result.hasErrors()) {
			handleValidationErrors(result, response);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			response.put("mensaje", "El correo electrónico ya está registrado");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		String unencryptedPassword = user.getPassword();
		User newUser = saveUser(user);
		user.setToken(callOAuthTokenEndpoint(newUser.getEmail(), unencryptedPassword));
		newUser = userRepository.save(user);
		response.put("mensaje", "¡El usuario ha sido creado con éxito!");
		response.put("usuario", newUser);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
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
		Map<String, String> errores = new HashMap<>();
		for (FieldError error : result.getFieldErrors()) {
			errores.put(error.getField(), error.getDefaultMessage());
		}
		response.put("mensaje", errores);
	}

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
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		response.put("mensaje", "¡El usuario ha sido actualizado con éxito!");
		response.put("usuario", userUpdate);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	private User updateUser(User user) {
		User userUpdate = userRepository.findById(user.getId());
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
			for (Phone phone : updatedPhones) {
				Phone savedPhone = phoneRepository.save(phone);
				userUpdate.getPhones().add(savedPhone);
			}
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
		response.put("usuario", userUpdatePass);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private User updateIdPassword(Long userId, String newPassword) {
		User user = userRepository.findById(userId);
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
			User user = userRepository.findById(id);
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
			return null;
		}
	}

}
