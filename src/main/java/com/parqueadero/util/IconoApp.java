package com.parqueadero.util;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;

/** Ícono por defecto del software, empaquetado dentro del .jar. El cliente puede reemplazarlo
 * subiendo su propio logo en Configuración (ver {@link RecursosApp}), que tiene prioridad. */
public final class IconoApp {
    private static Image icono;

    private IconoApp() {
    }

    public static Image obtener() {
        if (icono == null) {
            URL recurso = IconoApp.class.getResource("/icono.png");
            if (recurso != null) {
                icono = new ImageIcon(recurso).getImage();
            }
        }
        return icono;
    }
}
