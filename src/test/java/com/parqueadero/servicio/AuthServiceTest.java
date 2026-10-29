package com.parqueadero.servicio;

import com.parqueadero.BaseTemporalPruebas;
import com.parqueadero.dao.ConexionBD;
import com.parqueadero.dao.UsuarioDAO;
import com.parqueadero.modelo.RolUsuario;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.util.PasswordUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebo el inicio de sesión y la migración de contraseñas viejas contra una base temporal
 * (nunca toco la base real). Cada prueba usa nombres de usuario propios porque la base se comparte
 * con las demás pruebas.
 */
class AuthServiceTest {
    private static final AuthService AUTH = new AuthService();
    private static final UsuarioDAO DAO = new UsuarioDAO();

    @BeforeAll
    static void prepararBaseTemporal() {
        BaseTemporalPruebas.preparar();
    }

    /** Inserto un usuario como lo hacía la versión anterior: SHA-256 con la sal en la columna salt. */
    private static void crearUsuarioConHashViejo(String usuario, String contrasena, RolUsuario rol) throws Exception {
        String salt = PasswordUtils.generarSalt();
        String hashViejo = PasswordUtils.hashSha256Antiguo(contrasena, salt);
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO usuarios (usuario, hash_contrasena, salt, rol) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, usuario);
            ps.setString(2, hashViejo);
            ps.setString(3, salt);
            ps.setString(4, rol.name());
            ps.executeUpdate();
        }
    }

    @Test
    void usuarioNuevoSeGuardaConPbkdf2YPuedeIniciarSesion() {
        AUTH.registrar("nuevo1", "clave1234", RolUsuario.CAJERO);

        Usuario guardado = DAO.buscarPorUsuario("nuevo1");
        assertTrue(guardado.getHashContrasena().startsWith("pbkdf2$310000$"));
        assertEquals("", guardado.getSalt());

        Usuario u = AUTH.autenticar("nuevo1", "clave1234");
        assertNotNull(u);
        assertEquals(RolUsuario.CAJERO, u.getRol());
        // Si el hash ya es del formato nuevo, iniciar sesión no lo cambia.
        assertEquals(guardado.getHashContrasena(), DAO.buscarPorUsuario("nuevo1").getHashContrasena());
    }

    @Test
    void unaClaveMalEscritaNoAutentica() {
        AUTH.registrar("nuevo2", "clave1234", RolUsuario.CAJERO);
        assertNull(AUTH.autenticar("nuevo2", "clave1235"));
        assertNull(AUTH.autenticar("nuevo2", ""));
        assertNull(AUTH.autenticar("nuevo2", null));
        assertNull(AUTH.autenticar("noexiste", "clave1234"));
        assertNull(AUTH.autenticar(null, "clave1234"));
    }

    @Test
    void usuarioConHashViejoIniciaSesionYQuedaGuardadoEnFormatoNuevo() throws Exception {
        crearUsuarioConHashViejo("viejo1", "miClave99", RolUsuario.ADMIN);
        Usuario antes = DAO.buscarPorUsuario("viejo1");
        assertFalse(antes.getHashContrasena().startsWith("pbkdf2$"));
        assertFalse(antes.getSalt().isEmpty());

        // Con una clave incorrecta no entra y lo guardado queda igual.
        assertNull(AUTH.autenticar("viejo1", "otraClave"));
        assertEquals(antes.getHashContrasena(), DAO.buscarPorUsuario("viejo1").getHashContrasena());

        // Con la clave correcta entra y, sin avisarle, lo paso al formato nuevo.
        Usuario u = AUTH.autenticar("viejo1", "miClave99");
        assertNotNull(u);
        assertEquals(RolUsuario.ADMIN, u.getRol());

        Usuario despues = DAO.buscarPorUsuario("viejo1");
        assertTrue(despues.getHashContrasena().startsWith("pbkdf2$310000$"));
        assertNotEquals(antes.getHashContrasena(), despues.getHashContrasena());

        // Sigue entrando con la misma clave, ahora con el hash nuevo, y la incorrecta sigue sin entrar.
        assertNotNull(AUTH.autenticar("viejo1", "miClave99"));
        assertNull(AUTH.autenticar("viejo1", "otraClave"));
        // Y ya no lo vuelvo a reescribir.
        assertEquals(despues.getHashContrasena(), DAO.buscarPorUsuario("viejo1").getHashContrasena());
    }

    @Test
    void restablecerContrasenaGuardaFormatoNuevoTambienSobreUnHashViejo() throws Exception {
        crearUsuarioConHashViejo("viejo2", "claveVieja", RolUsuario.CAJERO);
        int id = DAO.buscarPorUsuario("viejo2").getId();

        AUTH.restablecerContrasena(id, "claveNueva1");

        Usuario despues = DAO.buscarPorUsuario("viejo2");
        assertTrue(despues.getHashContrasena().startsWith("pbkdf2$310000$"));
        assertNull(AUTH.autenticar("viejo2", "claveVieja"));
        assertNotNull(AUTH.autenticar("viejo2", "claveNueva1"));

        // Sigo exigiendo la longitud mínima de 4 caracteres.
        assertThrows(IllegalArgumentException.class, () -> AUTH.restablecerContrasena(id, "abc"));
        assertNotNull(AUTH.autenticar("viejo2", "claveNueva1"));
    }

    @Test
    void laMigracionNoPisaUnaContrasenaRestablecidaMientrasTanto() throws Exception {
        crearUsuarioConHashViejo("viejo3", "claveVieja", RolUsuario.CAJERO);
        Usuario leido = DAO.buscarPorUsuario("viejo3");

        // Alguien restablece la contraseña después de que yo leí el hash viejo.
        AUTH.restablecerContrasena(leido.getId(), "claveNueva1");

        // Mi intento de migrar con el hash viejo no debe pisar la clave nueva.
        assertFalse(DAO.migrarHash(leido.getId(), leido.getHashContrasena(), PasswordUtils.hashear("claveVieja")));
        assertNotNull(AUTH.autenticar("viejo3", "claveNueva1"));
        assertNull(AUTH.autenticar("viejo3", "claveVieja"));
    }

    @Test
    void dosUsuariosConLaMismaClaveGuardanHashesDistintos() {
        AUTH.registrar("igual1", "mismaClave", RolUsuario.CAJERO);
        AUTH.registrar("igual2", "mismaClave", RolUsuario.CAJERO);
        assertNotEquals(DAO.buscarPorUsuario("igual1").getHashContrasena(),
                DAO.buscarPorUsuario("igual2").getHashContrasena());
    }

    @Test
    void mantengoLaLongitudMinimaDeCuatroCaracteres() {
        assertThrows(IllegalArgumentException.class, () -> AUTH.registrar("corto1", "abc", RolUsuario.CAJERO));
        AUTH.registrar("corto2", "abcd", RolUsuario.CAJERO);
        assertNotNull(AUTH.autenticar("corto2", "abcd"));
    }
}
