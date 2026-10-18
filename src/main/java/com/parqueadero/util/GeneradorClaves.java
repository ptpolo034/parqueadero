package com.parqueadero.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Calcula la clave de activación que corresponde a un ID de instalación, usando una
 * clave secreta embebida en la aplicación (HMAC-SHA256). Esta clase solo VALIDA lo que
 * el cliente escribe. Para GENERAR claves para vender, usa la herramienta separada en
 * {@code herramienta-licencias/GeneradorLicencias.java} (en la raíz del proyecto, fuera
 * de src/ para que nunca quede empaquetada dentro del .jar que recibe el cliente).
 *
 * IMPORTANTE: cambia SECRETO por un valor propio, largo y que no compartas con nadie,
 * antes de vender el software — y cambia el mismo valor en la copia que tiene
 * herramienta-licencias/GeneradorLicencias.java, deben coincidir siempre.
 */
public final class GeneradorClaves {

    private static final String SECRETO = "KShl327qQOhNBoOUTGU-qqZu_4pYwqexw5NXQdxRT4w";

    private GeneradorClaves() {
    }

    public static String calcularClave(String idInstalacion) {
        try {
            String normalizado = idInstalacion.trim().toUpperCase().replace(" ", "");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(normalizado.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().withUpperCase().formatHex(hash).substring(0, 16);
            return hex.replaceAll("(.{4})(?!$)", "$1-");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular la clave", e);
        }
    }
}
