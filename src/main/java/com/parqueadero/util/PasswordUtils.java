package com.parqueadero.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Aquí manejo cómo guardo las contraseñas. Nunca guardo la contraseña en texto plano.
 *
 * <p>Formato nuevo: guardo la contraseña con PBKDF2WithHmacSHA256 (sal aleatoria de 16 bytes,
 * 310000 iteraciones, 256 bits) y todo va en un solo texto con su propia versión:
 * {@code pbkdf2$310000$<sal-base64>$<hash-base64>}. Como la sal y las iteraciones viajan dentro
 * del texto, más adelante puedo subir las iteraciones sin dejar a nadie fuera.</p>
 *
 * <p>Formato viejo: antes guardaba SHA-256(sal + contraseña) con la sal en la columna {@code salt}.
 * Lo sigo aceptando para no dejar fuera a los usuarios que ya existen; en cuanto uno de ellos
 * inicia sesión bien, {@code AuthService} lo vuelve a guardar en el formato nuevo.</p>
 */
public final class PasswordUtils {

    /** Etiqueta con la que reconozco el formato nuevo al inicio del hash guardado. */
    private static final String PREFIJO = "pbkdf2";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    /** Iteraciones con las que guardo cualquier contraseña nueva. */
    public static final int ITERACIONES = 310_000;
    /** Tope de cordura para no quedarme "pegado" si un hash guardado viniera dañado. */
    private static final int ITERACIONES_MAXIMAS = 50_000_000;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private PasswordUtils() {
    }

    /**
     * Guardo la contraseña con PBKDF2 y una sal nueva, así dos usuarios con la misma clave
     * (o la misma clave reescrita) nunca dan el mismo texto.
     *
     * @return el hash completo, listo para la columna {@code hash_contrasena}
     */
    public static String hashear(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(contrasena, sal, ITERACIONES, BITS_HASH);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Compruebo una contraseña contra lo que tengo guardado.
     * Si el hash es del formato nuevo lo verifico con PBKDF2; si es del formato viejo lo acepto
     * con SHA-256 y la sal de la columna {@code salt}. Siempre comparo en tiempo constante.
     *
     * @param hashGuardado valor de la columna {@code hash_contrasena}
     * @param saltViejo    valor de la columna {@code salt} (solo se usa con el formato viejo)
     * @return {@code true} si la contraseña coincide; {@code false} si no coincide o si el hash guardado es inválido
     */
    public static boolean verificar(String contrasena, String hashGuardado, String saltViejo) {
        if (contrasena == null || hashGuardado == null) {
            return false;
        }
        try {
            if (esFormatoNuevo(hashGuardado)) {
                return verificarPbkdf2(contrasena, hashGuardado);
            }
            if (saltViejo == null || saltViejo.isEmpty()) {
                return false;
            }
            byte[] calculado = hashSha256Antiguo(contrasena, saltViejo).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(calculado, hashGuardado.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            // Base64 dañado o parámetros imposibles: lo trato como contraseña incorrecta, no como un fallo.
            return false;
        }
    }

    /** Digo si el hash guardado ya lleva la etiqueta del formato nuevo. */
    public static boolean esFormatoNuevo(String hashGuardado) {
        return hashGuardado != null && hashGuardado.startsWith(PREFIJO + "$");
    }

    /**
     * Digo si debo volver a guardar este hash: cuando es del formato viejo, o del nuevo
     * pero con menos iteraciones de las que uso hoy.
     */
    public static boolean requiereActualizacion(String hashGuardado) {
        if (!esFormatoNuevo(hashGuardado)) {
            return true;
        }
        String[] partes = hashGuardado.split("\\$", -1);
        if (partes.length != 4) {
            return false; // Está dañado; no lo toco, de todos modos nunca va a verificar.
        }
        try {
            return Integer.parseInt(partes[1]) < ITERACIONES;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ---- Formato viejo (solo para aceptar a quienes ya existen) ----

    /** Genero una sal del formato viejo. Ya no la uso para usuarios nuevos; queda para las pruebas de compatibilidad. */
    public static String generarSalt() {
        byte[] salt = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Calculo el hash del formato viejo: SHA-256(sal + contraseña) en Base64. Solo para compatibilidad. */
    public static String hashSha256Antiguo(String contrasena, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hash = digest.digest(contrasena.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash no disponible", e);
        }
    }

    // ---- Interno ----

    private static boolean verificarPbkdf2(String contrasena, String hashGuardado) {
        String[] partes = hashGuardado.split("\\$", -1);
        if (partes.length != 4) {
            return false;
        }
        int iteraciones = Integer.parseInt(partes[1]);
        if (iteraciones <= 0 || iteraciones > ITERACIONES_MAXIMAS) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        if (sal.length == 0 || esperado.length == 0) {
            return false;
        }
        // Derivo con la misma longitud que tiene el hash guardado.
        byte[] calculado = derivar(contrasena, sal, iteraciones, esperado.length * 8);
        return MessageDigest.isEqual(calculado, esperado);
    }

    private static byte[] derivar(String contrasena, byte[] sal, int iteraciones, int bits) {
        char[] caracteres = contrasena.toCharArray();
        PBEKeySpec especificacion = new PBEKeySpec(caracteres, sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("PBKDF2 no está disponible en esta versión de Java", e);
        } finally {
            especificacion.clearPassword();
            java.util.Arrays.fill(caracteres, '\0');
        }
    }
}
