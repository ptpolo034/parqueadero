package com.parqueadero.modelo;

import java.time.LocalDateTime;

public class RegistroLavado {
    private int id;
    private final String placa;
    private final ServicioLavado servicio;
    private final Integer vehiculoId;
    private final int precio;
    private final LocalDateTime fechaInicio;
    private final LocalDateTime fechaFin;
    private final EstadoLavado estado;
    private final String nombreDueno;
    private final String telefonoDueno;
    private final String usuarioInicio;
    private final String usuarioFin;
    private final Integer montoPagado;
    private final Integer vuelto;

    public RegistroLavado(String placa, ServicioLavado servicio, Integer vehiculoId, LocalDateTime fechaInicio,
                          String nombreDueno, String telefonoDueno, String usuarioInicio) {
        this.placa = placa;
        this.servicio = servicio;
        this.vehiculoId = vehiculoId;
        this.precio = servicio.getPrecio();
        this.fechaInicio = fechaInicio;
        this.estado = EstadoLavado.PENDIENTE;
        this.nombreDueno = nombreDueno;
        this.telefonoDueno = telefonoDueno;
        this.usuarioInicio = usuarioInicio;
        this.fechaFin = null;
        this.usuarioFin = null;
        this.montoPagado = null;
        this.vuelto = null;
    }

    public RegistroLavado(int id, String placa, ServicioLavado servicio, Integer vehiculoId, int precio,
                          LocalDateTime fechaInicio, LocalDateTime fechaFin, EstadoLavado estado,
                          String nombreDueno, String telefonoDueno, String usuarioInicio, String usuarioFin,
                          Integer montoPagado, Integer vuelto) {
        this.id = id;
        this.placa = placa;
        this.servicio = servicio;
        this.vehiculoId = vehiculoId;
        this.precio = precio;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.nombreDueno = nombreDueno;
        this.telefonoDueno = telefonoDueno;
        this.usuarioInicio = usuarioInicio;
        this.usuarioFin = usuarioFin;
        this.montoPagado = montoPagado;
        this.vuelto = vuelto;
    }

    public int getId() {
        return id;
    }

    /** Solo se usa justo después de insertar, para completar el id autogenerado por la base de datos. */
    public void setId(int id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public ServicioLavado getServicio() {
        return servicio;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public int getPrecio() {
        return precio;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public EstadoLavado getEstado() {
        return estado;
    }

    public String getNombreDueno() {
        return nombreDueno;
    }

    public String getTelefonoDueno() {
        return telefonoDueno;
    }

    public String getUsuarioInicio() {
        return usuarioInicio;
    }

    public String getUsuarioFin() {
        return usuarioFin;
    }

    public Integer getMontoPagado() {
        return montoPagado;
    }

    public Integer getVuelto() {
        return vuelto;
    }
}
