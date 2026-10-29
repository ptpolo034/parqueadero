package com.parqueadero.servicio;

import com.parqueadero.dao.LicenciaDAO;
import com.parqueadero.util.ValidadorLicencia;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LicenciaService {

    public static final int DIAS_PRUEBA = 30;

    private final LicenciaDAO licenciaDAO = new LicenciaDAO();
    private final ValidadorLicencia validador;

    /** Uso mi clave pública de producción para validar las claves de activación. */
    public LicenciaService() {
        this(ValidadorLicencia.porDefecto());
    }

    /** Me permite probar el servicio con otra clave pública (por ejemplo, un par efímero en las pruebas). */
    public LicenciaService(ValidadorLicencia validador) {
        this.validador = validador;
    }

    /**
     * Lo llamo una vez al arrancar la aplicación: avanzo la "última fecha vista" hasta
     * hoy (nunca hacia atrás), para que retrasar el reloj del sistema no reinicie la
     * cuenta de días de prueba.
     */
    public void verificarAlIniciar() {
        LicenciaDAO.RegistroLicencia registro = licenciaDAO.obtener();
        LocalDate hoy = LocalDate.now();
        if (hoy.isAfter(registro.ultimaFechaVista())) {
            licenciaDAO.actualizarUltimaFechaVista(hoy);
        }
    }

    public String idInstalacion() {
        return licenciaDAO.obtener().idInstalacion();
    }

    public boolean estaActivada() {
        LicenciaDAO.RegistroLicencia registro = licenciaDAO.obtener();
        if (registro.claveActivacion() == null || registro.claveActivacion().isBlank()) {
            return false;
        }
        // Compruebo la firma guardada contra mi clave pública; ya no hay ningún secreto con qué comparar
        return validador.esValida(registro.idInstalacion(), registro.claveActivacion());
    }

    public int diasRestantesPrueba() {
        LicenciaDAO.RegistroLicencia registro = licenciaDAO.obtener();
        long transcurridos = ChronoUnit.DAYS.between(registro.fechaInstalacion(), registro.ultimaFechaVista());
        return (int) Math.max(0, DIAS_PRUEBA - transcurridos);
    }

    public boolean estaBloqueado() {
        return !estaActivada() && diasRestantesPrueba() <= 0;
    }

    public void activar(String claveIngresada) {
        if (claveIngresada == null || claveIngresada.isBlank()) {
            throw new IllegalArgumentException("Ingresa una clave de activación");
        }
        // Acepto la clave con espacios, saltos de línea, guiones o minúsculas, como suele llegar al pegarla
        String normalizada = ValidadorLicencia.normalizarClave(claveIngresada);
        if (normalizada == null || !validador.esValida(idInstalacion(), normalizada)) {
            throw new IllegalArgumentException("La clave de activación no es válida para este equipo");
        }
        licenciaDAO.guardarClaveActivacion(normalizada);
    }
}
