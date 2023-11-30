package cl.bci.userclient.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import cl.bci.userclient.constantes.UserStatus;

@Entity
@Table(name = "tbl_user")
public class User implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@NotEmpty(message = "El nombre no puede estar vacío")
	@JsonProperty("nombre")
	private String name;

	@Column(unique = true)
	@Email(message = "El formato del correo no es válido")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", message = "El formato del correo no es válido")
	@JsonProperty("correo")
	private String email;

	@NotEmpty(message = "La contraseña no puede estar vacía")
	@JsonProperty("contraseña")
	private String password;

	@OneToMany()
	@JsonProperty("telefonos")
	private List<Phone> phones;

	@JsonIgnore
	private int active;

	@JsonIgnore
	private Date creationDate;

	@JsonIgnore
	private Date modificationDate;

	@JsonIgnore
	private Date lastLogin;

	@Column(length = 600)
	private String token;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<Phone> getPhones() {
		return phones;
	}

	public void setPhone(List<Phone> phones) {
		this.phones = phones;
	}

	public int getActive() {
		return active;
	}

	public void setActive(int active) {
		this.active = active;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@PrePersist
	public void prePersist() {
		active = UserStatus.ON;
		creationDate = new Date();
		lastLogin = new Date();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
