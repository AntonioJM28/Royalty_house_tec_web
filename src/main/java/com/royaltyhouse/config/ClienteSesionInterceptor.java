package com.royaltyhouse.config;

import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClienteSesionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        Object usuarioEnSesion = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(SesionWeb.USUARIO);

        if (!(usuarioEnSesion instanceof UsuarioSesion usuario) || usuario.rol() != Rol.CLIENTE) {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }
}
