package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Tarjeta de indicador: ícono, título, cifra grande, texto de apoyo y, opcionalmente, una barra de avance. */
public class TarjetaKpi extends Tarjeta {
    private final JLabel titulo = new JLabel();
    private final JLabel valor = new JLabel();
    private final JLabel apoyo = new JLabel();
    private final Barra barra = new Barra();

    public TarjetaKpi(String tituloTexto, String icono, Estilos.Tono tono, boolean conBarra) {
        super(new BorderLayout(14, 0));

        JLabel iconoLabel = new JLabel(Estilos.iconoCircular(icono, tono, 44));
        iconoLabel.setVerticalAlignment(JLabel.TOP);
        add(iconoLabel, BorderLayout.WEST);

        titulo.setText(tituloTexto);
        titulo.setForeground(Tema.textoSecundario());
        valor.putClientProperty(FlatClientProperties.STYLE, "font:bold +9");
        apoyo.putClientProperty(FlatClientProperties.STYLE, "font:-1");
        apoyo.setForeground(Tema.textoSecundario());

        JPanel textos = new JPanel(new GridBagLayout());
        textos.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        textos.add(titulo, gbc);
        gbc.insets = new Insets(2, 0, 2, 0);
        textos.add(valor, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        textos.add(apoyo, gbc);
        if (conBarra) {
            gbc.insets = new Insets(8, 0, 0, 0);
            textos.add(barra, gbc);
        }
        add(textos, BorderLayout.CENTER);
    }

    public void actualizar(String valorTexto, String apoyoTexto) {
        valor.setText(valorTexto);
        apoyo.setText(apoyoTexto);
        titulo.setForeground(Tema.textoSecundario());
        apoyo.setForeground(Tema.textoSecundario());
    }

    /** Barra de avance (0..1) con el tono indicado. */
    public void actualizarBarra(double proporcion, Estilos.Tono tono) {
        barra.proporcion = Math.max(0, Math.min(1, proporcion));
        barra.tono = tono;
        barra.repaint();
    }

    private static class Barra extends JComponent {
        double proporcion;
        Estilos.Tono tono = Estilos.Tono.ACENTO;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(UIScale.scale(120), UIScale.scale(8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float h = UIScale.scale(8f);
            float y = (getHeight() - h) / 2f;
            g2.setColor(Tema.borde());
            g2.fill(new RoundRectangle2D.Float(0, y, getWidth(), h, h, h));
            g2.setColor(tono.color());
            g2.fill(new RoundRectangle2D.Float(0, y, (float) (getWidth() * proporcion), h, h, h));
            g2.dispose();
        }
    }
}
