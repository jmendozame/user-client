package cl.bci.userclient.controller;

import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bci.userclient.model.User;
import cl.bci.userclient.service.IUserService;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private IUserService userServices;

	@ApiOperation(value = "Muestra todos los usuarios que se han creado", response = Map.class)
	@GetMapping
	public ResponseEntity<Map<String, Object>> getAll() {
		return userServices.findByActive();
	}

	@ApiOperation(value = "Crea un nuevo usuario", response = Map.class)
	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody User user, BindingResult result) {
		return userServices.create(user, result);
	}

	@ApiOperation(value = "Modifica un usuario", response = Map.class)
	@PutMapping("/{id}")
	public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody User user,
			BindingResult result) {
		user.setId(id);
		return userServices.update(user, result);
	}

	@ApiOperation(value = "Actualiza solo la contraseña de un usuario", response = Map.class)
	@PatchMapping("/{id}/password")
	public ResponseEntity<Map<String, Object>> updatePassword(@PathVariable Long id,
			@RequestBody Map<String, String> request) {
		return userServices.updatePassword(id, request.get("nuevaContraseña"));
	}

	@ApiOperation(value = "Elimina un usuario", response = Map.class)
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
		return userServices.delete(id);
	}
}
