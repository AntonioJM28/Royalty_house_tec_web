package com.royaltyhouse.model;

import java.time.LocalTime;

public record HorarioAtencion(
        Long id,
        LocalTime horaApertura,
        LocalTime horaCierre
) {
}
