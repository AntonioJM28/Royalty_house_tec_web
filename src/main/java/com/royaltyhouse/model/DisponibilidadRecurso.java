package com.royaltyhouse.model;

import java.util.List;

public record DisponibilidadRecurso(
        Recurso recurso,
        List<BloqueDisponibilidad> bloques
) {
}
