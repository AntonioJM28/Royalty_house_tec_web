package com.royaltyhouse.controller;

import com.royaltyhouse.model.Reserva;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.service.DisponibilidadService;
import com.royaltyhouse.service.RecepcionistaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/recepcion")
public class RecepcionistaController {

    private final RecepcionistaService recepcionistaService;
    private final DisponibilidadService disponibilidadService;

    public RecepcionistaController(RecepcionistaService recepcionistaService,
                                   DisponibilidadService disponibilidadService) {
        this.recepcionistaService = recepcionistaService;
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/recepcion/disponibilidad";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(@RequestParam(required = false) String servicio,
                                 @RequestParam(required = false) LocalDate fecha,
                                 @RequestParam(required = false) TipoRecurso tipo,
                                 @RequestParam(required = false) Long peliculaId,
                                 Model model) {
        LocalDate consulta = fecha == null ? LocalDate.now() : fecha;
        model.addAttribute("servicio", servicio == null ? "" : servicio);
        model.addAttribute("fecha", consulta);
        model.addAttribute("peliculas", disponibilidadService.listarPeliculasEnCartelera());
        model.addAttribute("tiposBillar", java.util.List.of(TipoRecurso.MESA_NORMAL, TipoRecurso.MESA_VIP));
        if ("billar".equalsIgnoreCase(servicio) && tipo != null) {
            model.addAttribute("disponibilidad", disponibilidadService.consultarBillar(consulta, tipo));
        } else if ("minicine".equalsIgnoreCase(servicio) && peliculaId != null) {
            model.addAttribute("disponibilidad", disponibilidadService.consultarMinicine(consulta, peliculaId));
        }
        return "recepcion/disponibilidad";
    }

    @GetMapping("/reserva/nueva")
    public String nuevaReserva(@RequestParam(required = false) Long recursoId,
                               @RequestParam(required = false) LocalDate fecha,
                               @RequestParam(required = false) LocalTime hora,
                               Model model) {
        model.addAttribute("recursos", disponibilidadService.obtenerRecursosActivos());
        model.addAttribute("peliculas", disponibilidadService.listarPeliculasEnCartelera());
        model.addAttribute("recursoId", recursoId);
        model.addAttribute("fecha", fecha == null ? LocalDate.now() : fecha);
        model.addAttribute("hora", hora);
        return "recepcion/nueva-reserva";
    }

    @GetMapping("/clientes/buscar")
    public String buscarClientes(@RequestParam(required = false) String texto, Model model) {
        if (texto != null && !texto.isBlank()) model.addAttribute("clientes", recepcionistaService.buscarClientes(texto));
        model.addAttribute("texto", texto);
        return "recepcion/buscar-clientes";
    }

    @PostMapping("/reserva/nueva")
    public String registrar(@RequestParam(required = false) Long usuarioId,
                            @RequestParam String nombre,
                            @RequestParam String telefono,
                            @RequestParam Long recursoId,
                            @RequestParam(required = false) Long peliculaId,
                            @RequestParam LocalDate fecha,
                            @RequestParam LocalTime hora,
                            @RequestParam(required = false) Integer bloques,
                            @RequestParam(required = false) Integer asistentes,
                            RedirectAttributes redirectAttributes) {
        try {
            Reserva reserva = recepcionistaService.registrarMostrador(usuarioId, nombre, telefono,
                    recursoId, peliculaId, fecha, hora, bloques, asistentes);
            redirectAttributes.addFlashAttribute("mensaje", "Reserva creada. Código: " + reserva.codigo());
            redirectAttributes.addFlashAttribute("reservaCreada", reserva);
            return "redirect:/recepcion/operacion?codigo=" + reserva.codigo();
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/recepcion/reserva/nueva";
        }
    }

    @GetMapping("/operacion")
    public String operacion(@RequestParam(required = false) String codigo,
                            @RequestParam(required = false) String nombre,
                            Model model) {
        if (codigo != null && !codigo.isBlank()) {
            try { model.addAttribute("reserva", recepcionistaService.buscarPorCodigo(codigo)); }
            catch (Exception exception) { model.addAttribute("error", exception.getMessage()); }
        } else if (nombre != null && !nombre.isBlank()) {
            model.addAttribute("reservas", recepcionistaService.buscarPorNombre(nombre));
        }
        return "recepcion/operacion";
    }

    @PostMapping("/operacion/entrada")
    public String entrada(@RequestParam String codigo, RedirectAttributes redirectAttributes) {
        try { recepcionistaService.marcarEntrada(codigo); redirectAttributes.addFlashAttribute("mensaje", "Entrada registrada."); }
        catch (Exception exception) { redirectAttributes.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/recepcion/operacion?codigo=" + codigo;
    }

    @PostMapping("/operacion/salida")
    public String salida(@RequestParam String codigo, RedirectAttributes redirectAttributes) {
        try { recepcionistaService.marcarSalida(codigo); redirectAttributes.addFlashAttribute("mensaje", "Salida registrada."); }
        catch (Exception exception) { redirectAttributes.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/recepcion/operacion?codigo=" + codigo;
    }

    @PostMapping("/operacion/no-presentada")
    public String noPresentada(@RequestParam String codigo, RedirectAttributes redirectAttributes) {
        try { recepcionistaService.marcarNoPresentada(codigo); redirectAttributes.addFlashAttribute("mensaje", "Reserva marcada como no presentada."); }
        catch (Exception exception) { redirectAttributes.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/recepcion/operacion?codigo=" + codigo;
    }

    @GetMapping("/consumos")
    public String consumos(@RequestParam(required = false) String codigo, Model model) {
        model.addAttribute("productos", recepcionistaService.listarProductos());
        if (codigo != null && !codigo.isBlank()) {
            try {
                Reserva reserva = recepcionistaService.buscarPorCodigo(codigo);
                model.addAttribute("reserva", reserva);
            } catch (Exception exception) { model.addAttribute("error", exception.getMessage()); }
        }
        return "recepcion/consumos";
    }

    @PostMapping("/consumos")
    public String registrarConsumos(@RequestParam long reservaId,
                                    @RequestParam Map<String, String> parametros,
                                    RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cantidades = new HashMap<>();
        parametros.forEach((clave, valor) -> {
            if (clave.startsWith("cantidad_")) {
                try {
                    int cantidad = Integer.parseInt(valor);
                    if (cantidad > 0) cantidades.put(Long.valueOf(clave.substring("cantidad_".length())), cantidad);
                } catch (NumberFormatException ignored) { }
            }
        });
        try {
            recepcionistaService.registrarConsumos(reservaId, cantidades);
            redirectAttributes.addFlashAttribute("mensaje", "Consumos registrados y stock descontado.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/recepcion/consumos";
    }
}
