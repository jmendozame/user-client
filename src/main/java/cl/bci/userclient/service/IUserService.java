package cl.bci.userclient.service;

import cl.bci.userclient.model.User;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface IUserService {
	ResponseEntity<Map<String, Object>> findByActive();

	ResponseEntity<Map<String, Object>> create(User user, BindingResult result);

	ResponseEntity<Map<String, Object>> update(User user, BindingResult result);

	ResponseEntity<Map<String, Object>> updatePassword(Long userId, String newPassword);

	ResponseEntity<Map<String, Object>> delete(Long id);
}
