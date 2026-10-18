package com.parqueadero.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad para almacenar contraseñas de forma segura: cada usuario recibe una
 * sal (salt) aleatoria y se guarda únicamente el hash SHA-256 de "contraseña + sal",
 * nunca la contraseña en texto plano.
 */
public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String generarSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String contrasena, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hash = digest.digest(contrasena.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash no disponible", e);
        }
    }

    public static boolean verificar(String contrasena, String salt, String hashEsperado) {
        return hash(contrasena, salt).equals(hashEsperado);
    }
}
