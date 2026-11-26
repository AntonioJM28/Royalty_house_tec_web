package com.royaltyhouse.model;

public record Usuario(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String passwordHash,
        Rol rol,
        boolean activo,
        boolean aceptoDatos
) {
}
