package cl.bci.userclient.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.client.RestTemplate;

import cl.bci.userclient.constantes.UserStatus;
import cl.bci.userclient.dto.UserDTOWithoutToken;
import cl.bci.userclient.model.Phone;
import cl.bci.userclient.model.User;
import cl.bci.userclient.repository.IPhoneRepository;
import cl.bci.userclient.repository.IUserRepository;

class UserServiceImplTest {

	@Mock
	private IUserRepository userRepository;

	@Mock
	private IPhoneRepository phoneRepository;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private BindingResult bindingResult;

	@InjectMocks
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testFindByActive_WhenUsersExist() {
		User user = new User();
		Phone phone = new Phone();
		List<Phone> phones = new ArrayList<Phone>();

		user.setId(1);
		user.setName("Giorgio Mendoza");
		user.setEmail("giorgio.mendoza@nttdata.com");
		user.setPassword("123456");
		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);
		user.setPhones(phones);
		user.setActive(UserStatus.ON);

		when(userRepository.findByActive(UserStatus.ON)).thenReturn(Arrays.asList(user));

		ResponseEntity<Map<String, Object>> response = userService.findByActive();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().containsKey("usuarios"));
		List<UserDTOWithoutToken> users = (List<UserDTOWithoutToken>) response.getBody().get("usuarios");
		assertEquals(1, users.size());
		assertEquals("Giorgio Mendoza", users.get(0).getNombre());
	}

	@Test
	void testFindByActive_WhenNoUsersExist() {
		when(userRepository.findByActive(UserStatus.ON)).thenReturn(Arrays.asList());

		ResponseEntity<Map<String, Object>> response = userService.findByActive();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertEquals("¡No hay usuarios ingresados!", response.getBody().get("mensaje"));
	}

	@Test
	void testCreate_WhenUserIsValid() throws Exception {
		User user = new User();
		Phone phone = new Phone();

		List<Phone> phones = new ArrayList<Phone>();

		user.setId(1);
		user.setName("Giorgio Mendoza");
		user.setEmail("giorgio.mendoza@nttdata.com");
		user.setPassword("123456");
		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);
		user.setPhones(phones);
		user.setActive(UserStatus.ON);

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.existsByEmailAndActive(user.getEmail(), UserStatus.ON)).thenReturn(false);
		when(userRepository.save(any(User.class))).thenReturn(user);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

		testCallOAuthTokenEndpoint();
		/*
		 * una estrategia común es hacer uso de reflection en Java para acceder a
		 * métodos privados
		 */

		ResponseEntity<Map<String, Object>> response = userService.create(user, bindingResult);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertEquals("¡El usuario ha sido creado con éxito!", response.getBody().get("mensaje"));
	}

	// @Test
	void testCallOAuthTokenEndpoint() throws Exception {
		String email = "test@example.com";
		String password = "password";
		String clientCredentials = "clientId:clientSecret";
		String oauthTokenUrl = "http://oauth.token.url";

		// Set values para campos privados usando reflexión
		setField(userService, "clientCredentials", clientCredentials);
		setField(userService, "oauthTokenUrl", oauthTokenUrl);

		ResponseEntity<String> mockResponse = new ResponseEntity<>("{\"access_token\":\"valid_token\"}", HttpStatus.OK);
		when(restTemplate.postForEntity(eq(oauthTokenUrl), any(HttpEntity.class), eq(String.class)))
				.thenReturn(mockResponse);

		// Accede al método privado usando la reflexión.
		Method method = UserServiceImpl.class.getDeclaredMethod("callOAuthTokenEndpoint", String.class, String.class);
		method.setAccessible(true);
		String token = (String) method.invoke(userService, email, password);

		assertEquals("valid_token", token);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	void testCreate_WhenUserHasValidationErrors() {
		when(bindingResult.hasErrors()).thenReturn(true);
		when(bindingResult.getFieldErrors())
				.thenReturn(Arrays.asList(new FieldError("user", "email", "Email is invalid")));

		User user = new User();
		user.setEmail("invalid");

		ResponseEntity<Map<String, Object>> response = userService.create(user, bindingResult);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
	}

	@Test
	void testUpdatePassword_WhenUserNotFound() {
		// Usamos la reflexión para establecer el valor de passwordRegex en el servicio
		ReflectionTestUtils.setField(userService, "passwordRegex", ".*");

		when(userRepository.findByIdAndActive(1L, UserStatus.ON)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = userService.updatePassword(1L, "newPassword");

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Usuario no encontrado", response.getBody().get("mensaje"));
	}

	@Test
	void testDelete_WhenUserExists() {
		User user = new User();
		user.setId(1L);
		user.setActive(UserStatus.ON);

		when(userRepository.findByIdAndActive(1L, UserStatus.ON)).thenReturn(user);

		ResponseEntity<Map<String, Object>> response = userService.delete(1L);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("¡El usuario ha sido eliminado con éxito!", response.getBody().get("mensaje"));
	}

	@Test
	void testDelete_WhenUserDoesNotExist() {
		when(userRepository.findByIdAndActive(1L, UserStatus.ON)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = userService.delete(1L);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("El usuario no existe", response.getBody().get("mensaje"));
	}

	@Test
	void testCreate_WhenEmailAlreadyRegistered() {
		User user = new User();
		user.setEmail("john.doe@example.com");

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.existsByEmailAndActive(user.getEmail(), UserStatus.ON)).thenReturn(true);

		ResponseEntity<Map<String, Object>> response = userService.create(user, bindingResult);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("El correo electrónico ya está registrado", response.getBody().get("mensaje"));
	}

	@Test
	void testUpdate_WhenUserNotFound() {
		User user = new User();
		user.setId(1L);

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.findByIdAndActive(user.getId(), UserStatus.ON)).thenReturn(null);

		ResponseEntity<Map<String, Object>> response = userService.update(user, bindingResult);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Usuario no encontrado", response.getBody().get("mensaje"));
	}

	@Test
	void testUpdate_WhenValidationFails() {
		User user = new User();
		user.setId(1L);

		when(bindingResult.hasErrors()).thenReturn(true);
		when(bindingResult.getFieldErrors())
				.thenReturn(Arrays.asList(new FieldError("user", "email", "Email is invalid")));

		ResponseEntity<Map<String, Object>> response = userService.update(user, bindingResult);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
	}

	@Test
	void testUpdate_WhenUserIsUpdatedSuccessfully() {

		User user = new User();
		user.setId(1L);
		user.setName("Updated Name");
		user.setEmail("updated.email@example.com");

		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setName("Old Name");
		existingUser.setEmail("old.email@example.com");
		existingUser.setActive(UserStatus.ON);

		Phone phone = new Phone();
		List<Phone> phones = new ArrayList<Phone>();

		existingUser.setId(1);
		existingUser.setName("Giorgio Mendoza");
		existingUser.setEmail("giorgio.mendoza@nttdata.com");
		existingUser.setPassword("123456");
		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);
		existingUser.setPhones(phones);
		existingUser.setActive(UserStatus.ON);

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.findByIdAndActive(user.getId(), UserStatus.ON)).thenReturn(existingUser);
		when(userRepository.save(any(User.class))).thenReturn(existingUser);

		ResponseEntity<Map<String, Object>> response = userService.update(user, bindingResult);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("¡El usuario ha sido actualizado con éxito!", response.getBody().get("mensaje"));
	}

	@Test
	void testUpdatePassword_WhenPasswordInvalid() {
		Long userId = 1L;
		String newPassword = "invalid";
		// Usamos la reflexión para establecer el valor de passwordRegex en el servicio
		ReflectionTestUtils.setField(userService, "passwordRegex",
				"^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#\\\\$%\\\\^&\\\\*]).{8,}$");

		ResponseEntity<Map<String, Object>> response = userService.updatePassword(userId, newPassword);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("El formato de la nueva contraseña no es válido", response.getBody().get("mensaje"));
	}

	@Test
	void testUpdatePassword_WhenDatabaseErrorOccurs() throws Exception {
		Long userId = 1L;
		String newPassword = "Valid@123";
		User user = new User();
		user.setId(userId);
		user.setActive(UserStatus.ON);

		ReflectionTestUtils.setField(userService, "passwordRegex", ".*");

		when(userRepository.findByIdAndActive(userId, UserStatus.ON)).thenReturn(user);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		doThrow(new DataAccessException("Error updating password") {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
		}).when(userRepository).save(any(User.class));

		ResponseEntity<Map<String, Object>> response = userService.updatePassword(userId, newPassword);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("¡Error al actualizar la contraseña en la base de datos!", response.getBody().get("mensaje"));
	}

	@Test
	void testCreateUserReactivatesExistingInactiveUser() {

		User user = new User();
		User existingUser = new User();

		user = new User();
		user.setEmail("test@example.com");
		user.setName("Test User");
		user.setPassword("password");

		Phone phone = new Phone();
		List<Phone> phones = new ArrayList<Phone>();

		existingUser.setId(1);
		existingUser.setName("Giorgio Mendoza");
		existingUser.setEmail("giorgio.mendoza@nttdata.com");
		existingUser.setPassword("123456");

		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);

		existingUser.setPhones(phones);
		existingUser.setActive(UserStatus.OFF);

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.existsByEmailAndActive(user.getEmail(), UserStatus.ON)).thenReturn(false);
		when(userRepository.findByEmail(user.getEmail())).thenReturn(existingUser);
		when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ResponseEntity<Map<String, Object>> response = userService.create(user, bindingResult);

		// Verificar que el usuario existente fue actualizado y guardado
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(UserStatus.ON, existingUser.getActive());
		assertEquals("Test User", existingUser.getName());
		assertEquals("encodedPassword", existingUser.getPassword());
		assertNotNull(existingUser.getModificationDate());
		assertNotNull(response.getBody().get("usuario"));
		assertEquals("¡El usuario ha sido creado con éxito!", response.getBody().get("mensaje"));

		verify(userRepository, times(1)).save(existingUser);
		verify(userRepository, never()).save(user); // Verifica que no se guardó un nuevo usuario
	}

	@Test
	void testUpdatePhones() {
		// Configuración de datos
		Phone phone1 = new Phone();
		phone1.setPhoneNumber(122);
		Phone phone2 = new Phone();
		phone2.setPhoneNumber(456);

		User user = new User();
		user.setId(1L);
		user.setPhones(Arrays.asList(phone1));

		User userUpdate = new User();
		userUpdate.setId(1L);
		userUpdate.setPhones(new ArrayList<>(Arrays.asList(phone2)));

		when(userRepository.findByIdAndActive(user.getId(), UserStatus.ON)).thenReturn(userUpdate);
		when(phoneRepository.save(any(Phone.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ResponseEntity<Map<String, Object>> response = userService.update(user, bindingResult);

		assertEquals(1, userUpdate.getPhones().size());
		assertEquals(122, userUpdate.getPhones().get(0).getPhoneNumber());

		// Verifica que phoneRepository.save se haya llamado el número correcto de veces
		verify(phoneRepository, times(1)).save(any(Phone.class));

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertTrue(response.getBody().containsKey("usuario"));
	}

	@Test
	void testUpdate_DataAccessException() {
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setEmail("test@example.com");

		when(bindingResult.hasErrors()).thenReturn(false);

		when(userRepository.findByIdAndActive(user.getId(), UserStatus.ON)).thenReturn(user);
		doThrow(new DataAccessException("Test Exception") {
		}).when(userRepository).save(any(User.class));

		ResponseEntity<Map<String, Object>> response = userService.update(user, bindingResult);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

		assertTrue(response.getBody().containsKey("mensaje"));
		assertEquals("Error al actualizar en la base de datos", response.getBody().get("mensaje"));
		assertTrue(response.getBody().containsKey("error"));
		assertTrue(response.getBody().get("error").toString().contains("Test Exception"));

		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void testUpdatePasswordSuccess() {
		Long userId = 1L;
		String newPassword = "NewPassword123!";
		User user = new User();
		Phone phone = new Phone();
		List<Phone> phones = new ArrayList<Phone>();

		user.setId(1);
		user.setName("Giorgio Mendoza");
		user.setEmail("giorgio.mendoza@nttdata.com");
		user.setPassword("123456");
		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);
		user.setPhones(phones);
		user.setActive(UserStatus.ON);

		ReflectionTestUtils.setField(userService, "passwordRegex", ".*");

		when(userRepository.findByIdAndActive(userId, UserStatus.ON)).thenReturn(user);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ResponseEntity<Map<String, Object>> response = userService.updatePassword(userId, newPassword);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertEquals("¡Contraseña actualizada con éxito!", response.getBody().get("mensaje"));
		assertTrue(response.getBody().containsKey("usuario"));
		UserDTOWithoutToken userDTO = (UserDTOWithoutToken) response.getBody().get("usuario");
		assertEquals(user.getName(), userDTO.getNombre());
		assertEquals(user.getEmail(), userDTO.getCorreo());

		verify(userRepository, times(1)).save(user);
	}

	@Test
	void testDeleteUserDataAccessException() {
		Long userId = 1L;
		String errorMessage = "Database error occurred";
		Throwable cause = new Throwable("Cause of the error");

		DataAccessException dataAccessException = new DataAccessException(errorMessage, cause) {
		};
		when(userRepository.findByIdAndActive(userId, UserStatus.ON)).thenThrow(dataAccessException);

		ResponseEntity<Map<String, Object>> response = userService.delete(userId);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertEquals("Error al eliminar lógicamente el usuario", response.getBody().get("mensaje"));
		assertTrue(response.getBody().containsKey("error"));
		assertTrue(response.getBody().get("error").toString().contains(errorMessage));
		assertTrue(response.getBody().get("error").toString().contains(cause.toString()));

		verify(userRepository, times(1)).findByIdAndActive(userId, UserStatus.ON);
	}

	@Test
	void testCreateUserJsonProcessingException() throws Exception {
		User user = new User();
		Phone phone = new Phone();
		List<Phone> phones = new ArrayList<Phone>();

		user.setId(1);
		user.setName("Giorgio Mendoza");
		user.setEmail("giorgio.mendoza@nttdata.com");
		user.setPassword("123456");
		phone.setCityCode(0);
		phone.setCountryCode(0);
		phone.setPhoneNumber(6543132);
		phones.add(phone);
		user.setPhones(phones);
		user.setActive(UserStatus.ON);

		BindingResult bindingResult = mock(BindingResult.class);

		String clientCredentials = "clientId:clientSecret";
		String oauthTokenUrl = "http://oauth.token.url";

		// Set values para campos privados usando reflexión
		setField(userService, "clientCredentials", clientCredentials);
		setField(userService, "oauthTokenUrl", oauthTokenUrl);

		when(bindingResult.hasErrors()).thenReturn(false);
		when(userRepository.existsByEmailAndActive(user.getEmail(), UserStatus.ON)).thenReturn(false);

		String malformedJson = "{access_token"; // JSON incorrecto
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
				.thenReturn(new ResponseEntity<>(malformedJson, HttpStatus.OK));
		when(userRepository.save(any(User.class))).thenReturn(user);
		ResponseEntity<Map<String, Object>> response = userService.create(user, bindingResult);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertTrue(response.getBody().containsKey("mensaje"));
		assertTrue(response.getBody().containsKey("usuario"));
		assertEquals("", user.getToken());
	}

}
