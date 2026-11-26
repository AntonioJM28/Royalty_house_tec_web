package com.royaltyhouse.model;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadDia(
        LocalDate fecha,
        TipoRecurso tipoRecurso,
        Pelicula pelicula,
        int bloquesRequeridos,
        List<DisponibilidadRecurso> recursos
) {
}
