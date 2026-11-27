package com.royaltyhouse.repository;

import com.royaltyhouse.model.Pelicula;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PeliculaRepository {

    private final JdbcClient jdbcClient;

    public PeliculaRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Pelicula> listarTodas() {
        return jdbcClient.sql("""
                        SELECT id, titulo, duracion_min, en_cartelera
                        FROM pelicula ORDER BY titulo
                        """)
                .query((rs, rowNum) -> new Pelicula(rs.getLong("id"), rs.getString("titulo"),
                        rs.getInt("duracion_min"), rs.getBoolean("en_cartelera"))).list();
    }

    public void guardar(Pelicula pelicula) {
        if (pelicula.id() == null) {
            jdbcClient.sql("INSERT INTO pelicula (titulo,duracion_min,en_cartelera) VALUES (:titulo,:duracion,:cartelera)")
                    .param("titulo", pelicula.titulo()).param("duracion", pelicula.duracionMin())
                    .param("cartelera", pelicula.enCartelera()).update();
        } else {
            jdbcClient.sql("UPDATE pelicula SET titulo=:titulo,duracion_min=:duracion,en_cartelera=:cartelera WHERE id=:id")
                    .param("id", pelicula.id()).param("titulo", pelicula.titulo()).param("duracion", pelicula.duracionMin())
                    .param("cartelera", pelicula.enCartelera()).update();
        }
    }

    public List<Pelicula> listarEnCartelera() {
        return jdbcClient.sql("""
                        SELECT id, titulo, duracion_min, en_cartelera
                        FROM pelicula
                        WHERE en_cartelera = TRUE
                        ORDER BY titulo
                        """)
                .query((rs, rowNum) -> new Pelicula(
                        rs.getLong("id"),
                        rs.getString("titulo"),
                        rs.getInt("duracion_min"),
                        rs.getBoolean("en_cartelera")))
                .list();
    }

    public Optional<Pelicula> buscarEnCarteleraPorId(long id) {
        return jdbcClient.sql("""
                        SELECT id, titulo, duracion_min, en_cartelera
                        FROM pelicula
                        WHERE id = :id AND en_cartelera = TRUE
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new Pelicula(
                        rs.getLong("id"),
                        rs.getString("titulo"),
                        rs.getInt("duracion_min"),
                        rs.getBoolean("en_cartelera")))
                .optional();
    }
}
