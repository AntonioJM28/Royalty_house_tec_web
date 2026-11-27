package com.royaltyhouse.controller;

import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.TipoRecurso;
import com.royaltyhouse.service.AdministradorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin")
public class AdministradorController {
    private final AdministradorService service;
    private final com.royaltyhouse.service.ReporteService reporteService;

    public AdministradorController(AdministradorService service,
                                   com.royaltyhouse.service.ReporteService reporteService) {
        this.service = service;
        this.reporteService = reporteService;
    }

    @GetMapping({"", "/"}) public String inicio(Model model) {
        model.addAttribute("personal", service.personal());
        return "admin/index";
    }

    @GetMapping("/mesas") public String mesas(Model model) { model.addAttribute("recursos", service.recursos()); return "admin/mesas"; }
    @PostMapping("/mesas") public String guardarMesa(@RequestParam(required=false) Long id, @RequestParam String nombre,
            @RequestParam TipoRecurso tipo, @RequestParam int capacidad, @RequestParam(defaultValue="false") boolean activo) {
        service.guardarRecurso(id,nombre,tipo,capacidad,activo); return "redirect:/admin/mesas";
    }

    @GetMapping("/peliculas") public String peliculas(Model model) { model.addAttribute("peliculas", service.peliculas()); return "admin/peliculas"; }
    @PostMapping("/peliculas") public String guardarPelicula(@RequestParam(required=false) Long id, @RequestParam String titulo,
            @RequestParam int duracion, @RequestParam(defaultValue="false") boolean cartelera) {
        service.guardarPelicula(id,titulo,duracion,cartelera); return "redirect:/admin/peliculas";
    }

    @GetMapping("/tarifas") public String tarifas(Model model) { model.addAttribute("tarifas", service.tarifas()); return "admin/tarifas"; }
    @PostMapping("/tarifas") public String guardarTarifa(@RequestParam TipoRecurso tipo, @RequestParam BigDecimal precio) {
        service.actualizarTarifa(tipo,precio); return "redirect:/admin/tarifas";
    }

    @GetMapping("/horario") public String horario(Model model) { model.addAttribute("horario", service.horario()); return "admin/horario"; }
    @PostMapping("/horario") public String guardarHorario(@RequestParam LocalTime apertura, @RequestParam LocalTime cierre) {
        service.actualizarHorario(apertura,cierre); return "redirect:/admin/horario";
    }

    @GetMapping("/productos") public String productos(Model model) { model.addAttribute("productos", service.productos()); return "admin/productos"; }
    @PostMapping("/productos") public String guardarProducto(@RequestParam(required=false) Long id, @RequestParam String nombre,
            @RequestParam BigDecimal precio, @RequestParam int stock, @RequestParam(defaultValue="false") boolean activo) {
        service.guardarProducto(id,nombre,precio,stock,activo); return "redirect:/admin/productos";
    }

    @GetMapping("/reportes") public String reportes(@RequestParam(required=false) java.time.LocalDate desde,
            @RequestParam(required=false) java.time.LocalDate hasta, Model model) {
        java.time.LocalDate inicio = desde == null ? java.time.LocalDate.now().minusDays(30) : desde;
        java.time.LocalDate fin = hasta == null ? java.time.LocalDate.now() : hasta;
        model.addAttribute("desde", inicio); model.addAttribute("hasta", fin);
        model.addAttribute("reportes", reporteService.generar(inicio, fin));
        return "admin/reportes";
    }

    @GetMapping("/personal") public String personal(Model model) { model.addAttribute("personal", service.personal()); return "admin/personal"; }
    @PostMapping("/personal") public String guardarPersonal(@RequestParam(required=false) Long id,
            @RequestParam(required=false) String nombre, @RequestParam(required=false) String correo,
            @RequestParam(required=false) String telefono, @RequestParam Rol rol,
            @RequestParam(defaultValue="false") boolean activo, @RequestParam(required=false) String contrasena,
            RedirectAttributes redirectAttributes) {
        try {
            if (id == null) {
                if (contrasena == null || contrasena.length() < 8) throw new com.royaltyhouse.exception.ReglaNegocioException("La contraseña debe tener al menos 8 caracteres.");
                service.crearPersonal(nombre,correo,telefono,rol,contrasena);
            } else service.actualizarPersonal(id,rol,activo);
        } catch (Exception exception) { redirectAttributes.addFlashAttribute("error",exception.getMessage()); }
        return "redirect:/admin/personal";
    }
}
