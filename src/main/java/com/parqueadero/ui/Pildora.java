package com.parqueadero.ui;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Etiqueta en forma de píldora con un tono suave de fondo (estado, contador, aviso...). */
public class Pildora extends JLabel {
    private Estilos.Tono tono;

    public Pildora(String texto, Estilos.Tono tono) {
        super(texto);
        this.tono = tono;
        setOpaque(false);
        setBorder(new EmptyBorder(UIScale.scale(5), UIScale.scale(12), UIScale.scale(5), UIScale.scale(12)));
        putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "font:semibold");
    }

    public void establecer(String texto, Estilos.Tono tono) {
        this.tono = tono;
        setText(texto);
        repaint();
    }

    @Override
    public Color getForeground() {
        return tono == null ? super.getForeground() : tono.color();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, UIScale.scale(28)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = tono.color();
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 32));
        float arco = Math.min(getHeight(), UIScale.scale(28f));
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arco, arco));
        g2.dispose();
        super.paintComponent(g);
    }
}
