package com.parqueadero.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Aquí manejo la apariencia de toda la aplicación: tipografía Inter, colores y modo claro/oscuro. */
public final class Tema {
    private static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".parqueadero", "tema.txt");

    private static boolean oscuro;

    private Tema() {
    }

    /** Lo llamo una sola vez al arrancar, antes de crear cualquier ventana. */
    public static void instalar() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        FlatLaf.registerCustomDefaultsSource("com.parqueadero.tema");
        UIManager.put("defaultFont", new FontUIResource(FlatInterFont.FAMILY, Font.PLAIN, 13));
        // Dejo los botones de los avisos y preguntas siempre en español, sin depender del idioma de Windows o de Java.
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.messageDialogTitle", "Aviso");
        UIManager.put("OptionPane.inputDialogTitle", "Escribe un dato");
        UIManager.put("OptionPane.titleText", "Seleccione una opción");
        oscuro = leerPreferencia();
        aplicar();
    }

    public static boolean esOscuro() {
        return oscuro;
    }

    /** Cambio entre claro y oscuro, refresco las ventanas abiertas y guardo la elección. */
    public static void alternar() {
        oscuro = !oscuro;
        guardarPreferencia();
        aplicar();
        FlatLaf.updateUI();
    }

    private static void aplicar() {
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    public static Color acento() {
        Color c = UIManager.getColor("Component.accentColor");
        return c != null ? c : new Color(0x2563EB);
    }

    /** Devuelvo el fondo de las tarjetas (igual al de los campos de texto, que contrasta con el fondo de la página). */
    public static Color fondoTarjeta() {
        Color c = UIManager.getColor("TextField.background");
        return c != null ? c : Color.WHITE;
    }

    public static Color borde() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : new Color(0xDDE3EC);
    }

    public static Color textoSecundario() {
        return oscuro ? new Color(0x94A3B8) : new Color(0x64748B);
    }

    public static Color verde() {
        return oscuro ? new Color(0x34D399) : new Color(0x059669);
    }

    public static Color ambar() {
        return oscuro ? new Color(0xFBBF24) : new Color(0xD97706);
    }

    public static Color rojo() {
        return oscuro ? new Color(0xF87171) : new Color(0xDC2626);
    }

    private static boolean leerPreferencia() {
        try {
            return Files.exists(ARCHIVO) && Files.readString(ARCHIVO).trim().equals("oscuro");
        } catch (IOException e) {
            return false;
        }
    }

    private static void guardarPreferencia() {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Files.writeString(ARCHIVO, oscuro ? "oscuro" : "claro");
        } catch (IOException e) {
            // No es crítico: si falla, simplemente no se guarda la elección.
        }
    }
}
