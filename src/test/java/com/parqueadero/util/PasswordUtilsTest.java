package com.parqueadero.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebo cómo guardo y verifico las contraseñas, incluido el formato viejo que debo seguir aceptando. */
class PasswordUtilsTest {

    @Test
    void elHashNuevoTieneElFormatoConVersionYSeVerifica() {
        String hash = PasswordUtils.hashear("secreta123");
        String[] partes = hash.split("\\$");
        assertEquals(4, partes.length);
        assertEquals("pbkdf2", partes[0]);
        assertEquals("310000", partes[1]);
        assertTrue(PasswordUtils.esFormatoNuevo(hash));
        assertFalse(PasswordUtils.requiereActualizacion(hash));
        assertTrue(PasswordUtils.verificar("secreta123", hash, ""));
    }

    @Test
    void unaContrasenaIncorrectaNoVerifica() {
        String hash = PasswordUtils.hashear("secreta123");
        assertFalse(PasswordUtils.verificar("otra", hash, ""));
        assertFalse(PasswordUtils.verificar("secreta124", hash, ""));
        assertFalse(PasswordUtils.verificar("", hash, ""));
        assertFalse(PasswordUtils.verificar(null, hash, ""));
    }

    @Test
    void laMismaClaveDaHashesDistintosPorLaSal() {
        String hash1 = PasswordUtils.hashear("igual");
        String hash2 = PasswordUtils.hashear("igual");
        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtils.verificar("igual", hash1, ""));
        assertTrue(PasswordUtils.verificar("igual", hash2, ""));
    }

    @Test
    void sigoAceptandoElHashViejoSha256() {
        String salt = PasswordUtils.generarSalt();
        String hashViejo = PasswordUtils.hashSha256Antiguo("secreta123", salt);
        assertFalse(PasswordUtils.esFormatoNuevo(hashViejo));
        assertTrue(PasswordUtils.requiereActualizacion(hashViejo));
        assertTrue(PasswordUtils.verificar("secreta123", hashViejo, salt));
        assertFalse(PasswordUtils.verificar("otra", hashViejo, salt));
        // Sin la sal no puedo comprobar el hash viejo.
        assertFalse(PasswordUtils.verificar("secreta123", hashViejo, ""));
    }

    @Test
    void unHashDanadoNuncaVerificaNiLanzaError() {
        assertFalse(PasswordUtils.verificar("x", "pbkdf2$310000$###$###", ""));
        assertFalse(PasswordUtils.verificar("x", "pbkdf2$abc$AAAA$AAAA", ""));
        assertFalse(PasswordUtils.verificar("x", "pbkdf2$310000$AAAA", ""));
        assertFalse(PasswordUtils.verificar("x", "pbkdf2$0$AAAA$AAAA", ""));
        assertFalse(PasswordUtils.verificar("x", null, ""));
        assertFalse(PasswordUtils.verificar("x", "basura", "###"));
    }

    @Test
    void unHashNuevoConMenosIteracionesSePideActualizar() {
        // Hash con 1000 iteraciones: lo verifico con sus propios parámetros, pero conviene reforzarlo.
        String hash = "pbkdf2$1000$" + java.util.Base64.getEncoder().encodeToString(new byte[16]) + "$"
                + java.util.Base64.getEncoder().encodeToString(derivarParaPrueba("clave", new byte[16], 1000));
        assertTrue(PasswordUtils.verificar("clave", hash, ""));
        assertTrue(PasswordUtils.requiereActualizacion(hash));
    }

    private static byte[] derivarParaPrueba(String clave, byte[] sal, int iteraciones) {
        try {
            var spec = new javax.crypto.spec.PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
            return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
