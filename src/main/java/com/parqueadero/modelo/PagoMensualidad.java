package com.parqueadero.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PagoMensualidad {
    private int id;
    private int mensualidadId;
    private final String placa;
    private final int monto;
    private final LocalDateTime fechaPago;
    private final LocalDate fechaVencimientoNueva;
    private final String usuario;

    public PagoMensualidad(int mensualidadId, String placa, int monto, LocalDateTime fechaPago,
                           LocalDate fechaVencimientoNueva, String usuario) {
        this.mensualidadId = mensualidadId;
        this.placa = placa;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.fechaVencimientoNueva = fechaVencimientoNueva;
        this.usuario = usuario;
    }

    public PagoMensualidad(int id, int mensualidadId, String placa, int monto, LocalDateTime fechaPago,
                           LocalDate fechaVencimientoNueva, String usuario) {
        this(mensualidadId, placa, monto, fechaPago, fechaVencimientoNueva, usuario);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMensualidadId(int mensualidadId) {
        this.mensualidadId = mensualidadId;
    }

    public int getMensualidadId() {
        return mensualidadId;
    }

    public String getPlaca() {
        return placa;
    }

    public int getMonto() {
        return monto;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public LocalDate getFechaVencimientoNueva() {
        return fechaVencimientoNueva;
    }

    public String getUsuario() {
        return usuario;
    }
}
