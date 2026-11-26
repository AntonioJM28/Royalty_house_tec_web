package com.royaltyhouse.repository;

import com.royaltyhouse.model.Producto;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductoRepository {

    private final JdbcClient jdbcClient;

    public ProductoRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Producto> listarActivos() {
        return jdbcClient.sql("""
                        SELECT id, nombre, precio, existencias, activo
                        FROM producto
                        WHERE activo = TRUE
                        ORDER BY nombre
                        """)
                .query((rs, rowNum) -> new Producto(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio"),
                        rs.getInt("existencias"),
                        rs.getBoolean("activo")))
                .list();
    }

    public Optional<Producto> buscarActivoPorId(long id) {
        return jdbcClient.sql("""
                        SELECT id, nombre, precio, existencias, activo
                        FROM producto
                        WHERE id = :id AND activo = TRUE
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new Producto(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("precio"),
                        rs.getInt("existencias"),
                        rs.getBoolean("activo")))
                .optional();
    }
}
