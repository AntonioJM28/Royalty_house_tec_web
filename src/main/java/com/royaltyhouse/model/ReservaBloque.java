package com.royaltyhouse.model;

import java.time.LocalDate;

public record ReservaBloque(
        Long id,
        Long reservaId,
        Long recursoId,
        LocalDate fecha,
        int bloque
) {
}
