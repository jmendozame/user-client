package cl.bci.userclient.dto;

import java.util.List;

public class UserDTOWithoutToken {

    private long id;
    private String nombre;
    private String correo;
    private String contraseña;
    private List<PhoneDTO> telefonos;

    // Getters y setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public List<PhoneDTO> getTelefonos() {
        return telefonos;
    }

    public void setTelefonos(List<PhoneDTO> telefonos) {
        this.telefonos = telefonos;
    }
}
