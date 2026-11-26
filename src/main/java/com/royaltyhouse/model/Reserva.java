package com.royaltyhouse.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record Reserva(
        Long id,
        String codigo,
        Long usuarioId,
        String nombreCliente,
        String telefonoCliente,
        Long recursoId,
        Long peliculaId,
        LocalDate fecha,
        LocalTime horaInicio,
        int bloques,
        EstadoReserva estado,
        LocalTime horaEntrada,
        LocalTime horaSalida,
        OffsetDateTime creadaEn
) {
}
