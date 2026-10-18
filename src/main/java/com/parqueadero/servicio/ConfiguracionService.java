package com.parqueadero.servicio;

import com.parqueadero.dao.ConfiguracionDAO;
import com.parqueadero.modelo.ConfiguracionNegocio;

public class ConfiguracionService {

    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

    public ConfiguracionNegocio obtener() {
        return configuracionDAO.obtener();
    }

    public void actualizar(ConfiguracionNegocio configuracion) {
        if (configuracion.getNombre() == null || configuracion.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del negocio es obligatorio");
        }
        if (configuracion.getCuposTotales() < 1) {
            throw new IllegalArgumentException("Los cupos totales deben ser al menos 1");
        }
        configuracionDAO.actualizar(configuracion);
    }

    public void actualizarReglasCobro(int minutosGracia, int minutosFraccion) {
        if (minutosGracia < 0 || minutosGracia > 240) {
            throw new IllegalArgumentException("Los minutos de gracia deben estar entre 0 y 240");
        }
        if (minutosFraccion < 5 || minutosFraccion > 60) {
            throw new IllegalArgumentException("La fracción de cobro debe estar entre 5 y 60 minutos");
        }
        configuracionDAO.actualizarReglasCobro(minutosGracia, minutosFraccion);
    }

    public int obtenerCuposTotales() {
        return obtener().getCuposTotales();
    }
}
