package com.parqueadero.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;

/** Aquí reúno los íconos vectoriales (SVG) de la interfaz: nunca se ven pixelados y se adaptan al tema claro/oscuro. */
public final class Iconos {
    private Iconos() {
    }

    /** Devuelvo un ícono gris neutro que cambia de tono solo entre el tema claro y el oscuro. */
    public static FlatSVGIcon neutro(String nombre, int tamano) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter()
                .add(Color.BLACK, new Color(0x64748B), new Color(0x94A3B8)));
        return icono;
    }

    /** Devuelvo un ícono de un color fijo (por ejemplo, blanco sobre un botón de color). */
    public static FlatSVGIcon color(String nombre, int tamano, Color color) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter().add(Color.BLACK, color, color));
        return icono;
    }
}
