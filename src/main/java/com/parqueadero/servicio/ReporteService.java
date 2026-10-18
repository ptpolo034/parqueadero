package com.parqueadero.servicio;

import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.Vehiculo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteService {

    private final MensualidadService mensualidadService = new MensualidadService(null);
    private final ParqueoService parqueoService = new ParqueoService(null, new ConfiguracionService(), mensualidadService);
    private final LavadoService lavadoService = new LavadoService(null);

    public int ingresosParqueoEnRango(LocalDate desde, LocalDate hasta) {
        return parqueoService.ingresosEnRango(desde, hasta);
    }

    public int ingresosLavadoEnRango(LocalDate desde, LocalDate hasta) {
        return lavadoService.ingresosEnRango(desde, hasta);
    }

    public int ingresosMensualidadesEnRango(LocalDate desde, LocalDate hasta) {
        return mensualidadService.ingresosEnRango(desde, hasta);
    }

    public int ingresosTotalesEnRango(LocalDate desde, LocalDate hasta) {
        return ingresosParqueoEnRango(desde, hasta) + ingresosLavadoEnRango(desde, hasta)
                + ingresosMensualidadesEnRango(desde, hasta);
    }

    public void exportarCSV(Path destino, LocalDate desde, LocalDate hasta) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("tipo,placa,detalle,inicio,fin,total");

        for (Vehiculo v : parqueoService.obtenerHistorialEnRango(desde, hasta)) {
            lineas.add(filaCSV("parqueo", v.getPlaca(), v.getTipo().name(),
                    v.getHoraEntrada().toString(), v.getHoraSalida().toString(), String.valueOf(v.getTotalParqueo())));
        }

        for (RegistroLavado r : lavadoService.obtenerHistorialEnRango(desde, hasta)) {
            lineas.add(filaCSV("lavado", r.getPlaca(), r.getServicio().getNombre(),
                    r.getFechaInicio().toString(), r.getFechaFin().toString(), String.valueOf(r.getPrecio())));
        }

        Files.write(destino, lineas);
    }

    private String filaCSV(String... campos) {
        StringBuilder fila = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                fila.append(",");
            }
            fila.append(escaparCSV(campos[i]));
        }
        return fila.toString();
    }

    private String escaparCSV(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
