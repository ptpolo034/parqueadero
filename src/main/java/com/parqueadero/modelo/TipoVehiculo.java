package com.parqueadero.modelo;

import java.util.Locale;
import java.util.Objects;

/**
 * Aquí represento el tipo de vehículo (Carro, Moto, Camioneta...). Los tipos disponibles los define el
 * administrador en Tarifas, así que ya no es una lista fija: lo que identifica a un tipo es su nombre en mayúsculas.
 */
public final class TipoVehiculo {
    public static final TipoVehiculo CARRO = new TipoVehiculo("CARRO");
    public static final TipoVehiculo MOTO = new TipoVehiculo("MOTO");
    public static final TipoVehiculo BICICLETA = new TipoVehiculo("BICICLETA");

    private final String nombre;

    private TipoVehiculo(String nombre) {
        this.nombre = nombre;
    }

    public static TipoVehiculo de(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del tipo de vehículo es obligatorio");
        }
        return new TipoVehiculo(nombre.trim().toUpperCase(Locale.ROOT));
    }

    public String name() {
        return nombre;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof TipoVehiculo t && t.nombre.equals(nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
