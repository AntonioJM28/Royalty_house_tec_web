package com.royaltyhouse.model;

public record UsuarioSesion(
        Long id,
        String nombre,
        String correo,
        Rol rol
) {
}
