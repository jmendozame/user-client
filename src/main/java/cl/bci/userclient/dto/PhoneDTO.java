package cl.bci.userclient.dto;

public class PhoneDTO {

    private int numero;
    private long codigoCiudad;
    private long codigoPais;

    // Getters y setters
    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public long getCodigoCiudad() {
        return codigoCiudad;
    }

    public void setCodigoCiudad(long codigoCiudad) {
        this.codigoCiudad = codigoCiudad;
    }

    public long getCodigoPais() {
        return codigoPais;
    }

    public void setCodigoPais(long codigoPais) {
        this.codigoPais = codigoPais;
    }
}
