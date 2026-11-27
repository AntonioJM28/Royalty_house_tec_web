package com.royaltyhouse.service;

import com.royaltyhouse.exception.EntidadNoEncontradaException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.EstadoReserva;
import com.royaltyhouse.model.Pelicula;
import com.royaltyhouse.model.Producto;
import com.royaltyhouse.model.Recurso;
import com.royaltyhouse.model.Reserva;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.model.Usuario;
import com.royaltyhouse.repository.ConsumoRepository;
import com.royaltyhouse.repository.ProductoRepository;
import com.royaltyhouse.repository.ReservaBloqueRepository;
import com.royaltyhouse.repository.ReservaRepository;
import com.royaltyhouse.repository.TarifaRepository;
import com.royaltyhouse.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class RecepcionistaService {

    private final ReservaRepository reservaRepository;
    private final ReservaBloqueRepository reservaBloqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ConsumoRepository consumoRepository;
    private final TarifaRepository tarifaRepository;
    private final DisponibilidadService disponibilidadService;
    private final Clock reloj;
    private final int toleranciaMinutos;

    public RecepcionistaService(ReservaRepository reservaRepository,
                                ReservaBloqueRepository reservaBloqueRepository,
                                UsuarioRepository usuarioRepository,
                                ProductoRepository productoRepository,
                                ConsumoRepository consumoRepository,
                                TarifaRepository tarifaRepository,
                                DisponibilidadService disponibilidadService,
                                Clock reloj,
                                @Value("${royaltyhouse.reservas.tolerancia-no-presentada-minutos:15}") int toleranciaMinutos) {
        this.reservaRepository = reservaRepository;
        this.reservaBloqueRepository = reservaBloqueRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.consumoRepository = consumoRepository;
        this.tarifaRepository = tarifaRepository;
        this.disponibilidadService = disponibilidadService;
        this.reloj = reloj;
        this.toleranciaMinutos = toleranciaMinutos;
    }

    public List<Usuario> buscarClientes(String texto) {
        return usuarioRepository.buscarPorNombreOCorreo(texto);
    }

    @Transactional
    public Reserva registrarMostrador(Long usuarioId, String nombre, String telefono,
                                      Long recursoId, Long peliculaId, LocalDate fecha,
                                      LocalTime horaInicio, Integer bloques, Integer asistentes) {
        if (nombre == null || nombre.isBlank() || telefono == null || telefono.isBlank()) {
            throw new ReglaNegocioException("El nombre y teléfono del cliente son obligatorios.");
        }
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(recursoId);
        Pelicula pelicula = null;
        int duracion;

        if (recurso.tipo() == TipoRecurso.MESA_NORMAL || recurso.tipo() == TipoRecurso.MESA_VIP) {
            if (bloques == null || bloques < 1 || bloques > 4) {
                throw new ReglaNegocioException("La duración de billar debe ser de 1 a 4 horas.");
            }
            duracion = bloques;
        } else if (recurso.tipo() == TipoRecurso.SALA_MINICINE) {
            if (asistentes == null || asistentes < 1 || asistentes > 7) {
                throw new ReglaNegocioException("Los asistentes deben estar entre 1 y 7.");
            }
            if (peliculaId == null) throw new ReglaNegocioException("Seleccione una película.");
            pelicula = disponibilidadService.obtenerPeliculaEnCartelera(peliculaId);
            duracion = disponibilidadService.calcularBloques(pelicula);
        } else {
            throw new ReglaNegocioException("Tipo de recurso no válido.");
        }

        disponibilidadService.validarHorario(fecha, horaInicio, duracion, recurso);
        String codigo;
        do {
            codigo = generarCodigo();
        } while (reservaRepository.buscarPorCodigo(codigo).isPresent());

        Reserva reserva = new Reserva(null, codigo, usuarioId, nombre.trim(), telefono.trim(),
                recurso.id(), pelicula == null ? null : pelicula.id(), fecha, horaInicio,
                duracion, EstadoReserva.CONFIRMADA, null, null, asistentes, null);
        long id = reservaRepository.insertar(reserva);
        for (int i = 0; i < duracion; i++) {
            reservaBloqueRepository.insertar(id, recurso.id(), fecha, horaInicio.plusHours(i).getHour());
        }
        return reservaRepository.buscarPorId(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("No se pudo recuperar la reserva."));
    }

    @Transactional
    public Reserva marcarEntrada(String codigo) {
        Reserva reserva = buscarPorCodigo(codigo);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (reserva.estado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se puede registrar entrada a una reserva confirmada.");
        }
        if (!reserva.fecha().equals(ahora.toLocalDate())
                || ahora.toLocalTime().isBefore(reserva.horaInicio())
                || ahora.toLocalTime().isAfter(reserva.horaInicio().plusHours(reserva.bloques()))) {
            throw new ReglaNegocioException("La entrada no corresponde a la fecha y horario de la reserva.");
        }
        reservaRepository.registrarEntrada(reserva.id(), ahora.toLocalTime());
        return reservaRepository.buscarPorId(reserva.id()).orElseThrow();
    }

    @Transactional
    public Reserva marcarSalida(String codigo) {
        Reserva reserva = buscarPorCodigo(codigo);
        if (reserva.estado() != EstadoReserva.EN_USO) {
            throw new ReglaNegocioException("Solo se puede registrar salida de una reserva en uso.");
        }
        reservaRepository.registrarSalida(reserva.id(), LocalTime.now(reloj));
        reservaBloqueRepository.eliminarPorReserva(reserva.id());
        return reservaRepository.buscarPorId(reserva.id()).orElseThrow();
    }

    @Transactional
    public Reserva marcarNoPresentada(String codigo) {
        Reserva reserva = buscarPorCodigo(codigo);
        LocalDateTime inicioMasTolerancia = LocalDateTime.of(reserva.fecha(), reserva.horaInicio())
                .plusMinutes(toleranciaMinutos);
        if (reserva.estado() != EstadoReserva.CONFIRMADA
                || LocalDateTime.now(reloj).isBefore(inicioMasTolerancia)) {
            throw new ReglaNegocioException("Solo puede marcarse no presentada después de la tolerancia configurada.");
        }
        reservaRepository.actualizarEstado(reserva.id(), EstadoReserva.NO_PRESENTADA);
        reservaBloqueRepository.eliminarPorReserva(reserva.id());
        return reservaRepository.buscarPorId(reserva.id()).orElseThrow();
    }

    @Transactional
    public void registrarConsumos(long reservaId, Map<Long, Integer> cantidades) {
        Reserva reserva = reservaRepository.buscarPorIdParaActualizar(reservaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La reserva no existe."));
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(reserva.recursoId());
        if (recurso.tipo() != TipoRecurso.SALA_MINICINE || reserva.estado() != EstadoReserva.EN_USO) {
            throw new ReglaNegocioException("Solo se pueden registrar consumos para reservas de minicine en uso.");
        }

        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            if (item.getValue() == null || item.getValue() < 1) continue;
            Producto producto = productoRepository.buscarActivoPorId(item.getKey())
                    .orElseThrow(() -> new EntidadNoEncontradaException("Producto no disponible."));
            if (producto.existencias() < item.getValue()) {
                throw new ReglaNegocioException("Stock insuficiente para: " + producto.nombre());
            }
        }
        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            if (item.getValue() == null || item.getValue() < 1) continue;
            Producto producto = productoRepository.buscarActivoPorId(item.getKey()).orElseThrow();
            consumoRepository.insertar(new com.royaltyhouse.model.Consumo(
                    null, reservaId, producto.id(), item.getValue(), producto.precio(), null));
            productoRepository.descontarStock(producto.id(), item.getValue());
        }
    }

    public Reserva buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new ReglaNegocioException("Ingrese el código.");
        return reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No se encontró la reserva."));
    }

    public List<Reserva> buscarPorNombre(String nombre) {
        return reservaRepository.buscarPorNombreCliente(nombre);
    }

    public List<Producto> listarProductos() {
        return productoRepository.listarActivos();
    }

    public BigDecimal costoMinicine(Reserva reserva) {
        var tarifa = tarifaRepository.buscarPorTipo(TipoRecurso.SALA_MINICINE)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe tarifa de minicine."));
        return tarifa.precioHora().multiply(BigDecimal.valueOf(reserva.asistentes() == null ? 0 : reserva.asistentes()));
    }

    private String generarCodigo() {
        char[] caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        SecureRandom random = new SecureRandom();
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 8; i++) codigo.append(caracteres[random.nextInt(caracteres.length)]);
        return codigo.toString();
    }
}
