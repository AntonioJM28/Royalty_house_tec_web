package com.royaltyhouse.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public record DetalleReserva(
        Reserva reserva,
        Recurso recurso,
        Pelicula pelicula,
        LocalTime horaFin,
        BigDecimal costoEstimado
) {
}
