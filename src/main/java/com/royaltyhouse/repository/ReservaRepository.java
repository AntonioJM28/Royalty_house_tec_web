package com.royaltyhouse.repository;

import com.royaltyhouse.model.EstadoReserva;
import com.royaltyhouse.model.Reserva;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservaRepository {

    private final JdbcClient jdbcClient;

    public ReservaRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long insertar(Reserva reserva) {
        return jdbcClient.sql("""
                        INSERT INTO reserva (
                            codigo, usuario_id, nombre_cliente, telefono_cliente,
                            recurso_id, pelicula_id, fecha, hora_inicio, bloques, estado
                        )
                        VALUES (
                            :codigo, :usuarioId, :nombreCliente, :telefonoCliente,
                            :recursoId, :peliculaId, :fecha, :horaInicio, :bloques, :estado
                        )
                        RETURNING id
                        """)
                .param("codigo", reserva.codigo())
                .param("usuarioId", reserva.usuarioId())
                .param("nombreCliente", reserva.nombreCliente())
                .param("telefonoCliente", reserva.telefonoCliente())
                .param("recursoId", reserva.recursoId())
                .param("peliculaId", reserva.peliculaId())
                .param("fecha", reserva.fecha())
                .param("horaInicio", reserva.horaInicio())
                .param("bloques", reserva.bloques())
                .param("estado", reserva.estado().name())
                .query(Long.class)
                .single();
    }

    public Optional<Reserva> buscarPorId(long id) {
        return consultaReserva(id, false);
    }

    public Optional<Reserva> buscarPorIdParaActualizar(long id) {
        return consultaReserva(id, true);
    }

    public List<Reserva> listarPorUsuario(long usuarioId) {
        return jdbcClient.sql("""
                        SELECT id, codigo, usuario_id, nombre_cliente, telefono_cliente,
                               recurso_id, pelicula_id, fecha, hora_inicio, bloques, estado,
                               hora_entrada, hora_salida, creada_en
                        FROM reserva
                        WHERE usuario_id = :usuarioId
                        ORDER BY fecha DESC, hora_inicio DESC
                        """)
                .param("usuarioId", usuarioId)
                .query((rs, rowNum) -> mapearReserva(rs))
                .list();
    }

    public int contarConfirmadasFuturas(long usuarioId, LocalDate fechaActual, LocalTime horaActual) {
        return jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM reserva
                        WHERE usuario_id = :usuarioId
                          AND estado = 'CONFIRMADA'
                          AND (fecha > :fechaActual OR (fecha = :fechaActual AND hora_inicio > :horaActual))
                        """)
                .param("usuarioId", usuarioId)
                .param("fechaActual", fechaActual)
                .param("horaActual", horaActual)
                .query(Integer.class)
                .single();
    }

    public int actualizarEstado(long id, EstadoReserva estado) {
        return jdbcClient.sql("""
                        UPDATE reserva
                        SET estado = :estado
                        WHERE id = :id
                        """)
                .param("estado", estado.name())
                .param("id", id)
                .update();
    }

    private Optional<Reserva> consultaReserva(long id, boolean bloquear) {
        var sql = """
                SELECT id, codigo, usuario_id, nombre_cliente, telefono_cliente,
                       recurso_id, pelicula_id, fecha, hora_inicio, bloques, estado,
                       hora_entrada, hora_salida, creada_en
                FROM reserva
                WHERE id = :id
                """ + (bloquear ? " FOR UPDATE" : "");
        return jdbcClient.sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> mapearReserva(rs))
                .optional();
    }

    private Reserva mapearReserva(java.sql.ResultSet rs) throws java.sql.SQLException {
        var horaEntrada = rs.getTime("hora_entrada");
        var horaSalida = rs.getTime("hora_salida");
        return new Reserva(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getObject("usuario_id", Long.class),
                rs.getString("nombre_cliente"),
                rs.getString("telefono_cliente"),
                rs.getLong("recurso_id"),
                rs.getObject("pelicula_id", Long.class),
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora_inicio").toLocalTime(),
                rs.getInt("bloques"),
                EstadoReserva.valueOf(rs.getString("estado")),
                horaEntrada == null ? null : horaEntrada.toLocalTime(),
                horaSalida == null ? null : horaSalida.toLocalTime(),
                rs.getObject("creada_en", java.time.OffsetDateTime.class));
    }
}
