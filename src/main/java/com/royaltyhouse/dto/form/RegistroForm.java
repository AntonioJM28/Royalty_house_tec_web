package com.royaltyhouse.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistroForm {

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 254)
    private String correo;

    @NotBlank
    @Size(max = 30)
    private String telefono;

    @NotBlank
    @Size(min = 8, max = 72)
    private String contrasena;

    private boolean aceptoDatos;

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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isAceptoDatos() {
        return aceptoDatos;
    }

    public void setAceptoDatos(boolean aceptoDatos) {
        this.aceptoDatos = aceptoDatos;
    }
}
