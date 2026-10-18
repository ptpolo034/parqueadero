package com.parqueadero.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CierreCaja {
    private int id;
    private final LocalDate fecha;
    private final String usuario;
    private final int baseInicial;
    private final int ingresosParqueo;
    private final int ingresosLavado;
    private final int ingresosMensualidades;
    private final int ingresosManuales;
    private final int egresosManuales;
    private final int totalEsperado;
    private final int montoContado;
    private final int diferencia;
    private final LocalDateTime fechaHoraCierre;

    public CierreCaja(LocalDate fecha, String usuario, int baseInicial, int ingresosParqueo, int ingresosLavado,
                      int ingresosMensualidades, int ingresosManuales, int egresosManuales, int totalEsperado,
                      int montoContado, int diferencia, LocalDateTime fechaHoraCierre) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.baseInicial = baseInicial;
        this.ingresosParqueo = ingresosParqueo;
        this.ingresosLavado = ingresosLavado;
        this.ingresosMensualidades = ingresosMensualidades;
        this.ingresosManuales = ingresosManuales;
        this.egresosManuales = egresosManuales;
        this.totalEsperado = totalEsperado;
        this.montoContado = montoContado;
        this.diferencia = diferencia;
        this.fechaHoraCierre = fechaHoraCierre;
    }

    public CierreCaja(int id, LocalDate fecha, String usuario, int baseInicial, int ingresosParqueo, int ingresosLavado,
                      int ingresosMensualidades, int ingresosManuales, int egresosManuales, int totalEsperado,
                      int montoContado, int diferencia, LocalDateTime fechaHoraCierre) {
        this(fecha, usuario, baseInicial, ingresosParqueo, ingresosLavado, ingresosMensualidades, ingresosManuales,
                egresosManuales, totalEsperado, montoContado, diferencia, fechaHoraCierre);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public int getBaseInicial() {
        return baseInicial;
    }

    public int getIngresosParqueo() {
        return ingresosParqueo;
    }

    public int getIngresosLavado() {
        return ingresosLavado;
    }

    public int getIngresosMensualidades() {
        return ingresosMensualidades;
    }

    public int getIngresosManuales() {
        return ingresosManuales;
    }

    public int getEgresosManuales() {
        return egresosManuales;
    }

    public int getTotalEsperado() {
        return totalEsperado;
    }

    public int getMontoContado() {
        return montoContado;
    }

    public int getDiferencia() {
        return diferencia;
    }

    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }
}
