package com.royaltyhouse.repository;

import com.royaltyhouse.model.Consumo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConsumoRepository {

    private final JdbcClient jdbcClient;

    public ConsumoRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long insertar(Consumo consumo) {
        return jdbcClient.sql("""
                        INSERT INTO consumo (reserva_id, producto_id, cantidad, precio_unitario)
                        VALUES (:reservaId, :productoId, :cantidad, :precioUnitario)
                        RETURNING id
                        """)
                .param("reservaId", consumo.reservaId())
                .param("productoId", consumo.productoId())
                .param("cantidad", consumo.cantidad())
                .param("precioUnitario", consumo.precioUnitario())
                .query(Long.class)
                .single();
    }

    public List<Consumo> listarPorReserva(long reservaId) {
        return jdbcClient.sql("""
                        SELECT id, reserva_id, producto_id, cantidad, precio_unitario, fecha
                        FROM consumo
                        WHERE reserva_id = :reservaId
                        ORDER BY fecha, id
                        """)
                .param("reservaId", reservaId)
                .query((rs, rowNum) -> new Consumo(
                        rs.getLong("id"),
                        rs.getLong("reserva_id"),
                        rs.getLong("producto_id"),
                        rs.getInt("cantidad"),
                        rs.getBigDecimal("precio_unitario"),
                        rs.getObject("fecha", java.time.OffsetDateTime.class)))
                .list();
    }
}
