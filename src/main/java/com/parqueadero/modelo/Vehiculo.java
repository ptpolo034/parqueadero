package com.parqueadero.modelo;

import java.time.LocalDateTime;

public class Vehiculo {
    private int id;
    private final String placa;
    private final TipoVehiculo tipo;
    private final LocalDateTime horaEntrada;
    private final LocalDateTime horaSalida;
    private final Integer totalParqueo;
    private final EstadoVehiculo estado;
    private final String usuarioEntrada;
    private final String usuarioSalida;
    private final Integer montoPagado;
    private final Integer vuelto;
    private final boolean esMensual;

    public Vehiculo(String placa, TipoVehiculo tipo, LocalDateTime horaEntrada, String usuarioEntrada, boolean esMensual) {
        this.placa = placa;
        this.tipo = tipo;
        this.horaEntrada = horaEntrada;
        this.estado = EstadoVehiculo.ACTIVO;
        this.usuarioEntrada = usuarioEntrada;
        this.esMensual = esMensual;
        this.horaSalida = null;
        this.totalParqueo = null;
        this.usuarioSalida = null;
        this.montoPagado = null;
        this.vuelto = null;
    }

    public Vehiculo(int id, String placa, TipoVehiculo tipo, LocalDateTime horaEntrada,
                    LocalDateTime horaSalida, Integer totalParqueo, EstadoVehiculo estado,
                    String usuarioEntrada, String usuarioSalida, Integer montoPagado, Integer vuelto,
                    boolean esMensual) {
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.totalParqueo = totalParqueo;
        this.estado = estado;
        this.usuarioEntrada = usuarioEntrada;
        this.usuarioSalida = usuarioSalida;
        this.montoPagado = montoPagado;
        this.vuelto = vuelto;
        this.esMensual = esMensual;
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

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public LocalDateTime getHoraEntrada() {
        return horaEntrada;
    }

    public LocalDateTime getHoraSalida() {
        return horaSalida;
    }

    public Integer getTotalParqueo() {
        return totalParqueo;
    }

    public EstadoVehiculo getEstado() {
        return estado;
    }

    public String getUsuarioEntrada() {
        return usuarioEntrada;
    }

    public String getUsuarioSalida() {
        return usuarioSalida;
    }

    public Integer getMontoPagado() {
        return montoPagado;
    }

    public Integer getVuelto() {
        return vuelto;
    }

    public boolean isEsMensual() {
        return esMensual;
    }
}
