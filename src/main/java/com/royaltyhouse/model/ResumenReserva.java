package com.royaltyhouse.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ResumenReserva(
        Recurso recurso,
        Pelicula pelicula,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        int bloques,
        BigDecimal costoEstimado
) {
}
