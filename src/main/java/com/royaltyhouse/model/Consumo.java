package com.royaltyhouse.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Consumo(
        Long id,
        Long reservaId,
        Long productoId,
        int cantidad,
        BigDecimal precioUnitario,
        OffsetDateTime fecha
) {
}
