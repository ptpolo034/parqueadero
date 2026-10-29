package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.parqueadero.util.IconoApp;
import com.parqueadero.util.RecursosApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/** Aquí armo el panel lateral con degradado y el logo, para las pantallas de inicio de sesión y activación. */
public class PanelMarca extends JPanel {

    public PanelMarca(String titulo, String subtitulo) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 36, 40, 36));

        add(Box.createVerticalGlue());

        Image imagen = null;
        ImageIcon logo = RecursosApp.cargarLogo();
        if (logo != null) {
            imagen = logo.getImage();
        } else {
            imagen = IconoApp.obtener();
        }
        if (imagen != null) {
            int lado = UIScale.scale(84);
            JLabel etiquetaLogo = new JLabel(new ImageIcon(imagen.getScaledInstance(lado, lado, Image.SCALE_SMOOTH)));
            etiquetaLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(etiquetaLogo);
            add(Box.createVerticalStrut(24));
        }

        JLabel t = new JLabel("<html><body style='width:200px'>" + titulo + "</body></html>");
        t.setForeground(Color.WHITE);
        t.putClientProperty(FlatClientProperties.STYLE, "font:bold +12");
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(t);
        add(Box.createVerticalStrut(10));

        JLabel s = new JLabel("<html><body style='width:200px'>" + subtitulo + "</body></html>");
        s.setForeground(new Color(255, 255, 255, 215));
        s.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(s);

        add(Box.createVerticalGlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, new Color(0x1D4ED8), w, h, new Color(0x0F172A)));
        g2.fillRect(0, 0, w, h);
        // Pinto unos círculos decorativos muy sutiles
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fill(new Ellipse2D.Float(w * 0.45f, -h * 0.12f, w * 0.9f, w * 0.9f));
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fill(new Ellipse2D.Float(-w * 0.35f, h * 0.68f, w * 0.9f, w * 0.9f));
        g2.dispose();
        super.paintComponent(g);
    }
}
