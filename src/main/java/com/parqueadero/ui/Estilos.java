package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/** Pequeñas ayudas para dar a los componentes el estilo visual de la aplicación. */
public final class Estilos {
    private Estilos() {
    }

    /** Colores semánticos que se resuelven en el momento de pintar (así respetan el tema claro/oscuro). */
    public enum Tono {
        ACENTO, VERDE, AMBAR, ROJO, NEUTRO;

        public Color color() {
            return switch (this) {
                case ACENTO -> Tema.acento();
                case VERDE -> Tema.verde();
                case AMBAR -> Tema.ambar();
                case ROJO -> Tema.rojo();
                case NEUTRO -> Tema.textoSecundario();
            };
        }
    }

    public static JButton primario(JButton boton) {
        return primario(boton, "font:semibold");
    }

    /** Botón de acción principal, más alto y con letra más marcada (formularios grandes, inicio de sesión). */
    public static JButton primarioGrande(JButton boton) {
        return primario(boton, "font:bold +1; margin:11,20,11,20");
    }

    private static JButton primario(JButton boton, String extra) {
        boton.putClientProperty(FlatClientProperties.STYLE,
                "background:$Component.accentColor; foreground:#ffffff; borderWidth:0; focusWidth:0;"
                        + "hoverBackground:lighten($Component.accentColor,6%);"
                        + "pressedBackground:darken($Component.accentColor,6%);"
                        + "default.background:$Component.accentColor; default.foreground:#ffffff;"
                        + "default.borderColor:$Component.accentColor; default.focusedBorderColor:$Component.accentColor;"
                        + "default.hoverBackground:lighten($Component.accentColor,6%);"
                        + "default.pressedBackground:darken($Component.accentColor,6%);"
                        + extra);
        return boton;
    }

    public static JButton peligro(JButton boton) {
        boton.putClientProperty(FlatClientProperties.STYLE,
                "background:#DC2626; foreground:#ffffff; borderWidth:0; focusWidth:0;"
                        + "hoverBackground:#EF4444; pressedBackground:#B91C1C; font:semibold");
        return boton;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.textoSecundario());
        return l;
    }

    /** Etiqueta pequeña en mayúsculas para encabezar secciones. */
    public static JLabel seccion(String texto) {
        JLabel l = new JLabel(texto.toUpperCase());
        l.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Label.disabledForeground");
        return l;
    }

    public static void fuente(JComponent c, int estilo, float tamano) {
        Font base = c.getFont();
        c.setFont(base.deriveFont(estilo, tamano));
    }

    /** Círculo con un ícono dentro, con un tono suave de fondo. */
    public static Icon iconoCircular(String nombre, Tono tono, int diametro) {
        return new Icon() {
            private final FlatSVGIcon svg = svgConTono(nombre, tono, diametro / 2);

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = UIScale.scale(diametro);
                Color t = tono.color();
                g2.setColor(new Color(t.getRed(), t.getGreen(), t.getBlue(), 36));
                g2.fill(new Ellipse2D.Float(x, y, d, d));
                g2.dispose();
                int s = UIScale.scale(diametro / 2);
                svg.paintIcon(c, g, x + (d - s) / 2, y + (d - s) / 2);
            }

            @Override
            public int getIconWidth() {
                return UIScale.scale(diametro);
            }

            @Override
            public int getIconHeight() {
                return UIScale.scale(diametro);
            }
        };
    }

    private static FlatSVGIcon svgConTono(String nombre, Tono tono, int tamano) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter(c -> tono.color()));
        return icono;
    }

    /** Avatar redondo con la inicial del usuario. */
    public static Icon avatar(String nombre, int diametro) {
        String inicial = (nombre == null || nombre.isBlank()) ? "?" : nombre.substring(0, 1).toUpperCase();
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int d = UIScale.scale(diametro);
                g2.setColor(Tema.acento());
                g2.fill(new Ellipse2D.Float(x, y, d, d));
                g2.setColor(Color.WHITE);
                g2.setFont(c.getFont().deriveFont(Font.BOLD, d * 0.46f));
                var fm = g2.getFontMetrics();
                g2.drawString(inicial, x + (d - fm.stringWidth(inicial)) / 2f,
                        y + (d - fm.getHeight()) / 2f + fm.getAscent());
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return UIScale.scale(diametro);
            }

            @Override
            public int getIconHeight() {
                return UIScale.scale(diametro);
            }
        };
    }
}
