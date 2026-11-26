package com.royaltyhouse.model;

import java.math.BigDecimal;

public record Producto(
        Long id,
        String nombre,
        BigDecimal precio,
        int existencias,
        boolean activo
) {
}
