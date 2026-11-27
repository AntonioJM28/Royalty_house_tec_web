package com.royaltyhouse.controller;

import com.royaltyhouse.config.SesionWeb;
import com.royaltyhouse.dto.form.LoginForm;
import com.royaltyhouse.dto.form.RegistroForm;
import com.royaltyhouse.exception.CredencialesInvalidasException;
import com.royaltyhouse.exception.CuentaYaRegistradaException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.UsuarioSesion;
import com.royaltyhouse.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @GetMapping("/")
    public String inicio(jakarta.servlet.http.HttpSession sesion) {
        Object usuario = sesion.getAttribute(SesionWeb.USUARIO);
        if (usuario instanceof UsuarioSesion autenticado) {
            return "redirect:" + com.royaltyhouse.config.RolInterceptor.rutaPrincipal(autenticado.rol());
        }
        return "redirect:/login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroForm", new RegistroForm());
        return "cliente/registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @Valid @ModelAttribute("registroForm") RegistroForm formulario,
            BindingResult resultado,
            HttpServletRequest request,
            Model model) {
        if (!formulario.isAceptoDatos()) {
            resultado.rejectValue("aceptoDatos", "datos.noAceptados",
                    "Debe aceptar el tratamiento de sus datos para registrarse.");
        }
        if (resultado.hasErrors()) {
            return "cliente/registro";
        }

        try {
            UsuarioSesion usuario = autenticacionService.registrarCliente(
                    formulario.getNombre(),
                    formulario.getCorreo(),
                    formulario.getTelefono(),
                    formulario.getContrasena(),
                    formulario.isAceptoDatos());
            iniciarSesion(request, usuario);
            return "redirect:" + com.royaltyhouse.config.RolInterceptor.rutaPrincipal(usuario.rol());
        } catch (CuentaYaRegistradaException | ReglaNegocioException exception) {
            model.addAttribute("error", exception.getMessage());
            return "cliente/registro";
        }
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "cliente/login";
    }

    @PostMapping("/login")
    public String iniciarSesion(
            @Valid @ModelAttribute("loginForm") LoginForm formulario,
            BindingResult resultado,
            HttpServletRequest request,
            Model model) {
        if (resultado.hasErrors()) {
            return "cliente/login";
        }

        try {
            UsuarioSesion usuario = autenticacionService.iniciarSesion(
                    formulario.getCorreo(),
                    formulario.getContrasena());
            iniciarSesion(request, usuario);
            return "redirect:" + com.royaltyhouse.config.RolInterceptor.rutaPrincipal(usuario.rol());
        } catch (CredencialesInvalidasException exception) {
            model.addAttribute("error", exception.getMessage());
            return "cliente/login";
        }
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("mensaje", "Has cerrado sesión.");
        return "redirect:/login";
    }

    private void iniciarSesion(HttpServletRequest request, UsuarioSesion usuario) {
        HttpSession sesionAnterior = request.getSession(false);
        if (sesionAnterior != null) {
            sesionAnterior.invalidate();
        }
        HttpSession sesion = request.getSession(true);
        request.changeSessionId();
        sesion.setAttribute(SesionWeb.USUARIO, usuario);
    }
}
