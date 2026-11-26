package com.royaltyhouse.controller;

import com.royaltyhouse.dto.form.ReservaBillarForm;
import com.royaltyhouse.dto.form.ReservaMinicineForm;
import com.royaltyhouse.config.SesionWeb;
import com.royaltyhouse.exception.EntidadNoEncontradaException;
import com.royaltyhouse.exception.HorarioNoDisponibleException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.DetalleReserva;
import com.royaltyhouse.model.DisponibilidadDia;
import com.royaltyhouse.model.ResumenReserva;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.model.UsuarioSesion;
import com.royaltyhouse.service.DisponibilidadService;
import com.royaltyhouse.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;
    private final int horasLimiteCancelacion;

    public ClienteController(
            DisponibilidadService disponibilidadService,
            ReservaService reservaService,
            @Value("${royaltyhouse.reservas.horas-limite-cancelacion:2}") int horasLimiteCancelacion) {
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.horasLimiteCancelacion = horasLimiteCancelacion;
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(
            @RequestParam(required = false) String servicio,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) Long peliculaId,
            Model model) {
        LocalDate fechaConsulta = fecha == null ? LocalDate.now() : fecha;
        model.addAttribute("servicio", servicio == null ? "" : servicio);
        model.addAttribute("fecha", fechaConsulta);
        model.addAttribute("fechaMinima", LocalDate.now());
        model.addAttribute("peliculas", disponibilidadService.listarPeliculasEnCartelera());
        model.addAttribute("tiposBillar", java.util.List.of(TipoRecurso.MESA_NORMAL, TipoRecurso.MESA_VIP));

        if ("billar".equalsIgnoreCase(servicio) && tipo != null) {
            DisponibilidadDia disponibilidad = disponibilidadService.consultarBillar(fechaConsulta, tipo);
            model.addAttribute("disponibilidad", disponibilidad);
            model.addAttribute("hayHorariosLibres", hayHorariosLibres(disponibilidad));
            model.addAttribute("tipo", tipo);
            ReservaBillarForm form = new ReservaBillarForm();
            form.setTipo(tipo);
            model.addAttribute("formBillar", form);
        } else if ("minicine".equalsIgnoreCase(servicio) && peliculaId != null) {
            DisponibilidadDia disponibilidad = disponibilidadService.consultarMinicine(fechaConsulta, peliculaId);
            model.addAttribute("disponibilidad", disponibilidad);
            model.addAttribute("hayHorariosLibres", hayHorariosLibres(disponibilidad));
            model.addAttribute("peliculaId", peliculaId);
            model.addAttribute("formMinicine", new ReservaMinicineForm());
        }
        return "cliente/disponibilidad";
    }

    @PostMapping("/reservas/billar/resumen")
    public String resumirBillar(
            @Valid @ModelAttribute("formBillar") ReservaBillarForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return recargarBillar(formulario, model, "Revise el recurso, fecha, hora y duración seleccionados.");
        }
        try {
            ResumenReserva resumen = reservaService.resumirBillar(
                    formulario.getRecursoId(),
                    formulario.getFecha(),
                    formulario.getHoraInicio(),
                    formulario.getBloques());
            model.addAttribute("resumen", resumen);
            model.addAttribute("tipoServicio", "billar");
            ReservaBillarForm confirmacion = new ReservaBillarForm();
            confirmacion.setRecursoId(formulario.getRecursoId());
            confirmacion.setFecha(formulario.getFecha());
            confirmacion.setHoraInicio(formulario.getHoraInicio());
            confirmacion.setBloques(formulario.getBloques());
            confirmacion.setTipo(formulario.getTipo());
            model.addAttribute("formBillar", confirmacion);
            return "cliente/resumen";
        } catch (HorarioNoDisponibleException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return redirigirDisponibilidadBillar(formulario);
        }
    }

    @PostMapping("/reservas/billar/confirmar")
    public String confirmarBillar(
            @Valid @ModelAttribute("formBillar") ReservaBillarForm formulario,
            BindingResult resultado,
            @SessionAttribute(SesionWeb.USUARIO) UsuarioSesion usuario,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return recargarBillar(formulario, model, "Los datos de la reserva no son válidos.");
        }
        try {
            DetalleReserva reserva = reservaService.reservarBillar(
                    usuario.id(),
                    formulario.getRecursoId(),
                    formulario.getFecha(),
                    formulario.getHoraInicio(),
                    formulario.getBloques());
            return "redirect:/cliente/reservas/" + reserva.reserva().id() + "/confirmacion";
        } catch (DuplicateKeyException | HorarioNoDisponibleException exception) {
            redirectAttributes.addFlashAttribute("error", "El horario fue ocupado. Selecciona otro bloque disponible.");
            return redirigirDisponibilidadBillar(formulario);
        }
    }

    @PostMapping("/reservas/minicine/resumen")
    public String resumirMinicine(
            @Valid @ModelAttribute("formMinicine") ReservaMinicineForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return recargarMinicine(formulario, model, "Revise el recurso, película, fecha y hora seleccionados.");
        }
        try {
            ResumenReserva resumen = reservaService.resumirMinicine(
                    formulario.getRecursoId(),
                    formulario.getPeliculaId(),
                    formulario.getFecha(),
                    formulario.getHoraInicio());
            model.addAttribute("resumen", resumen);
            model.addAttribute("tipoServicio", "minicine");
            ReservaMinicineForm confirmacion = new ReservaMinicineForm();
            confirmacion.setRecursoId(formulario.getRecursoId());
            confirmacion.setPeliculaId(formulario.getPeliculaId());
            confirmacion.setFecha(formulario.getFecha());
            confirmacion.setHoraInicio(formulario.getHoraInicio());
            model.addAttribute("formMinicine", confirmacion);
            return "cliente/resumen";
        } catch (HorarioNoDisponibleException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return redirigirDisponibilidadMinicine(formulario);
        }
    }

    @PostMapping("/reservas/minicine/confirmar")
    public String confirmarMinicine(
            @Valid @ModelAttribute("formMinicine") ReservaMinicineForm formulario,
            BindingResult resultado,
            @SessionAttribute(SesionWeb.USUARIO) UsuarioSesion usuario,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return recargarMinicine(formulario, model, "Los datos de la reserva no son válidos.");
        }
        try {
            DetalleReserva reserva = reservaService.reservarMinicine(
                    usuario.id(),
                    formulario.getRecursoId(),
                    formulario.getPeliculaId(),
                    formulario.getFecha(),
                    formulario.getHoraInicio());
            return "redirect:/cliente/reservas/" + reserva.reserva().id() + "/confirmacion";
        } catch (DuplicateKeyException | HorarioNoDisponibleException exception) {
            redirectAttributes.addFlashAttribute("error", "El horario fue ocupado. Selecciona otro bloque disponible.");
            return redirigirDisponibilidadMinicine(formulario);
        }
    }

    @GetMapping("/reservas")
    public String misReservas(
            @SessionAttribute(SesionWeb.USUARIO) UsuarioSesion usuario,
            Model model) {
        var reservas = reservaService.listarMisReservas(usuario.id());
        model.addAttribute("reservas", reservas);
        model.addAttribute("reservasCancelables", reservas.stream()
                .filter(reservaService::puedeCancelar)
                .map(reserva -> reserva.id())
                .collect(Collectors.toSet()));
        model.addAttribute("horasLimiteCancelacion", horasLimiteCancelacion);
        return "cliente/mis-reservas";
    }

    @PostMapping("/reservas/{reservaId}/cancelar")
    public String cancelar(
            @PathVariable long reservaId,
            @SessionAttribute(SesionWeb.USUARIO) UsuarioSesion usuario,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelar(usuario.id(), reservaId);
            redirectAttributes.addFlashAttribute("mensaje", "La reserva fue cancelada y el horario quedó libre.");
        } catch (ReglaNegocioException | EntidadNoEncontradaException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/cliente/reservas";
    }

    @GetMapping("/reservas/{reservaId}/confirmacion")
    public String confirmacion(
            @PathVariable long reservaId,
            @SessionAttribute(SesionWeb.USUARIO) UsuarioSesion usuario,
            Model model) {
        model.addAttribute("detalle", reservaService.obtenerDetalle(usuario.id(), reservaId));
        return "cliente/confirmacion";
    }

    private String recargarBillar(ReservaBillarForm formulario, Model model, String mensaje) {
        model.addAttribute("error", mensaje);
        model.addAttribute("servicio", "billar");
        model.addAttribute("fecha", formulario.getFecha());
        TipoRecurso tipo = formulario.getTipo() == TipoRecurso.MESA_VIP
                ? TipoRecurso.MESA_VIP : TipoRecurso.MESA_NORMAL;
        model.addAttribute("tipo", tipo);
        formulario.setTipo(tipo);
        model.addAttribute("tiposBillar", java.util.List.of(TipoRecurso.MESA_NORMAL, TipoRecurso.MESA_VIP));
        model.addAttribute("peliculas", disponibilidadService.listarPeliculasEnCartelera());
        if (formulario.getFecha() != null) {
            model.addAttribute("disponibilidad",
                    disponibilidadService.consultarBillar(formulario.getFecha(), tipo));
            model.addAttribute("hayHorariosLibres", hayHorariosLibres(
                    disponibilidadService.consultarBillar(formulario.getFecha(), tipo)));
        }
        return "cliente/disponibilidad";
    }

    private String recargarMinicine(ReservaMinicineForm formulario, Model model, String mensaje) {
        model.addAttribute("error", mensaje);
        model.addAttribute("servicio", "minicine");
        model.addAttribute("fecha", formulario.getFecha());
        model.addAttribute("peliculaId", formulario.getPeliculaId());
        model.addAttribute("peliculas", disponibilidadService.listarPeliculasEnCartelera());
        model.addAttribute("tiposBillar", java.util.List.of(TipoRecurso.MESA_NORMAL, TipoRecurso.MESA_VIP));
        if (formulario.getFecha() != null && formulario.getPeliculaId() != null) {
            model.addAttribute("disponibilidad",
                    disponibilidadService.consultarMinicine(formulario.getFecha(), formulario.getPeliculaId()));
            model.addAttribute("hayHorariosLibres", hayHorariosLibres(
                    disponibilidadService.consultarMinicine(formulario.getFecha(), formulario.getPeliculaId())));
        }
        return "cliente/disponibilidad";
    }

    private String redirigirDisponibilidadBillar(ReservaBillarForm formulario) {
        return "redirect:/cliente/disponibilidad?servicio=billar&fecha=" + formulario.getFecha()
                + "&tipo=" + formulario.getTipo();
    }

    private String redirigirDisponibilidadMinicine(ReservaMinicineForm formulario) {
        return "redirect:/cliente/disponibilidad?servicio=minicine&fecha=" + formulario.getFecha()
                + "&peliculaId=" + formulario.getPeliculaId();
    }

    private boolean hayHorariosLibres(DisponibilidadDia disponibilidad) {
        return disponibilidad.recursos().stream()
                .flatMap(recurso -> recurso.bloques().stream())
                .anyMatch(bloque -> bloque.inicioDisponible());
    }
}
