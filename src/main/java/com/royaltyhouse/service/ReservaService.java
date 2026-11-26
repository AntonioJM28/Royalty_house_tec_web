package com.royaltyhouse.service;

import com.royaltyhouse.exception.EntidadNoEncontradaException;
import com.royaltyhouse.exception.HorarioNoDisponibleException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.DetalleReserva;
import com.royaltyhouse.model.EstadoReserva;
import com.royaltyhouse.model.Pelicula;
import com.royaltyhouse.model.Recurso;
import com.royaltyhouse.model.Reserva;
import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.ResumenReserva;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.model.Usuario;
import com.royaltyhouse.repository.ReservaBloqueRepository;
import com.royaltyhouse.repository.ReservaRepository;
import com.royaltyhouse.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    private static final char[] CARACTERES_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaBloqueRepository reservaBloqueRepository;
    private final DisponibilidadService disponibilidadService;
    private final Clock reloj;
    private final int maximoReservasActivas;
    private final int horasLimiteCancelacion;

    public ReservaService(
            UsuarioRepository usuarioRepository,
            ReservaRepository reservaRepository,
            ReservaBloqueRepository reservaBloqueRepository,
            DisponibilidadService disponibilidadService,
            Clock reloj,
            @Value("${royaltyhouse.reservas.maximo-activas-por-cliente:2}") int maximoReservasActivas,
            @Value("${royaltyhouse.reservas.horas-limite-cancelacion:2}") int horasLimiteCancelacion) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.reservaBloqueRepository = reservaBloqueRepository;
        this.disponibilidadService = disponibilidadService;
        this.reloj = reloj;
        if (maximoReservasActivas < 1 || horasLimiteCancelacion < 0) {
            throw new IllegalArgumentException("La configuración de reservas no es válida.");
        }
        this.maximoReservasActivas = maximoReservasActivas;
        this.horasLimiteCancelacion = horasLimiteCancelacion;
    }

    @Transactional
    public DetalleReserva reservarBillar(
            long usuarioId,
            long recursoId,
            LocalDate fecha,
            LocalTime horaInicio,
            int bloques) {
        if (bloques < 1 || bloques > 4) {
            throw new ReglaNegocioException("La duración de billar debe ser de 1 a 4 bloques.");
        }
        Usuario usuario = obtenerClienteBloqueado(usuarioId);
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(recursoId);
        if (recurso.tipo() != TipoRecurso.MESA_NORMAL && recurso.tipo() != TipoRecurso.MESA_VIP) {
            throw new ReglaNegocioException("El recurso seleccionado no es una mesa de billar.");
        }

        disponibilidadService.validarHorario(fecha, horaInicio, bloques, recurso); // RN 01 y RN 02
        validarLimiteReservas(usuarioId); // RN 11
        return crearReserva(usuario, recurso, null, fecha, horaInicio, bloques);
    }

    @Transactional
    public DetalleReserva reservarMinicine(
            long usuarioId,
            long recursoId,
            long peliculaId,
            LocalDate fecha,
            LocalTime horaInicio) {
        Usuario usuario = obtenerClienteBloqueado(usuarioId);
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(recursoId);
        if (recurso.tipo() != TipoRecurso.SALA_MINICINE) {
            throw new ReglaNegocioException("El recurso seleccionado no es una sala de minicine.");
        }
        Pelicula pelicula = disponibilidadService.obtenerPeliculaEnCartelera(peliculaId);
        int bloques = disponibilidadService.calcularBloques(pelicula); // RN 05

        disponibilidadService.validarHorario(fecha, horaInicio, bloques, recurso); // RN 01 y RN 02
        validarLimiteReservas(usuarioId); // RN 11
        return crearReserva(usuario, recurso, pelicula, fecha, horaInicio, bloques);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarMisReservas(long usuarioId) {
        return reservaRepository.listarPorUsuario(usuarioId);
    }

    public boolean puedeCancelar(Reserva reserva) {
        if (reserva.estado() != EstadoReserva.CONFIRMADA) {
            return false;
        }
        LocalDateTime limite = LocalDateTime.of(reserva.fecha(), reserva.horaInicio())
                .minusHours(horasLimiteCancelacion);
        return !LocalDateTime.now(reloj).isAfter(limite);
    }

    @Transactional(readOnly = true)
    public DetalleReserva obtenerDetalle(long usuarioId, long reservaId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .filter(candidata -> candidata.usuarioId() != null && candidata.usuarioId() == usuarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La reserva no existe."));
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(reserva.recursoId());
        Pelicula pelicula = reserva.peliculaId() == null
                ? null
                : disponibilidadService.obtenerPeliculaEnCartelera(reserva.peliculaId());
        return new DetalleReserva(
                reserva,
                recurso,
                pelicula,
                reserva.horaInicio().plusHours(reserva.bloques()),
                disponibilidadService.calcularCosto(recurso.tipo(), reserva.bloques()));
    }

    @Transactional(readOnly = true)
    public ResumenReserva resumirBillar(long recursoId, LocalDate fecha, LocalTime horaInicio, int bloques) {
        if (bloques < 1 || bloques > 4) {
            throw new ReglaNegocioException("La duración de billar debe ser de 1 a 4 bloques.");
        }
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(recursoId);
        if (recurso.tipo() != TipoRecurso.MESA_NORMAL && recurso.tipo() != TipoRecurso.MESA_VIP) {
            throw new ReglaNegocioException("El recurso seleccionado no es una mesa de billar.");
        }
        disponibilidadService.validarHorario(fecha, horaInicio, bloques, recurso);
        return new ResumenReserva(
                recurso,
                null,
                fecha,
                horaInicio,
                horaInicio.plusHours(bloques),
                bloques,
                disponibilidadService.calcularCosto(recurso.tipo(), bloques));
    }

    @Transactional(readOnly = true)
    public ResumenReserva resumirMinicine(
            long recursoId,
            long peliculaId,
            LocalDate fecha,
            LocalTime horaInicio) {
        Recurso recurso = disponibilidadService.obtenerRecursoActivo(recursoId);
        if (recurso.tipo() != TipoRecurso.SALA_MINICINE) {
            throw new ReglaNegocioException("El recurso seleccionado no es una sala de minicine.");
        }
        Pelicula pelicula = disponibilidadService.obtenerPeliculaEnCartelera(peliculaId);
        int bloques = disponibilidadService.calcularBloques(pelicula);
        disponibilidadService.validarHorario(fecha, horaInicio, bloques, recurso);
        return new ResumenReserva(
                recurso,
                pelicula,
                fecha,
                horaInicio,
                horaInicio.plusHours(bloques),
                bloques,
                disponibilidadService.calcularCosto(recurso.tipo(), bloques));
    }

    @Transactional
    public Reserva cancelar(long usuarioId, long reservaId) {
        Reserva reserva = reservaRepository.buscarPorIdParaActualizar(reservaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La reserva no existe."));
        if (reserva.usuarioId() == null || reserva.usuarioId() != usuarioId) {
            throw new EntidadNoEncontradaException("La reserva no existe.");
        }
        if (reserva.estado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas confirmadas.");
        }

        if (!puedeCancelar(reserva)) {
            throw new ReglaNegocioException(
                    "La reserva solo se puede cancelar hasta " + horasLimiteCancelacion + " horas antes del inicio.");
        }

        reservaRepository.actualizarEstado(reservaId, EstadoReserva.CANCELADA);
        reservaBloqueRepository.eliminarPorReserva(reservaId);
        return reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("No se pudo recuperar la reserva cancelada."));
    }

    private Usuario obtenerClienteBloqueado(long usuarioId) {
        Usuario usuario = usuarioRepository.buscarPorIdParaActualizar(usuarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La cuenta de cliente no existe."));
        if (!usuario.activo() || usuario.rol() != Rol.CLIENTE) {
            throw new ReglaNegocioException("La cuenta no puede realizar reservas.");
        }
        return usuario;
    }

    private void validarLimiteReservas(long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int activas = reservaRepository.contarConfirmadasFuturas(usuarioId, ahora.toLocalDate(), ahora.toLocalTime());
        if (activas >= maximoReservasActivas) {
            throw new ReglaNegocioException(
                    "Cada cliente puede tener como máximo " + maximoReservasActivas + " reservas activas futuras.");
        }
    }

    private DetalleReserva crearReserva(
            Usuario usuario,
            Recurso recurso,
            Pelicula pelicula,
            LocalDate fecha,
            LocalTime horaInicio,
            int bloques) {
        Reserva nueva = new Reserva(
                null,
                generarCodigo(),
                usuario.id(),
                usuario.nombre(),
                usuario.telefono(),
                recurso.id(),
                pelicula == null ? null : pelicula.id(),
                fecha,
                horaInicio,
                bloques,
                EstadoReserva.CONFIRMADA,
                null,
                null,
                null);
        long reservaId = reservaRepository.insertar(nueva);
        for (int offset = 0; offset < bloques; offset++) {
            int bloque = horaInicio.plusHours(offset).getHour();
            reservaBloqueRepository.insertar(reservaId, recurso.id(), fecha, bloque);
        }

        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("No se pudo recuperar la reserva creada."));
        return new DetalleReserva(
                reserva,
                recurso,
                pelicula,
                horaInicio.plusHours(bloques),
                disponibilidadService.calcularCosto(recurso.tipo(), bloques));
    }

    private String generarCodigo() {
        StringBuilder codigo = new StringBuilder(8);
        for (int indice = 0; indice < 8; indice++) {
            codigo.append(CARACTERES_CODIGO[RANDOM.nextInt(CARACTERES_CODIGO.length)]);
        }
        return codigo.toString();
    }
}
