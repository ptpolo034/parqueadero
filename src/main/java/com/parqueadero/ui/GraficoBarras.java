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

/**
 * Mi gráfico de barras simple y limpio, dibujado a mano: sin ejes pesados, con el valor sobre cada barra.
 * Si hay muchas barras y los textos no caben, muestro solo algunas etiquetas del eje (siempre la última)
 * y el valor de la barra más alta, en vez de dejar los textos encimados.
 */
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
        return new Dimension(UIScale.scale(420), UIScale.scale(150));
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
        FontMetrics fmValor = g2.getFontMetrics(fuenteValor);

        int margenInferior = fmEtiqueta.getHeight() + UIScale.scale(8);
        int margenSuperior = UIScale.scale(26);
        int alto = getHeight() - margenInferior - margenSuperior;
        int n = valores.size();
        int maximo = Math.max(1, valores.stream().mapToInt(Integer::intValue).max().orElse(1));
        boolean todoEnCero = valores.stream().allMatch(v -> v == 0);

        // Pinto unas líneas guía muy sutiles
        g2.setColor(Tema.borde());
        for (int i = 0; i <= 3; i++) {
            int y = margenSuperior + alto * i / 3;
            g2.drawLine(0, y, getWidth(), y);
        }

        float ancho = (float) getWidth() / n;
        float anchoBarra = Math.min(ancho * 0.52f, UIScale.scale(46f));
        Color acento = Tema.acento();

        // ¿Caben los textos? Si no, muestro menos etiquetas del eje y solo el valor de la barra más alta.
        int anchoEtiquetaMayor = 0;
        int anchoValorMayor = 0;
        for (int i = 0; i < n; i++) {
            anchoEtiquetaMayor = Math.max(anchoEtiquetaMayor, fmEtiqueta.stringWidth(etiquetas.get(i)));
            anchoValorMayor = Math.max(anchoValorMayor, fmValor.stringWidth(compacto(valores.get(i))));
        }
        int separacion = UIScale.scale(4);
        int saltoEtiquetas = Math.max(1, (int) Math.ceil((anchoEtiquetaMayor + separacion) / ancho));
        boolean valoresCaben = anchoValorMayor + UIScale.scale(4) <= ancho;
        int indiceMaximo = valores.indexOf(maximo);

        for (int i = 0; i < n; i++) {
            int valor = valores.get(i);
            float altoBarra = valor == 0 ? UIScale.scale(3f) : (float) valor / maximo * alto;
            float x = i * ancho + (ancho - anchoBarra) / 2f;
            float y = margenSuperior + alto - altoBarra;
            boolean ultima = i == n - 1;

            // Las barras de días anteriores van más suaves; en tema oscuro les subo la opacidad para que se distingan del fondo.
            g2.setColor(ultima ? acento : new Color(acento.getRed(), acento.getGreen(), acento.getBlue(), Tema.esOscuro() ? 150 : 110));
            g2.fill(barraRedondeada(x, y, anchoBarra, altoBarra, UIScale.scale(8f)));

            // El valor sobre la barra: nada si es cero (la barrita baja ya lo dice) y solo la mayor si no caben todos.
            if (valor > 0 && (valoresCaben || i == indiceMaximo)) {
                g2.setFont(fuenteValor);
                g2.setColor(ultima ? acento : Tema.textoSecundario());
                String texto = compacto(valor);
                // Lo centro sobre la barra, pero sin que se salga del gráfico (la primera y la última barra quedan en los bordes).
                float xTexto = x + (anchoBarra - fmValor.stringWidth(texto)) / 2f;
                xTexto = Math.max(0f, Math.min(xTexto, getWidth() - fmValor.stringWidth(texto)));
                g2.drawString(texto, xTexto, y - UIScale.scale(6f));
            }

            // Pinto la etiqueta del eje contando desde la última, así la de "hoy" o la última fecha siempre se ve.
            if ((n - 1 - i) % saltoEtiquetas == 0) {
                g2.setFont(fuenteEtiqueta);
                g2.setColor(ultima ? getForeground() : Tema.textoSecundario());
                String etiqueta = etiquetas.get(i);
                g2.drawString(etiqueta, x + (anchoBarra - fmEtiqueta.stringWidth(etiqueta)) / 2f,
                        getHeight() - fmEtiqueta.getDescent() - UIScale.scale(2f));
            }
        }

        if (todoEnCero) {
            String aviso = "Sin ingresos en este período";
            g2.setFont(base);
            g2.setColor(Tema.textoSecundario());
            FontMetrics fm = g2.getFontMetrics();
            float xAviso = (getWidth() - fm.stringWidth(aviso)) / 2f;
            float yAviso = margenSuperior + alto * 0.4f;
            // Le pongo un fondo del color de la tarjeta para que las líneas guía no atraviesen el texto.
            g2.setColor(Tema.fondoTarjeta());
            g2.fillRect((int) xAviso - UIScale.scale(8), (int) (yAviso - fm.getAscent()), fm.stringWidth(aviso) + UIScale.scale(16), fm.getHeight());
            g2.setColor(Tema.textoSecundario());
            g2.drawString(aviso, xAviso, yAviso);
        }
        g2.dispose();
    }

    /** Armo una barra con las esquinas superiores redondeadas y la base recta. */
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

    /** Devuelvo el valor corto para poner sobre la barra: $850, $1,3k, $325k, $1,2M (con coma decimal, como se escribe aquí). */
    private static String compacto(int valor) {
        if (valor >= 1_000_000) {
            return "$" + decimal(valor / 1_000_000.0) + "M";
        }
        if (valor >= 10_000) {
            return "$" + Math.round(valor / 1000.0) + "k";
        }
        if (valor >= 1000) {
            return "$" + decimal(valor / 1000.0) + "k";
        }
        return "$" + valor;
    }

    /** Devuelvo un decimal con coma, sin el ",0" sobrante (1,0 pasa a 1). */
    private static String decimal(double valor) {
        String texto = String.format(java.util.Locale.US, "%.1f", valor);
        return (texto.endsWith(".0") ? texto.substring(0, texto.length() - 2) : texto).replace('.', ',');
    }
}
