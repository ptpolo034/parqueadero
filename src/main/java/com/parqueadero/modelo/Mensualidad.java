package com.parqueadero.modelo;

import java.time.LocalDate;

public class Mensualidad {
    private int id;
    private final String placa;
    private final String nombreCliente;
    private final String telefono;
    private final TipoVehiculo tipoVehiculo;
    private final int montoMensual;
    private final LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private EstadoMensualidad estado;
    private final String usuarioRegistro;

    public Mensualidad(String placa, String nombreCliente, String telefono, TipoVehiculo tipoVehiculo,
                       int montoMensual, LocalDate fechaInicio, LocalDate fechaVencimiento, String usuarioRegistro) {
        this.placa = placa;
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
        this.tipoVehiculo = tipoVehiculo;
        this.montoMensual = montoMensual;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = EstadoMensualidad.ACTIVA;
        this.usuarioRegistro = usuarioRegistro;
    }

    public Mensualidad(int id, String placa, String nombreCliente, String telefono, TipoVehiculo tipoVehiculo,
                       int montoMensual, LocalDate fechaInicio, LocalDate fechaVencimiento,
                       EstadoMensualidad estado, String usuarioRegistro) {
        this.id = id;
        this.placa = placa;
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
        this.tipoVehiculo = tipoVehiculo;
        this.montoMensual = montoMensual;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.usuarioRegistro = usuarioRegistro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public TipoVehiculo getTipoVehiculo() {
        return tipoVehiculo;
    }

    public int getMontoMensual() {
        return montoMensual;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public EstadoMensualidad getEstado() {
        return estado;
    }

    public void setEstado(EstadoMensualidad estado) {
        this.estado = estado;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public boolean estaVigente(LocalDate fecha) {
        return estado == EstadoMensualidad.ACTIVA && !fecha.isAfter(fechaVencimiento);
    }
}
