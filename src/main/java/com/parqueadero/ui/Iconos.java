package com.parqueadero.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;

/** Íconos vectoriales (SVG) de la interfaz. Nunca se ven pixelados y se adaptan al tema claro/oscuro. */
public final class Iconos {
    private Iconos() {
    }

    /** Ícono gris neutro que cambia de tono automáticamente entre tema claro y oscuro. */
    public static FlatSVGIcon neutro(String nombre, int tamano) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter()
                .add(Color.BLACK, new Color(0x64748B), new Color(0x94A3B8)));
        return icono;
    }

    /** Ícono de un color fijo (por ejemplo, blanco sobre un botón de color). */
    public static FlatSVGIcon color(String nombre, int tamano, Color color) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter().add(Color.BLACK, color, color));
        return icono;
    }
}
