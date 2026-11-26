package com.royaltyhouse.model;

import java.math.BigDecimal;

public record Tarifa(
        Long id,
        TipoRecurso tipoRecurso,
        BigDecimal precioHora
) {
}
