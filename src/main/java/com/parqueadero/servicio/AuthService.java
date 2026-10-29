package com.parqueadero.servicio;

import com.parqueadero.dao.UsuarioDAO;
import com.parqueadero.modelo.RolUsuario;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.util.PasswordUtils;

import java.util.List;

public class AuthService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public boolean existeAlMenosUnUsuario() {
        return usuarioDAO.contarUsuarios() > 0;
    }

    /** Lo uso solo en el primer arranque, para crear la cuenta de administrador inicial. */
    public void registrarAdministrador(String usuario, String contrasena) {
        registrar(usuario, contrasena, RolUsuario.ADMIN);
    }

    public void registrar(String usuario, String contrasena, RolUsuario rol) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (contrasena == null || contrasena.length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }
        String usuarioLimpio = usuario.trim();
        if (usuarioDAO.buscarPorUsuario(usuarioLimpio) != null) {
            throw new IllegalStateException("Ya existe un usuario con ese nombre");
        }
        // Guardo con PBKDF2; la sal va dentro del hash, por eso la columna salt queda vacía.
        usuarioDAO.crear(usuarioLimpio, PasswordUtils.hashear(contrasena), "", rol);
    }

    /**
     * Valido el usuario y la contraseña. Si la contraseña estaba guardada con el formato viejo
     * (SHA-256) y es correcta, la vuelvo a guardar en el formato nuevo sin que el usuario note nada.
     *
     * @return el usuario autenticado (con su rol), o {@code null} si las credenciales no son válidas.
     */
    public Usuario autenticar(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank() || contrasena == null) {
            return null;
        }
        Usuario u = usuarioDAO.buscarPorUsuario(usuario.trim());
        if (u == null || !PasswordUtils.verificar(contrasena, u.getHashContrasena(), u.getSalt())) {
            return null;
        }
        return migrarHashSiHaceFalta(u, contrasena);
    }

    /**
     * Si el hash es del formato viejo (o tiene menos iteraciones de las actuales) lo vuelvo a guardar.
     * Si por algún motivo no puedo escribir en la base, el inicio de sesión sigue funcionando igual
     * y lo intento de nuevo la próxima vez.
     */
    private Usuario migrarHashSiHaceFalta(Usuario u, String contrasena) {
        if (!PasswordUtils.requiereActualizacion(u.getHashContrasena())) {
            return u;
        }
        try {
            String hashNuevo = PasswordUtils.hashear(contrasena);
            if (usuarioDAO.migrarHash(u.getId(), u.getHashContrasena(), hashNuevo)) {
                return new Usuario(u.getId(), u.getUsuario(), hashNuevo, "", u.getRol());
            }
        } catch (RuntimeException e) {
            System.err.println("No pude actualizar el formato de la contraseña de " + u.getUsuario()
                    + ": " + e.getMessage());
        }
        return u;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioDAO.listarTodos();
    }

    public void cambiarRol(int idUsuario, RolUsuario nuevoRol) {
        Usuario usuario = obtenerPorId(idUsuario);
        if (usuario.getRol() == RolUsuario.ADMIN && nuevoRol != RolUsuario.ADMIN && usuarioDAO.contarAdmins() <= 1) {
            throw new IllegalStateException("Debe quedar al menos un administrador");
        }
        usuarioDAO.actualizarRol(idUsuario, nuevoRol);
    }

    public void renombrarUsuario(int idUsuario, String nuevoUsuario) {
        if (nuevoUsuario == null || nuevoUsuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        String limpio = nuevoUsuario.trim();
        Usuario existente = usuarioDAO.buscarPorUsuario(limpio);
        if (existente != null && existente.getId() != idUsuario) {
            throw new IllegalStateException("Ya existe un usuario con ese nombre");
        }
        usuarioDAO.actualizarUsuario(idUsuario, limpio);
    }

    public void restablecerContrasena(int idUsuario, String nuevaContrasena) {
        if (nuevaContrasena == null || nuevaContrasena.length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }
        // Siempre guardo en el formato nuevo (PBKDF2), con la sal dentro del hash.
        usuarioDAO.actualizarContrasena(idUsuario, PasswordUtils.hashear(nuevaContrasena), "");
    }

    public void eliminarUsuario(int idUsuario, int idUsuarioActual) {
        if (idUsuario == idUsuarioActual) {
            throw new IllegalStateException("No puedes eliminar tu propio usuario mientras tienes la sesión iniciada");
        }
        Usuario usuario = obtenerPorId(idUsuario);
        if (usuario.getRol() == RolUsuario.ADMIN && usuarioDAO.contarAdmins() <= 1) {
            throw new IllegalStateException("Debe quedar al menos un administrador");
        }
        usuarioDAO.eliminar(idUsuario);
    }

    private Usuario obtenerPorId(int id) {
        return usuarioDAO.listarTodos().stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}
