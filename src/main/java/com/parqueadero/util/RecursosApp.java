package com.parqueadero.util;

import javax.swing.ImageIcon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Aquí manejo los recursos que el cliente puede personalizar, como el logo del negocio, y los guardo
 * en una carpeta de datos del usuario para que no se pierdan entre ejecuciones.
 */
public final class RecursosApp {
    private static final Path CARPETA_DATOS = Path.of(System.getProperty("user.home"), ".parqueadero");
    private static final Path ARCHIVO_LOGO = CARPETA_DATOS.resolve("logo.img");

    private RecursosApp() {
    }

    public static boolean tieneLogo() {
        return Files.exists(ARCHIVO_LOGO);
    }

    public static ImageIcon cargarLogo() {
        if (!tieneLogo()) {
            return null;
        }
        return new ImageIcon(ARCHIVO_LOGO.toString());
    }

    public static void guardarLogo(Path archivoOrigen) throws IOException {
        Files.createDirectories(CARPETA_DATOS);
        Files.copy(archivoOrigen, ARCHIVO_LOGO, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void eliminarLogo() throws IOException {
        Files.deleteIfExists(ARCHIVO_LOGO);
    }
}
