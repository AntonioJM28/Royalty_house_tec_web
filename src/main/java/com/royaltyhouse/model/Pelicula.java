package com.royaltyhouse.model;

public record Pelicula(
        Long id,
        String titulo,
        int duracionMin,
        boolean enCartelera
) {
}
