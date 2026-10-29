package com.parqueadero.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/** Aquí envuelvo el contenido de una pantalla en un área desplazable transparente que se ajusta al ancho disponible. */
public final class Pagina {
    private Pagina() {
    }

    public static JScrollPane desplazable(JComponent contenido) {
        JPanel base = new Base(false);
        base.add(contenido, BorderLayout.NORTH);
        return envolver(base);
    }

    /**
     * Lo uso en las pantallas completas: estiro el contenido hasta llenar la ventana, pero si la ventana es
     * más baja que lo que el contenido necesita (por ejemplo a 1120x700), dejo que aparezca la barra de desplazamiento
     * en vez de aplastar las tablas o cortar los botones de abajo.
     */
    public static JScrollPane desplazableAjustable(JComponent contenido) {
        JPanel base = new Base(true);
        base.add(contenido, BorderLayout.CENTER);
        return envolver(base);
    }

    private static JScrollPane envolver(JPanel base) {
        JScrollPane scroll = new JScrollPane(base);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private static class Base extends JPanel implements Scrollable {
        private final boolean ajustaAlto;

        Base(boolean ajustaAlto) {
            super(new BorderLayout());
            this.ajustaAlto = ajustaAlto;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Mientras la ventana alcance para el alto que pide el contenido, lo estiro hasta el fondo.
            return ajustaAlto && getParent() instanceof JViewport vista
                    && vista.getHeight() >= getPreferredSize().height;
        }
    }
}
