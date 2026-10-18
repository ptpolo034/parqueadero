package com.parqueadero.util;

import java.net.InetAddress;

public final class NombreEquipo {

    private NombreEquipo() {
    }

    public static String obtener() {
        String nombre = System.getenv("COMPUTERNAME");
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "PC";
        }
    }
}
