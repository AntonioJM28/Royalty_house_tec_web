package com.royaltyhouse.service;

import com.royaltyhouse.exception.EntidadNoEncontradaException;
import com.royaltyhouse.exception.HorarioNoDisponibleException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.BloqueDisponibilidad;
import com.royaltyhouse.model.DisponibilidadDia;
import com.royaltyhouse.model.DisponibilidadRecurso;
import com.royaltyhouse.model.HorarioAtencion;
import com.royaltyhouse.model.Pelicula;
import com.royaltyhouse.model.Recurso;
import com.royaltyhouse.model.Tarifa;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.repository.HorarioAtencionRepository;
import com.royaltyhouse.repository.PeliculaRepository;
import com.royaltyhouse.repository.RecursoRepository;
import com.royaltyhouse.repository.ReservaBloqueRepository;
import com.royaltyhouse.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DisponibilidadService {

    private final RecursoRepository recursoRepository;
    private final ReservaBloqueRepository reservaBloqueRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final PeliculaRepository peliculaRepository;
    private final TarifaRepository tarifaRepository;
    private final Clock reloj;

    public DisponibilidadService(
            RecursoRepository recursoRepository,
            ReservaBloqueRepository reservaBloqueRepository,
            HorarioAtencionRepository horarioRepository,
            PeliculaRepository peliculaRepository,
            TarifaRepository tarifaRepository,
            Clock reloj) {
        this.recursoRepository = recursoRepository;
        this.reservaBloqueRepository = reservaBloqueRepository;
        this.horarioRepository = horarioRepository;
        this.peliculaRepository = peliculaRepository;
        this.tarifaRepository = tarifaRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<com.royaltyhouse.model.Recurso> obtenerRecursosActivos() {
        return recursoRepository.listarActivos();
    }

    public List<Pelicula> listarPeliculasEnCartelera() {
        return peliculaRepository.listarEnCartelera();
    }

    @Transactional(readOnly = true)
    public DisponibilidadDia consultarBillar(LocalDate fecha, TipoRecurso tipo) {
        if (tipo != TipoRecurso.MESA_NORMAL && tipo != TipoRecurso.MESA_VIP) {
            throw new ReglaNegocioException("El tipo de recurso seleccionado no corresponde a billar.");
        }
        validarFecha(fecha);
        return construirDisponibilidad(fecha, tipo, null, 1);
    }

    @Transactional(readOnly = true)
    public DisponibilidadDia consultarMinicine(LocalDate fecha, long peliculaId) {
        validarFecha(fecha);
        Pelicula pelicula = peliculaRepository.buscarEnCarteleraPorId(peliculaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La película no está disponible."));
        return construirDisponibilidad(fecha, TipoRecurso.SALA_MINICINE, pelicula, calcularBloques(pelicula));
    }

    public int calcularBloques(Pelicula pelicula) {
        return (pelicula.duracionMin() + 59) / 60;
    }

    public BigDecimal calcularCosto(TipoRecurso tipo, int bloques) {
        Tarifa tarifa = tarifaRepository.buscarPorTipo(tipo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No se encontró la tarifa del recurso."));
        return tarifa.precioHora().multiply(BigDecimal.valueOf(bloques));
    }

    public HorarioAtencion obtenerHorarioAtencion() {
        return horarioRepository.buscarHorario()
                .orElseThrow(() -> new EntidadNoEncontradaException("No se configuró el horario de atención."));
    }

    public Recurso obtenerRecursoActivo(long recursoId) {
        return recursoRepository.buscarActivoPorId(recursoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("El recurso no existe o no está activo."));
    }

    public Pelicula obtenerPeliculaEnCartelera(long peliculaId) {
        return peliculaRepository.buscarEnCarteleraPorId(peliculaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("La película no está disponible."));
    }

    public void validarHorario(LocalDate fecha, LocalTime inicio, int bloques, Recurso recurso) {
        validarFecha(fecha);
        if (inicio == null || bloques < 1 || bloques > 24 || inicio.getMinute() != 0
                || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new HorarioNoDisponibleException();
        }

        HorarioAtencion horario = obtenerHorarioAtencion();
        LocalTime fin;
        try {
            fin = inicio.plusHours(bloques);
        } catch (RuntimeException exception) {
            throw new HorarioNoDisponibleException();
        }
        if (inicio.isBefore(horario.horaApertura()) || !fin.isAfter(inicio) || fin.isAfter(horario.horaCierre())) {
            throw new HorarioNoDisponibleException();
        }

        if (fecha.equals(LocalDate.now(reloj))
                && !LocalDateTime.of(fecha, inicio).isAfter(LocalDateTime.now(reloj))) {
            throw new HorarioNoDisponibleException();
        }

        List<Integer> ocupados = reservaBloqueRepository.listarBloquesOcupados(recurso.id(), fecha);
        for (int numeroBloque : numerosBloque(inicio, bloques)) {
            if (ocupados.contains(numeroBloque)) {
                throw new HorarioNoDisponibleException();
            }
        }
    }

    private DisponibilidadDia construirDisponibilidad(
            LocalDate fecha,
            TipoRecurso tipo,
            Pelicula pelicula,
            int bloquesRequeridos) {
        HorarioAtencion horario = obtenerHorarioAtencion();
        List<Recurso> recursosActivos = recursoRepository.listarActivosPorTipo(tipo);
        List<DisponibilidadRecurso> recursos = new java.util.ArrayList<>();
        for (Recurso recurso : recursosActivos) {
            Set<Integer> ocupados = new HashSet<>(
                    reservaBloqueRepository.listarBloquesOcupados(recurso.id(), fecha));
            List<BloqueDisponibilidad> bloques = new java.util.ArrayList<>();
            for (LocalTime inicio = horario.horaApertura();
                 inicio.isBefore(horario.horaCierre());
                 inicio = inicio.plusHours(1)) {
                if (inicio.plusHours(1).isAfter(horario.horaCierre())) {
                    continue;
                }
                boolean disponible = !ocupados.contains(inicio.getHour())
                        && esHoraFutura(fecha, inicio);
                boolean inicioDisponible = disponible
                        && puedeIniciar(inicio, bloquesRequeridos, horario, ocupados, fecha);
                bloques.add(new BloqueDisponibilidad(inicio, disponible, inicioDisponible));
            }
            recursos.add(new DisponibilidadRecurso(recurso, List.copyOf(bloques)));
        }
        return new DisponibilidadDia(fecha, tipo, pelicula, bloquesRequeridos, recursos);
    }

    private boolean puedeIniciar(
            LocalTime inicio,
            int bloques,
            HorarioAtencion horario,
            Set<Integer> ocupados,
            LocalDate fecha) {
        LocalTime fin = inicio.plusHours(bloques);
        if (!fin.isAfter(inicio) || fin.isAfter(horario.horaCierre()) || !esHoraFutura(fecha, inicio)) {
            return false;
        }
        for (int offset = 0; offset < bloques; offset++) {
            if (ocupados.contains(inicio.plusHours(offset).getHour())) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> numerosBloque(LocalTime inicio, int bloques) {
        List<Integer> numeros = new java.util.ArrayList<>(bloques);
        for (int offset = 0; offset < bloques; offset++) {
            numeros.add(inicio.plusHours(offset).getHour());
        }
        return numeros;
    }

    private boolean esHoraFutura(LocalDate fecha, LocalTime hora) {
        return !fecha.equals(LocalDate.now(reloj))
                || LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now(reloj));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null || fecha.isBefore(LocalDate.now(reloj))) {
            throw new ReglaNegocioException("No se pueden consultar o reservar fechas pasadas.");
        }
    }
}
