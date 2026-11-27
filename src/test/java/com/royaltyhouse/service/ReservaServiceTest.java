package com.royaltyhouse.service;

import com.royaltyhouse.exception.HorarioNoDisponibleException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.EstadoReserva;
import com.royaltyhouse.model.Recurso;
import com.royaltyhouse.model.Reserva;
import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.model.Usuario;
import com.royaltyhouse.repository.ReservaBloqueRepository;
import com.royaltyhouse.repository.ReservaRepository;
import com.royaltyhouse.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final long USUARIO_ID = 10L;
    private static final long RECURSO_ID = 20L;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    private static final LocalDate MANANA = HOY.plusDays(1);
    private static final Clock RELOJ_FIJO = Clock.fixed(
            Instant.parse("2026-11-20T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ReservaBloqueRepository reservaBloqueRepository;

    @Mock
    private DisponibilidadService disponibilidadService;

    private ReservaService reservaService;

    @BeforeEach
    void configurarServicio() {
        reservaService = new ReservaService(
                usuarioRepository,
                reservaRepository,
                reservaBloqueRepository,
                disponibilidadService,
                RELOJ_FIJO,
                2,
                2);
    }

    @Test
    void noReservaCuandoElHorarioTieneSuperposicion() {
        Usuario usuario = clienteActivo();
        Recurso recurso = new Recurso(RECURSO_ID, "Mesa normal 1", TipoRecurso.MESA_NORMAL, 4, true);

        when(usuarioRepository.buscarPorIdParaActualizar(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(disponibilidadService.obtenerRecursoActivo(RECURSO_ID)).thenReturn(recurso);
        org.mockito.Mockito.doThrow(new HorarioNoDisponibleException())
                .when(disponibilidadService)
                .validarHorario(MANANA, LocalTime.of(10, 0), 2, recurso);

        assertThrows(
                HorarioNoDisponibleException.class,
                () -> reservaService.reservarBillar(
                        USUARIO_ID,
                        RECURSO_ID,
                        MANANA,
                        LocalTime.of(10, 0),
                        2));

        verify(reservaRepository, never()).insertar(any(Reserva.class));
        verify(reservaRepository, never()).contarConfirmadasFuturas(
                eq(USUARIO_ID), any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void rechazaReservaCuandoClienteYaTieneDosReservasActivas() {
        Usuario usuario = clienteActivo();
        Recurso recurso = new Recurso(RECURSO_ID, "Mesa normal 1", TipoRecurso.MESA_NORMAL, 4, true);

        when(usuarioRepository.buscarPorIdParaActualizar(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(disponibilidadService.obtenerRecursoActivo(RECURSO_ID)).thenReturn(recurso);
        when(reservaRepository.contarConfirmadasFuturas(
                USUARIO_ID, HOY, LocalTime.NOON)).thenReturn(2);

        assertThrows(
                ReglaNegocioException.class,
                () -> reservaService.reservarBillar(
                        USUARIO_ID,
                        RECURSO_ID,
                        MANANA,
                        LocalTime.of(10, 0),
                        1));

        verify(reservaRepository, never()).insertar(any(Reserva.class));
        verify(reservaBloqueRepository, never()).insertar(
                any(Long.class), any(Long.class), any(LocalDate.class), any(Integer.class));
    }

    @Test
    void rechazaCancelacionHechaDentroDeLasDosHorasPrevias() {
        Reserva reservaTardia = new Reserva(
                30L,
                "ABC23456",
                USUARIO_ID,
                "Cliente de prueba",
                "555-0100",
                RECURSO_ID,
                null,
                HOY,
                LocalTime.of(13, 30),
                1,
                EstadoReserva.CONFIRMADA,
                null,
                null,
                null,
                null);
        when(reservaRepository.buscarPorIdParaActualizar(30L)).thenReturn(Optional.of(reservaTardia));

        assertThrows(
                ReglaNegocioException.class,
                () -> reservaService.cancelar(USUARIO_ID, 30L));

        verify(reservaRepository, never()).actualizarEstado(30L, EstadoReserva.CANCELADA);
        verify(reservaBloqueRepository, never()).eliminarPorReserva(30L);
    }

    private Usuario clienteActivo() {
        return new Usuario(
                USUARIO_ID,
                "Cliente de prueba",
                "cliente@royaltyhouse.local",
                "555-0100",
                "hash",
                Rol.CLIENTE,
                true,
                true);
    }
}
