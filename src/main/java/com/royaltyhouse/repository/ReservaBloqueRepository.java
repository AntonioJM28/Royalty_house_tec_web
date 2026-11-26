package com.royaltyhouse.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReservaBloqueRepository {

    private final JdbcClient jdbcClient;

    public ReservaBloqueRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Integer> listarBloquesOcupados(long recursoId, LocalDate fecha) {
        return jdbcClient.sql("""
                        SELECT bloque
                        FROM reserva_bloque
                        WHERE recurso_id = :recursoId AND fecha = :fecha
                        ORDER BY bloque
                        """)
                .param("recursoId", recursoId)
                .param("fecha", fecha)
                .query(Integer.class)
                .list();
    }

    public void insertar(long reservaId, long recursoId, LocalDate fecha, int bloque) {
        jdbcClient.sql("""
                        INSERT INTO reserva_bloque (reserva_id, recurso_id, fecha, bloque)
                        VALUES (:reservaId, :recursoId, :fecha, :bloque)
                        """)
                .param("reservaId", reservaId)
                .param("recursoId", recursoId)
                .param("fecha", fecha)
                .param("bloque", bloque)
                .update();
    }

    public int eliminarPorReserva(long reservaId) {
        return jdbcClient.sql("""
                        DELETE FROM reserva_bloque
                        WHERE reserva_id = :reservaId
                        """)
                .param("reservaId", reservaId)
                .update();
    }
}
