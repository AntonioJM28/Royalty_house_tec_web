package com.royaltyhouse.repository;

import com.royaltyhouse.model.Recurso;
import com.royaltyhouse.model.TipoRecurso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RecursoRepository {

    private final JdbcClient jdbcClient;

    public RecursoRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Recurso> listarTodos() {
        return jdbcClient.sql("""
                        SELECT id, nombre, tipo, capacidad, activo
                        FROM recurso
                        ORDER BY id
                        """)
                .query((rs, rowNum) -> new Recurso(rs.getLong("id"), rs.getString("nombre"),
                        TipoRecurso.valueOf(rs.getString("tipo")), rs.getInt("capacidad"), rs.getBoolean("activo")))
                .list();
    }

    public void guardar(Recurso recurso) {
        if (recurso.id() == null) {
            jdbcClient.sql("""
                            INSERT INTO recurso (nombre, tipo, capacidad, activo)
                            VALUES (:nombre, :tipo, :capacidad, :activo)
                            """)
                    .param("nombre", recurso.nombre()).param("tipo", recurso.tipo().name())
                    .param("capacidad", recurso.capacidad()).param("activo", recurso.activo()).update();
        } else {
            jdbcClient.sql("""
                            UPDATE recurso SET nombre=:nombre, tipo=:tipo, capacidad=:capacidad, activo=:activo
                            WHERE id=:id
                            """)
                    .param("id", recurso.id()).param("nombre", recurso.nombre()).param("tipo", recurso.tipo().name())
                    .param("capacidad", recurso.capacidad()).param("activo", recurso.activo()).update();
        }
    }

    public List<Recurso> listarActivos() {
        return jdbcClient.sql("""
                        SELECT id, nombre, tipo, capacidad, activo
                        FROM recurso
                        WHERE activo = TRUE
                        ORDER BY id
                        """)
                .query((rs, rowNum) -> new Recurso(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        TipoRecurso.valueOf(rs.getString("tipo")),
                        rs.getInt("capacidad"),
                        rs.getBoolean("activo")))
                .list();
    }

    public List<Recurso> listarActivosPorTipo(TipoRecurso tipo) {
        return jdbcClient.sql("""
                        SELECT id, nombre, tipo, capacidad, activo
                        FROM recurso
                        WHERE activo = TRUE AND tipo = :tipo
                        ORDER BY id
                        """)
                .param("tipo", tipo.name())
                .query((rs, rowNum) -> new Recurso(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        TipoRecurso.valueOf(rs.getString("tipo")),
                        rs.getInt("capacidad"),
                        rs.getBoolean("activo")))
                .list();
    }

    public Optional<Recurso> buscarActivoPorId(long id) {
        return jdbcClient.sql("""
                        SELECT id, nombre, tipo, capacidad, activo
                        FROM recurso
                        WHERE id = :id AND activo = TRUE
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new Recurso(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        TipoRecurso.valueOf(rs.getString("tipo")),
                        rs.getInt("capacidad"),
                        rs.getBoolean("activo")))
                .optional();
    }
}
