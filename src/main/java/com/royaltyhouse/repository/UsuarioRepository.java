package com.royaltyhouse.repository;

import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.Usuario;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepository {

    private final JdbcClient jdbcClient;

    public UsuarioRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return jdbcClient.sql("""
                        SELECT id, nombre, correo, telefono, password_hash, rol, activo, acepto_datos
                        FROM usuario
                        WHERE correo = :correo
                        """)
                .param("correo", correo)
                .query((rs, rowNum) -> new Usuario(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("password_hash"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getBoolean("activo"),
                        rs.getBoolean("acepto_datos")))
                .optional();
    }

    public Optional<Usuario> buscarPorId(long id) {
        return jdbcClient.sql("""
                        SELECT id, nombre, correo, telefono, password_hash, rol, activo, acepto_datos
                        FROM usuario
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new Usuario(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("password_hash"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getBoolean("activo"),
                        rs.getBoolean("acepto_datos")))
                .optional();
    }

    public Optional<Usuario> buscarPorIdParaActualizar(long id) {
        return jdbcClient.sql("""
                        SELECT id, nombre, correo, telefono, password_hash, rol, activo, acepto_datos
                        FROM usuario
                        WHERE id = :id
                        FOR UPDATE
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new Usuario(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("password_hash"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getBoolean("activo"),
                        rs.getBoolean("acepto_datos")))
                .optional();
    }

    public long insertar(Usuario usuario) {
        return jdbcClient.sql("""
                        INSERT INTO usuario (nombre, correo, telefono, password_hash, rol, activo, acepto_datos)
                        VALUES (:nombre, :correo, :telefono, :passwordHash, :rol, :activo, :aceptoDatos)
                        RETURNING id
                        """)
                .param("nombre", usuario.nombre())
                .param("correo", usuario.correo())
                .param("telefono", usuario.telefono())
                .param("passwordHash", usuario.passwordHash())
                .param("rol", usuario.rol().name())
                .param("activo", usuario.activo())
                .param("aceptoDatos", usuario.aceptoDatos())
                .query(Long.class)
                .single();
    }
}
