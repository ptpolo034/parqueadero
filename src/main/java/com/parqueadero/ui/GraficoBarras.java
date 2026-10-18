package com.parqueadero.ui;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras simple y limpio, dibujado a mano: sin ejes pesados, con valores sobre cada barra. */
public class GraficoBarras extends JComponent {
    private final List<String> etiquetas = new ArrayList<>();
    private final List<Integer> valores = new ArrayList<>();

    public void establecerDatos(List<String> etiquetas, List<Integer> valores) {
        this.etiquetas.clear();
        this.etiquetas.addAll(etiquetas);
        this.valores.clear();
        this.valores.addAll(valores);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(UIScale.scale(420), UIScale.scale(220));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (valores.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font base = getFont();
        Font fuenteEtiqueta = base.deriveFont(Font.PLAIN, base.getSize2D() - 1f);
        Font fuenteValor = base.deriveFont(Font.BOLD, base.getSize2D() - 1f);
        FontMetrics fmEtiqueta = g2.getFontMetrics(fuenteEtiqueta);

        int margenInferior = fmEtiqueta.getHeight() + UIScale.scale(8);
        int margenSuperior = UIScale.scale(26);
        int alto = getHeight() - margenInferior - margenSuperior;
        int n = valores.size();
        int maximo = Math.max(1, valores.stream().mapToInt(Integer::intValue).max().orElse(1));

        // Líneas guía muy sutiles
        g2.setColor(Tema.borde());
        for (int i = 0; i <= 3; i++) {
            int y = margenSuperior + alto * i / 3;
            g2.drawLine(0, y, getWidth(), y);
        }

        float ancho = (float) getWidth() / n;
        float anchoBarra = Math.min(ancho * 0.52f, UIScale.scale(46f));
        Color acento = Tema.acento();
        for (int i = 0; i < n; i++) {
            int valor = valores.get(i);
            float altoBarra = valor == 0 ? UIScale.scale(3f) : (float) valor / maximo * alto;
            float x = i * ancho + (ancho - anchoBarra) / 2f;
            float y = margenSuperior + alto - altoBarra;
            boolean ultima = i == n - 1;

            g2.setColor(ultima ? acento : new Color(acento.getRed(), acento.getGreen(), acento.getBlue(), 110));
            g2.fill(barraRedondeada(x, y, anchoBarra, altoBarra, UIScale.scale(8f)));

            g2.setFont(fuenteValor);
            g2.setColor(ultima ? acento : Tema.textoSecundario());
            String texto = compacto(valor);
            FontMetrics fmValor = g2.getFontMetrics();
            g2.drawString(texto, x + (anchoBarra - fmValor.stringWidth(texto)) / 2f, y - UIScale.scale(6f));

            g2.setFont(fuenteEtiqueta);
            g2.setColor(ultima ? getForeground() : Tema.textoSecundario());
            String etiqueta = etiquetas.get(i);
            g2.drawString(etiqueta, x + (anchoBarra - fmEtiqueta.stringWidth(etiqueta)) / 2f,
                    getHeight() - fmEtiqueta.getDescent() - UIScale.scale(2f));
        }
        g2.dispose();
    }

    /** Barra con las esquinas superiores redondeadas y la base recta. */
    private static Path2D barraRedondeada(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w / 2f, h));
        Path2D p = new Path2D.Float();
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }

    private static String compacto(int valor) {
        if (valor >= 1_000_000) {
            return String.format("$%.1fM", valor / 1_000_000.0);
        }
        if (valor >= 1000) {
            return "$" + Math.round(valor / 1000.0) + "k";
        }
        return "$" + valor;
    }
}
