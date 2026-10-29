package com.parqueadero.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Valido la clave de activación contra la clave PÚBLICA de mi licencia (firma digital Ed25519).
 *
 * Aquí solo guardo la pública: sirve para comprobar que una clave fue firmada por mí, pero no
 * para crear ninguna. La clave privada vive únicamente en {@code herramienta-licencias/}, fuera
 * de src/ y fuera de GitHub, así que aunque alguien desarme el programa no puede fabricar
 * licencias.
 *
 * Formato de la clave de activación: la firma Ed25519 (64 bytes) de los bytes UTF-8 de
 * {@code "PARQUEADERO|" + ID de instalación en mayúsculas}, escrita en Base32 con el alfabeto
 * {@code ABCDEFGHJKLMNPQRSTUVWXYZ23456789} (sin 0/O/1/I para no confundirlos) y separada en
 * grupos de 5 caracteres con guion. Mide 103 caracteres más 20 guiones.
 *
 * Al leer lo que el cliente pega tolero espacios, saltos de línea, guiones y minúsculas.
 */
public final class ValidadorLicencia {

    /** Clave pública de producción (Base64 del formato X.509). Es pública a propósito. */
    private static final String CLAVE_PUBLICA_BASE64 = "MCowBQYDK2VwAyEAIaCS1lohO/zByuYHGPZMevFxEim4GMgnfLueN/wCaek=";

    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PREFIJO_MENSAJE = "PARQUEADERO|";
    private static final int BYTES_FIRMA = 64;

    private final PublicKey clavePublica;

    /** Creo un validador con la clave pública que le paso (la uso en las pruebas con un par efímero). */
    public ValidadorLicencia(PublicKey clavePublica) {
        this.clavePublica = clavePublica;
    }

    /** Creo un validador con una clave pública en Base64 (formato X.509). */
    public ValidadorLicencia(String clavePublicaBase64) {
        this(leerClavePublica(clavePublicaBase64));
    }

    /** Devuelvo el validador que usa la aplicación: el de mi clave pública de producción. */
    public static ValidadorLicencia porDefecto() {
        return new ValidadorLicencia(CLAVE_PUBLICA_BASE64);
    }

    /** Digo si la clave de activación la firmé yo para ese ID de instalación. */
    public boolean esValida(String idInstalacion, String claveActivacion) {
        if (idInstalacion == null || claveActivacion == null) {
            return false;
        }
        byte[] firma = decodificar(claveActivacion);
        if (firma == null) {
            return false;
        }
        try {
            Signature verificador = Signature.getInstance("Ed25519");
            verificador.initVerify(clavePublica);
            verificador.update(mensajeAFirmar(idInstalacion));
            return verificador.verify(firma);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** Devuelvo lo que firmo: "PARQUEADERO|" + ID normalizado, en UTF-8. */
    public static byte[] mensajeAFirmar(String idInstalacion) {
        return (PREFIJO_MENSAJE + normalizarId(idInstalacion)).getBytes(StandardCharsets.UTF_8);
    }

    /** Devuelvo el ID sin espacios y en mayúsculas, para que dé igual cómo lo escriban. */
    public static String normalizarId(String idInstalacion) {
        return idInstalacion.replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * Devuelvo la clave en su forma canónica (mayúsculas, grupos de 5 con guion) o null si no
     * tiene un formato válido. Así guardo siempre lo mismo sin importar cómo la hayan pegado.
     */
    public static String normalizarClave(String claveActivacion) {
        byte[] firma = decodificar(claveActivacion);
        return firma == null ? null : codificar(firma);
    }

    /** Escribo los 64 bytes de la firma en Base32 sin caracteres confusos, en grupos de 5 con guion. */
    public static String codificar(byte[] datos) {
        StringBuilder sb = new StringBuilder();
        int acumulado = 0;
        int bits = 0;
        for (byte b : datos) {
            acumulado = (acumulado << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALFABETO.charAt((acumulado >> (bits - 5)) & 31));
                bits -= 5;
            }
            acumulado &= (1 << bits) - 1;
        }
        if (bits > 0) {
            sb.append(ALFABETO.charAt((acumulado << (5 - bits)) & 31));
        }
        return sb.toString().replaceAll("(.{5})(?!$)", "$1-");
    }

    /**
     * Leo lo que pegó el cliente: quito espacios, saltos de línea y guiones, paso a mayúsculas y
     * convierto de Base32 a los 64 bytes de la firma. Devuelvo null si algo no cuadra (un
     * carácter fuera del alfabeto, longitud distinta o bits de relleno distintos de cero).
     */
    static byte[] decodificar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replaceAll("[\\s-]+", "").toUpperCase();
        // 64 bytes = 512 bits = 102 caracteres completos + 1 con 2 bits útiles y 3 de relleno.
        if (limpio.length() != (BYTES_FIRMA * 8 + 4) / 5) {
            return null;
        }
        byte[] salida = new byte[BYTES_FIRMA];
        int acumulado = 0;
        int bits = 0;
        int posicion = 0;
        for (int i = 0; i < limpio.length(); i++) {
            int valor = ALFABETO.indexOf(limpio.charAt(i));
            if (valor < 0) {
                return null;
            }
            acumulado = (acumulado << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                if (posicion >= BYTES_FIRMA) {
                    return null;
                }
                salida[posicion++] = (byte) ((acumulado >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
            acumulado &= (1 << bits) - 1;
        }
        if (posicion != BYTES_FIRMA || acumulado != 0) {
            return null; // faltan bytes o los bits de relleno no son cero
        }
        return salida;
    }

    private static PublicKey leerClavePublica(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("La clave pública de la licencia no es válida", e);
        }
    }
}
