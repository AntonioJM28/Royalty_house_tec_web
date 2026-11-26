package com.royaltyhouse.repository;

import com.royaltyhouse.model.HorarioAtencion;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class HorarioAtencionRepository {

    private final JdbcClient jdbcClient;

    public HorarioAtencionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<HorarioAtencion> buscarHorario() {
        return jdbcClient.sql("""
                        SELECT id, hora_apertura, hora_cierre
                        FROM horario_atencion
                        ORDER BY id
                        LIMIT 1
                        """)
                .query((rs, rowNum) -> new HorarioAtencion(
                        rs.getLong("id"),
                        rs.getTime("hora_apertura").toLocalTime(),
                        rs.getTime("hora_cierre").toLocalTime()))
                .optional();
    }
}
