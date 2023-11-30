package cl.bci.userclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.bci.userclient.model.Phone;

public interface IPhoneRepository extends JpaRepository<Phone, Integer> {

	public Phone findById(Long id);

}
