package com.royaltyhouse.model;

public record Recurso(
        Long id,
        String nombre,
        TipoRecurso tipo,
        int capacidad,
        boolean activo
) {
}
