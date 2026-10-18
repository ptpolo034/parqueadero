package com.parqueadero.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {
    @Test
    void verificaLaContrasenaCorrecta() {
        String salt = PasswordUtils.generarSalt();
        String hash = PasswordUtils.hash("secreta123", salt);
        assertTrue(PasswordUtils.verificar("secreta123", salt, hash));
        assertFalse(PasswordUtils.verificar("otra", salt, hash));
    }

    @Test
    void laSalHaceQueElMismoTextoDeHashesDistintos() {
        String hash1 = PasswordUtils.hash("igual", PasswordUtils.generarSalt());
        String hash2 = PasswordUtils.hash("igual", PasswordUtils.generarSalt());
        assertNotEquals(hash1, hash2);
    }
}
