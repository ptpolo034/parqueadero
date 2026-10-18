package com.parqueadero.ui;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Panel con esquinas redondeadas, fondo de "tarjeta" y un borde muy fino. */
public class Tarjeta extends JPanel {

    public Tarjeta(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(UIScale.scale(16), UIScale.scale(18), UIScale.scale(16), UIScale.scale(18)));
    }

    /** Tarjeta con un título (y, si se da, un subtítulo) encima del contenido. */
    public static Tarjeta conTitulo(String titulo, String subtitulo, java.awt.Component cuerpo) {
        Tarjeta t = new Tarjeta(new java.awt.BorderLayout(0, 12));
        JPanel cab = new JPanel(new java.awt.BorderLayout());
        cab.setOpaque(false);
        javax.swing.JLabel l = new javax.swing.JLabel(titulo);
        l.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "font:bold +1");
        cab.add(l, java.awt.BorderLayout.NORTH);
        if (subtitulo != null && !subtitulo.isEmpty()) {
            cab.add(Estilos.subtitulo(subtitulo), java.awt.BorderLayout.SOUTH);
        }
        t.add(cab, java.awt.BorderLayout.NORTH);
        t.add(cuerpo, java.awt.BorderLayout.CENTER);
        return t;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float arco = UIScale.scale(16f);
        g2.setColor(Tema.fondoTarjeta());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arco, arco));
        g2.setColor(Tema.borde());
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arco, arco));
        g2.dispose();
        super.paintComponent(g);
    }
}
