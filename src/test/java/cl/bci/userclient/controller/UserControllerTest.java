package cl.bci.userclient.controller;

import cl.bci.userclient.model.User;
import cl.bci.userclient.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserControllerTest {

	@Mock
	private IUserService userServices;

	@Mock
	private BindingResult bindingResult;

	@InjectMocks
	private UserController userController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testGetAll() {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "success");

		when(userServices.findByActive()).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

		ResponseEntity<Map<String, Object>> result = userController.getAll();

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("success", result.getBody().get("mensaje"));
	}

	@Test
	void testCreate() {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "¡El usuario ha sido creado con éxito!");

		when(userServices.create(any(User.class), any(BindingResult.class)))
				.thenReturn(new ResponseEntity<>(response, HttpStatus.CREATED));

		User user = new User();
		ResponseEntity<Map<String, Object>> result = userController.create(user, bindingResult);

		assertEquals(HttpStatus.CREATED, result.getStatusCode());
		assertEquals("¡El usuario ha sido creado con éxito!", result.getBody().get("mensaje"));
	}

	@Test
	void testUpdate() {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "¡El usuario ha sido actualizado con éxito!");

		when(userServices.update(any(User.class), any(BindingResult.class)))
				.thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

		User user = new User();
		ResponseEntity<Map<String, Object>> result = userController.update(1L, user, bindingResult);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("¡El usuario ha sido actualizado con éxito!", result.getBody().get("mensaje"));
	}

	@Test
	void testUpdatePassword() {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "¡Contraseña actualizada con éxito!");

		when(userServices.updatePassword(anyLong(), anyString()))
				.thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

		Map<String, String> request = new HashMap<>();
		request.put("nuevaContraseña", "newPassword");

		ResponseEntity<Map<String, Object>> result = userController.updatePassword(1L, request);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("¡Contraseña actualizada con éxito!", result.getBody().get("mensaje"));
	}

	@Test
	void testDelete() {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "¡El usuario ha sido eliminado con éxito!");

		when(userServices.delete(anyLong())).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

		ResponseEntity<Map<String, Object>> result = userController.delete(1L);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("¡El usuario ha sido eliminado con éxito!", result.getBody().get("mensaje"));
	}
}
