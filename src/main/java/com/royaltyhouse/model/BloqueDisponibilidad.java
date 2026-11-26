package com.royaltyhouse.model;

import java.time.LocalTime;

public record BloqueDisponibilidad(
        LocalTime horaInicio,
        boolean disponible,
        boolean inicioDisponible
) {
}
