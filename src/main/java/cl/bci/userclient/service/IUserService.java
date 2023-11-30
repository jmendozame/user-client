package cl.bci.userclient.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import cl.bci.userclient.model.User;
import javax.validation.Valid;

public interface IUserService {
	public ResponseEntity<Map<String, Object>> findByActive();

	public ResponseEntity<Map<String, Object>> create(@Valid User user, BindingResult result);

	public ResponseEntity<Map<String, Object>> update(@Valid User user, BindingResult result);

	public ResponseEntity<Map<String, Object>> updatePassword(Long userId, String newPassword);

	public ResponseEntity<Map<String, Object>> delete(Long id);

}
