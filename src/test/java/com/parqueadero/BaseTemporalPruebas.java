package com.parqueadero;

import com.parqueadero.dao.ConexionBD;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prepara UNA sola base de datos temporal para todas mis pruebas que usan SQLite.
 *
 * <p>{@code ConexionBD} lee {@code user.home} una única vez, cuando se carga la clase, y Surefire corre
 * todas las pruebas en la misma JVM. Por eso todas las clases de prueba con base de datos llaman a
 * {@link #preparar()}: la primera crea la carpeta temporal y fija {@code user.home}; las demás reutilizan
 * la misma. Así nunca toco el {@code ~/parqueadero.db} real.</p>
 */
public final class BaseTemporalPruebas {

    private static Path casaTemporal;

    private BaseTemporalPruebas() {
    }

    /** Fijo {@code user.home} a una carpeta temporal (solo la primera vez) y creo las tablas. */
    public static synchronized void preparar() {
        if (casaTemporal == null) {
            try {
                casaTemporal = Files.createTempDirectory("parqueadero-test");
            } catch (java.io.IOException e) {
                throw new IllegalStateException("No pude crear la carpeta temporal de pruebas", e);
            }
            System.setProperty("user.home", casaTemporal.toString());
        }
        ConexionBD.inicializar();
        // Seguro extra: si por algún motivo ConexionBD ya se había cargado con otra carpeta, no sigo.
        if (!ConexionBD.rutaArchivoBD().startsWith(casaTemporal)) {
            throw new IllegalStateException("ConexionBD apunta a " + ConexionBD.rutaArchivoBD()
                    + " y no a la base temporal; me detengo para no tocar la base real");
        }
    }
}
