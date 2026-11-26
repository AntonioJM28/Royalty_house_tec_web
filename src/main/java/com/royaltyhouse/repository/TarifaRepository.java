package com.royaltyhouse.repository;

import com.royaltyhouse.model.Tarifa;
import com.royaltyhouse.model.TipoRecurso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TarifaRepository {

    private final JdbcClient jdbcClient;

    public TarifaRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Tarifa> buscarPorTipo(TipoRecurso tipoRecurso) {
        return jdbcClient.sql("""
                        SELECT id, tipo_recurso, precio_hora
                        FROM tarifa
                        WHERE tipo_recurso = :tipoRecurso
                        """)
                .param("tipoRecurso", tipoRecurso.name())
                .query((rs, rowNum) -> new Tarifa(
                        rs.getLong("id"),
                        TipoRecurso.valueOf(rs.getString("tipo_recurso")),
                        rs.getBigDecimal("precio_hora")))
                .optional();
    }
}
