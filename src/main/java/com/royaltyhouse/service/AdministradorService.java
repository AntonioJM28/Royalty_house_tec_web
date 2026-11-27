package com.royaltyhouse.service;

import com.royaltyhouse.model.*;
import com.royaltyhouse.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Service
public class AdministradorService {
    private final RecursoRepository recursoRepository;
    private final PeliculaRepository peliculaRepository;
    private final TarifaRepository tarifaRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public AdministradorService(RecursoRepository recursoRepository, PeliculaRepository peliculaRepository,
                                TarifaRepository tarifaRepository, HorarioAtencionRepository horarioRepository,
                                ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.recursoRepository = recursoRepository;
        this.peliculaRepository = peliculaRepository;
        this.tarifaRepository = tarifaRepository;
        this.horarioRepository = horarioRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Recurso> recursos() { return recursoRepository.listarTodos(); }
    public void guardarRecurso(Long id, String nombre, TipoRecurso tipo, int capacidad, boolean activo) {
        recursoRepository.guardar(new Recurso(id, nombre, tipo, capacidad, activo));
    }
    public List<Pelicula> peliculas() { return peliculaRepository.listarTodas(); }
    public void guardarPelicula(Long id, String titulo, int duracion, boolean cartelera) {
        peliculaRepository.guardar(new Pelicula(id, titulo, duracion, cartelera));
    }
    public List<Tarifa> tarifas() { return tarifaRepository.listarTodas(); }
    public void actualizarTarifa(TipoRecurso tipo, BigDecimal precio) { tarifaRepository.actualizar(tipo, precio); }
    public HorarioAtencion horario() { return horarioRepository.buscarHorario().orElseThrow(); }
    public void actualizarHorario(LocalTime apertura, LocalTime cierre) { horarioRepository.actualizar(apertura, cierre); }
    public List<Producto> productos() { return productoRepository.listarTodos(); }
    public void guardarProducto(Long id, String nombre, BigDecimal precio, int stock, boolean activo) {
        productoRepository.guardar(new Producto(id, nombre, precio, stock, activo));
    }
    public List<Usuario> personal() { return usuarioRepository.listarPersonal(); }
    public void actualizarPersonal(long id, Rol rol, boolean activo) { usuarioRepository.actualizarPersonal(id, rol, activo); }
    public void crearPersonal(String nombre, String correo, String telefono, Rol rol, String contrasena) {
        usuarioRepository.insertar(new Usuario(null, nombre, correo.toLowerCase(), telefono,
                HashContrasena.crear(contrasena), rol, true, true));
    }
}
