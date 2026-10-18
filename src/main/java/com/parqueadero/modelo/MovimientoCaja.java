package com.parqueadero.modelo;

import java.time.LocalDateTime;

public class MovimientoCaja {
    private int id;
    private final TipoMovimientoCaja tipo;
    private final String concepto;
    private final int monto;
    private final LocalDateTime fecha;
    private final String usuario;

    public MovimientoCaja(TipoMovimientoCaja tipo, String concepto, int monto, LocalDateTime fecha, String usuario) {
        this.tipo = tipo;
        this.concepto = concepto;
        this.monto = monto;
        this.fecha = fecha;
        this.usuario = usuario;
    }

    public MovimientoCaja(int id, TipoMovimientoCaja tipo, String concepto, int monto, LocalDateTime fecha, String usuario) {
        this.id = id;
        this.tipo = tipo;
        this.concepto = concepto;
        this.monto = monto;
        this.fecha = fecha;
        this.usuario = usuario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public int getMonto() {
        return monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }
}
