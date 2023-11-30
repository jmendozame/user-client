package cl.bci.userclient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.bci.userclient.model.User;

public interface IUserRepository extends JpaRepository<User, Integer> {

	public List<User> findByActive(int active);

	public User findByEmail(String email);

	public User findById(Long id);

	public boolean existsByEmail(String email);

}
