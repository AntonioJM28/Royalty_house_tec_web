package com.royaltyhouse.config;

import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RolInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object sesion = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(SesionWeb.USUARIO);
        if (!(sesion instanceof UsuarioSesion usuario)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String ruta = request.getRequestURI();
        if (ruta.contains("/admin/") && usuario.rol() != Rol.ADMINISTRADOR) {
            response.sendRedirect(request.getContextPath() + rutaPrincipal(usuario.rol()));
            return false;
        }
        if (ruta.contains("/recepcion/")
                && usuario.rol() != Rol.RECEPCIONISTA
                && usuario.rol() != Rol.ADMINISTRADOR) {
            response.sendRedirect(request.getContextPath() + rutaPrincipal(usuario.rol()));
            return false;
        }
        return true;
    }

    public static String rutaPrincipal(Rol rol) {
        if (rol == Rol.ADMINISTRADOR) return "/admin";
        if (rol == Rol.RECEPCIONISTA) return "/recepcion";
        return "/cliente/disponibilidad";
    }
}
