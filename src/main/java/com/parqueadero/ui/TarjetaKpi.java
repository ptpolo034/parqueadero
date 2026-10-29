package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Aquí armo la tarjeta de indicador: ícono, título, cifra grande, texto de apoyo y, opcionalmente, una barra de avance.
 * Cuando la tarjeta es angosta (ventana pequeña) escondo el ícono para que el título, la cifra y el apoyo
 * se lean completos, y si aun así la cifra no cabe le achico la letra en vez de cortarla con "...".
 */
public class TarjetaKpi extends Tarjeta {
    /** Ancho (en píxeles sin escalar) por debajo del cual escondo el ícono circular. */
    private static final int ANCHO_MINIMO_CON_ICONO = 225;

    private final JLabel iconoLabel = new JLabel();
    private final JLabel titulo = new JLabel();
    private final JLabel valor = new CifraAjustable();
    private final JLabel apoyo = new JLabel();
    private final Barra barra = new Barra();

    public TarjetaKpi(String tituloTexto, String icono, Estilos.Tono tono, boolean conBarra) {
        super(new BorderLayout(14, 0));

        iconoLabel.setIcon(Estilos.iconoCircular(icono, tono, 44));
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

    /** Escondo o muestro el ícono según el ancho que tenga la tarjeta en este momento. */
    @Override
    public void doLayout() {
        boolean cabe = getWidth() >= UIScale.scale(ANCHO_MINIMO_CON_ICONO);
        if (iconoLabel.isVisible() != cabe) {
            iconoLabel.setVisible(cabe);
        }
        super.doLayout();
    }

    public void actualizar(String valorTexto, String apoyoTexto) {
        valor.setText(valorTexto);
        apoyo.setText(apoyoTexto);
        titulo.setForeground(Tema.textoSecundario());
        apoyo.setForeground(Tema.textoSecundario());
    }

    /** Actualizo la barra de avance (de 0 a 1) con el tono indicado. */
    public void actualizarBarra(double proporcion, Estilos.Tono tono) {
        barra.proporcion = Math.max(0, Math.min(1, proporcion));
        barra.tono = tono;
        barra.repaint();
    }

    /** La cifra grande: si no cabe en el ancho disponible, la dibujo con una letra más pequeña. */
    private static class CifraAjustable extends JLabel {
        private static final float TAMANO_MINIMO = 13f;

        @Override
        public Dimension getMinimumSize() {
            // Dejo que el diseño la angoste; el texto se ajusta solo al pintar.
            return new Dimension(0, super.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            String texto = getText();
            if (texto == null || texto.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font fuente = getFont();
            FontMetrics fm = g2.getFontMetrics(fuente);
            float tamano = fuente.getSize2D();
            while (fm.stringWidth(texto) > getWidth() && tamano > TAMANO_MINIMO) {
                tamano -= 1f;
                fuente = fuente.deriveFont(tamano);
                fm = g2.getFontMetrics(fuente);
            }
            g2.setFont(fuente);
            g2.setColor(getForeground());
            g2.drawString(texto, 0, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static class Barra extends JComponent {
        double proporcion;
        Estilos.Tono tono = Estilos.Tono.ACENTO;

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, UIScale.scale(8));
        }

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
