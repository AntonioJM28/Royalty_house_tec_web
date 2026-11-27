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

    public List<Producto> listarTodos() {
        return jdbcClient.sql("SELECT id,nombre,precio,existencias,activo FROM producto ORDER BY nombre")
                .query((rs, rowNum) -> new Producto(rs.getLong("id"), rs.getString("nombre"),
                        rs.getBigDecimal("precio"), rs.getInt("existencias"), rs.getBoolean("activo"))).list();
    }

    public void guardar(Producto producto) {
        if (producto.id() == null) {
            jdbcClient.sql("INSERT INTO producto(nombre,precio,existencias,activo) VALUES(:nombre,:precio,:stock,:activo)")
                    .param("nombre", producto.nombre()).param("precio", producto.precio())
                    .param("stock", producto.existencias()).param("activo", producto.activo()).update();
        } else {
            jdbcClient.sql("UPDATE producto SET nombre=:nombre,precio=:precio,existencias=:stock,activo=:activo WHERE id=:id")
                    .param("id", producto.id()).param("nombre", producto.nombre()).param("precio", producto.precio())
                    .param("stock", producto.existencias()).param("activo", producto.activo()).update();
        }
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

    public int descontarStock(long id, int cantidad) {
        return jdbcClient.sql("""
                        UPDATE producto
                        SET existencias = existencias - :cantidad
                        WHERE id = :id AND activo = TRUE AND existencias >= :cantidad
                        """)
                .param("id", id)
                .param("cantidad", cantidad)
                .update();
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
