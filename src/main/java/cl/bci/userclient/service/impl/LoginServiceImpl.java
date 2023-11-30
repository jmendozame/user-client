package cl.bci.userclient.service.impl;

import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bci.userclient.model.User;
import cl.bci.userclient.repository.IUserRepository;

@Service
public class LoginServiceImpl implements UserDetailsService {

	@Autowired
	private IUserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		try {
			User user;
			user = userRepository.findByEmail(email);
			if (user == null) {
				System.out.println("Error en el login: el usuario ".concat(email).concat("no esta registrado"));
				throw new UsernameNotFoundException(
						"Error en el login: el usuario ".concat(email).concat(" no esta registrado"));
			}
			user.setLastLogin(new Date());
			userRepository.save(user);
			return new org.springframework.security.core.userdetails.User(user.getName(), user.getPassword(), true,
					true, true, true, Collections.emptyList());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
