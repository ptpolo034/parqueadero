package com.parqueadero.servicio;

import com.parqueadero.dao.LicenciaDAO;
import com.parqueadero.util.GeneradorClaves;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LicenciaService {

    public static final int DIAS_PRUEBA = 30;

    private final LicenciaDAO licenciaDAO = new LicenciaDAO();

    /**
     * Se llama una vez al arrancar la aplicación: avanza la "última fecha vista" hasta
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
        return registro.claveActivacion().equals(GeneradorClaves.calcularClave(registro.idInstalacion()));
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
        String normalizada = claveIngresada.trim().toUpperCase().replace(" ", "");
        String esperada = GeneradorClaves.calcularClave(idInstalacion());
        if (!normalizada.equals(esperada)) {
            throw new IllegalArgumentException("La clave de activación no es válida para este equipo");
        }
        licenciaDAO.guardarClaveActivacion(normalizada);
    }
}
