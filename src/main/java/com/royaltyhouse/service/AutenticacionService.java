package com.royaltyhouse.service;

import com.royaltyhouse.exception.CredencialesInvalidasException;
import com.royaltyhouse.exception.CuentaYaRegistradaException;
import com.royaltyhouse.exception.ReglaNegocioException;
import com.royaltyhouse.model.Rol;
import com.royaltyhouse.model.Usuario;
import com.royaltyhouse.model.UsuarioSesion;
import com.royaltyhouse.repository.UsuarioRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;

    public AutenticacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioSesion registrarCliente(
            String nombre,
            String correo,
            String telefono,
            String contrasena,
            boolean aceptoDatos) {
        if (!aceptoDatos) {
            throw new ReglaNegocioException("Debe aceptar el tratamiento de sus datos para registrarse.");
        }
        if (nombre == null || nombre.isBlank()
                || correo == null || correo.isBlank()
                || telefono == null || telefono.isBlank()
                || contrasena == null || contrasena.length() < 8) {
            throw new ReglaNegocioException("Revise los datos del registro; la contraseña debe tener al menos 8 caracteres.");
        }

        String correoNormalizado = normalizarCorreo(correo);
        Usuario nuevoUsuario = new Usuario(
                null,
                nombre.trim(),
                correoNormalizado,
                telefono.trim(),
                HashContrasena.crear(contrasena),
                Rol.CLIENTE,
                true,
                true);
        try {
            long id = usuarioRepository.insertar(nuevoUsuario);
            return new UsuarioSesion(id, nuevoUsuario.nombre(), nuevoUsuario.correo(), nuevoUsuario.rol());
        } catch (DuplicateKeyException exception) {
            throw new CuentaYaRegistradaException();
        }
    }

    @Transactional(readOnly = true)
    public UsuarioSesion iniciarSesion(String correo, String contrasena) {
        if (correo == null || correo.isBlank() || contrasena == null) {
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioRepository.buscarPorCorreo(normalizarCorreo(correo))
                .filter(Usuario::activo)
                .filter(cuenta -> HashContrasena.coincide(contrasena, cuenta.passwordHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        return new UsuarioSesion(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol());
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
