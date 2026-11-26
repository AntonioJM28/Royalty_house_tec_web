package com.royaltyhouse.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public String manejarBloqueOcupado(
            DuplicateKeyException exception,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "error",
                "El horario fue ocupado. La disponibilidad se ha actualizado.");

        String servicio = request.getParameter("servicio");
        String fecha = request.getParameter("fecha");
        String tipo = request.getParameter("tipo");
        String peliculaId = request.getParameter("peliculaId");
        UriComponentsBuilder destino = UriComponentsBuilder.fromPath("/cliente/disponibilidad");
        if (servicio != null) {
            destino.queryParam("servicio", servicio);
        }
        if (fecha != null) {
            destino.queryParam("fecha", fecha);
        }
        if (tipo != null) {
            destino.queryParam("tipo", tipo);
        }
        if (peliculaId != null) {
            destino.queryParam("peliculaId", peliculaId);
        }
        return "redirect:" + destino.build().encode().toUriString();
    }

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarReglaNegocio(ReglaNegocioException exception, Model model) {
        model.addAttribute("error", exception.getMessage());
        return "error/negocio";
    }

    @ExceptionHandler(EntidadNoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarEntidadNoEncontrada(EntidadNoEncontradaException exception, Model model) {
        model.addAttribute("error", exception.getMessage());
        return "error/negocio";
    }
}
