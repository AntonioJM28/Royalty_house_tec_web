package com.royaltyhouse.model;

import java.math.BigDecimal;

public record ReporteServicio(
        String servicio,
        BigDecimal ocupacionPorcentaje,
        BigDecimal ingresosEstimados
) {
}
