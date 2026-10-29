package com.parqueadero.ui;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Aquí reúno las tablas con el aspecto de la aplicación: encabezados alineados con sus datos, columnas que
 * ajusto a su contenido (para que no se corten los textos), números a la derecha y un mensaje cuando no hay filas.
 */
public final class Tablas {
    private static final String COLUMNAS_A_LA_DERECHA = "parqueadero.columnasADerecha";
    private static final String MEDIDAS = "parqueadero.medidasColumnas";
    private static final int FILAS_VISIBLES_POR_DEFECTO = 3;
    /** Una columna cuyo texto más largo no pasa de estos caracteres la considero "corta" (fechas, placas, dinero). */
    private static final int CARACTERES_COLUMNA_CORTA = 11;

    private Tablas() {
    }

    /** Anchos que medí para cada columna: el mínimo duro (el título) y el mínimo "blando" (el contenido completo si es corto). */
    private record Medidas(int[] duro, int[] blando) {
    }

    public static JTable crear(TableModel modelo) {
        return crear(modelo, "Sin registros");
    }

    /** Creo la tabla; {@code mensajeVacio} lo muestro en el centro cuando no tiene ninguna fila. */
    public static JTable crear(TableModel modelo, String mensajeVacio) {
        JTable tabla = new JTable(modelo) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0 && mensajeVacio != null && !mensajeVacio.isEmpty()) {
                    pintarMensajeVacio((Graphics2D) g.create(), this, mensajeVacio);
                }
            }


            /** Si el texto de una celda no cabe y se ve cortado con "...", lo muestro completo en un cuadro flotante. */
            @Override
            public String getToolTipText(java.awt.event.MouseEvent evento) {
                java.awt.Point punto = evento.getPoint();
                int fila = rowAtPoint(punto);
                int columna = columnAtPoint(punto);
                if (fila < 0 || columna < 0) {
                    return super.getToolTipText(evento);
                }
                Object valor = getValueAt(fila, columna);
                if (valor == null || valor.toString().isEmpty()) {
                    return null;
                }
                Component celda = prepareRenderer(getCellRenderer(fila, columna), fila, columna);
                return celda.getPreferredSize().width > getCellRect(fila, columna, false).width ? valor.toString() : null;
            }

            /** Reparto el ancho con mi propia regla (ver {@link Tablas#repartir}) para no cortar fechas, placas ni dinero. */
            @Override
            public void doLayout() {
                JTableHeader encabezado = getTableHeader();
                TableColumnModel columnas = getColumnModel();
                int n = columnas.getColumnCount();
                int disponible = getWidth();
                if ((encabezado != null && encabezado.getResizingColumn() != null) || n == 0 || disponible <= 0) {
                    super.doLayout(); // el usuario está arrastrando el borde de una columna: lo dejo como siempre
                    return;
                }
                int[] preferido = new int[n];
                for (int i = 0; i < n; i++) {
                    preferido[i] = columnas.getColumn(i).getPreferredWidth();
                }
                Medidas medidas = (Medidas) getClientProperty(MEDIDAS);
                int[] duro = new int[n];
                int[] blando = new int[n];
                for (int i = 0; i < n; i++) {
                    boolean hayMedida = medidas != null && medidas.duro().length == n;
                    duro[i] = hayMedida ? medidas.duro()[i] : columnas.getColumn(i).getMinWidth();
                    blando[i] = hayMedida ? medidas.blando()[i] : duro[i];
                    preferido[i] = Math.max(preferido[i], blando[i]);
                }
                int[] ancho = repartir(disponible, duro, blando, preferido);
                for (int i = 0; i < n; i++) {
                    columnas.getColumn(i).setWidth(ancho[i]);
                }
            }
        };
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setToolTipText(""); // registro la tabla para mostrar el cuadro flotante con el texto completo de las celdas cortadas
        // Hago que la tabla llene todo el espacio de su tarjeta, así el mensaje de "vacío" queda centrado y no flotando arriba.
        tabla.setFillsViewportHeight(true);
        // Mantengo el modo "ajustar al ancho" para que la tabla siga el ancho de su tarjeta; el reparto lo hago yo.
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Le pongo un alto mínimo razonable (unas pocas filas), para que la pantalla pueda crecer pero no se aplaste en ventanas chicas.
        tabla.setPreferredScrollableViewportSize(new Dimension(UIScale.scale(300),
                tabla.getRowHeight() * FILAS_VISIBLES_POR_DEFECTO));

        // Alineo el encabezado igual que los datos de su columna (los números a la derecha, el resto a la izquierda).
        Set<String> derechas = new HashSet<>();
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            if (Number.class.isAssignableFrom(modelo.getColumnClass(i))) {
                derechas.add(modelo.getColumnName(i));
            }
        }
        tabla.putClientProperty(COLUMNAS_A_LA_DERECHA, derechas);
        TableCellRenderer base = tabla.getTableHeader().getDefaultRenderer();
        tabla.getTableHeader().setDefaultRenderer((t, valor, seleccionada, foco, fila, columna) -> {
            Component c = base.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            if (c instanceof JLabel etiqueta) {
                @SuppressWarnings("unchecked")
                Set<String> aDerecha = (Set<String>) tabla.getClientProperty(COLUMNAS_A_LA_DERECHA);
                etiqueta.setHorizontalAlignment(aDerecha != null && aDerecha.contains(String.valueOf(valor))
                        ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            return c;
        });

        // Cada vez que cambian los datos, vuelvo a medir las columnas (una sola vez por tanda de cambios).
        boolean[] pendiente = {false};
        modelo.addTableModelListener(e -> {
            if (!pendiente[0]) {
                pendiente[0] = true;
                SwingUtilities.invokeLater(() -> {
                    pendiente[0] = false;
                    ajustarAnchos(tabla);
                });
            }
        });
        ajustarAnchos(tabla);
        return tabla;
    }

    /** Pongo a la derecha (encabezado y datos) las columnas con esos títulos: dinero, cantidades, etc. */
    public static void alinearDerecha(JTable tabla, String... titulos) {
        @SuppressWarnings("unchecked")
        Set<String> aDerecha = (Set<String>) tabla.getClientProperty(COLUMNAS_A_LA_DERECHA);
        if (aDerecha == null) {
            aDerecha = new HashSet<>();
            tabla.putClientProperty(COLUMNAS_A_LA_DERECHA, aDerecha);
        }
        aDerecha.addAll(Arrays.asList(titulos));
        DefaultTableCellRenderer cuerpo = new DefaultTableCellRenderer();
        cuerpo.setHorizontalAlignment(SwingConstants.RIGHT);
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            TableColumn columna = columnas.getColumn(i);
            if (aDerecha.contains(String.valueOf(columna.getHeaderValue()))
                    && columna.getCellRenderer() == null) {
                columna.setCellRenderer(cuerpo);
            }
        }
        tabla.getTableHeader().repaint();
    }

    /**
     * Mido el encabezado y las primeras filas de cada columna. El ancho preferido es el del contenido más
     * ancho; el mínimo duro es el del título (nunca se corta con "..."), y si todos los datos de la columna
     * son cortos (fechas, placas, dinero) el mínimo blando es el ancho completo, para no cortarlos tampoco.
     */
    public static void ajustarAnchos(JTable tabla) {
        TableColumnModel columnas = tabla.getColumnModel();
        int n = columnas.getColumnCount();
        int holgura = UIScale.scale(6);
        int filasAMedir = Math.min(tabla.getRowCount(), 150);
        int[] duro = new int[n];
        int[] blando = new int[n];
        for (int c = 0; c < n; c++) {
            TableColumn columna = columnas.getColumn(c);
            TableCellRenderer renderEncabezado = columna.getHeaderRenderer() != null
                    ? columna.getHeaderRenderer() : tabla.getTableHeader().getDefaultRenderer();
            int anchoEncabezado = renderEncabezado.getTableCellRendererComponent(
                    tabla, columna.getHeaderValue(), false, false, -1, c).getPreferredSize().width;
            int ancho = anchoEncabezado;
            boolean corta = true;
            for (int f = 0; f < filasAMedir; f++) {
                Component celda = tabla.prepareRenderer(tabla.getCellRenderer(f, c), f, c);
                ancho = Math.max(ancho, celda.getPreferredSize().width);
                Object valor = tabla.getValueAt(f, c);
                if (valor != null && valor.toString().length() > CARACTERES_COLUMNA_CORTA) {
                    corta = false;
                }
            }
            ancho += holgura;
            duro[c] = anchoEncabezado + holgura;
            blando[c] = corta ? ancho : duro[c];
            columna.setMinWidth(duro[c]);
            columna.setPreferredWidth(Math.max(ancho, duro[c]));
        }
        tabla.putClientProperty(MEDIDAS, new Medidas(duro, blando));
        tabla.revalidate();
        tabla.getTableHeader().repaint();
    }

    /**
     * Reparto {@code disponible} píxeles entre las columnas:
     * <ul>
     *   <li>Si todo cabe a su ancho preferido, lo que sobra lo reparto en proporción al ancho de cada columna.</li>
     *   <li>Si no cabe, las columnas "cortas" (fechas, placas, dinero) conservan su ancho completo y las de texto
     *       largo las achico hasta el ancho de su título.</li>
     *   <li>Si ni así cabe, achico todas por igual entre su ancho completo y el de su título.</li>
     * </ul>
     */
    static int[] repartir(int disponible, int[] duro, int[] blando, int[] preferido) {
        int n = preferido.length;
        int[] ancho = new int[n];
        int sumaPreferido = Arrays.stream(preferido).sum();
        int sumaBlando = Arrays.stream(blando).sum();
        if (sumaPreferido <= disponible) {
            int repartido = 0;
            for (int i = 0; i < n; i++) {
                ancho[i] = preferido[i] + (int) ((long) (disponible - sumaPreferido) * preferido[i] / sumaPreferido);
                repartido += ancho[i];
            }
            ancho[n - 1] += disponible - repartido;
        } else if (sumaBlando <= disponible) {
            // Las columnas que no pueden achicarse (blando == preferido) las dejo como están; entre las demás
            // reparto lo que queda de forma equitativa, sin bajar de su blando ni pasar de su preferido.
            // Busco el nivel más alto que cabe: cada columna recibe ese nivel, acotado entre su blando y su preferido.
            // (Antes fijaba en su preferido las columnas que cabían en su parte sin mirar el título de las demás, y la
            // suma podía pasarse del ancho disponible; además perdía los píxeles del residuo de la división.)
            int bajo = 0;
            int alto = Arrays.stream(preferido).max().orElse(0);
            while (bajo < alto) {
                int medio = (int) (((long) bajo + alto + 1) / 2);
                if (sumaConNivel(medio, blando, preferido) <= disponible) {
                    bajo = medio;
                } else {
                    alto = medio - 1;
                }
            }
            long repartido = 0;
            for (int i = 0; i < n; i++) {
                ancho[i] = Math.min(preferido[i], Math.max(blando[i], bajo));
                repartido += ancho[i];
            }
            // Los píxeles que sobran (siempre menos que las columnas que van en el nivel) los doy de a uno, de izquierda a derecha.
            long sobra = disponible - repartido;
            for (int i = 0; i < n && sobra > 0; i++) {
                if (ancho[i] == bajo && ancho[i] < preferido[i]) {
                    ancho[i]++;
                    sobra--;
                }
            }
        } else {
            long sumaDuro = Arrays.stream(duro).sum();
            double factor = sumaBlando == sumaDuro ? 0 : Math.max(0, Math.min(1, (double) (disponible - sumaDuro) / (sumaBlando - sumaDuro)));
            int repartido = 0;
            for (int i = 0; i < n; i++) {
                ancho[i] = duro[i] + (int) ((blando[i] - duro[i]) * factor);
                repartido += ancho[i];
            }
            ancho[n - 1] += Math.max(0, disponible - repartido);
        }
        return ancho;
    }

    /** Sumo los anchos que quedarían si cada columna recibe {@code nivel}, acotado entre su blando y su preferido. */
    private static long sumaConNivel(int nivel, int[] blando, int[] preferido) {
        long suma = 0;
        for (int i = 0; i < preferido.length; i++) {
            suma += Math.min(preferido[i], Math.max(blando[i], nivel));
        }
        return suma;
    }

    /** Escribo el mensaje de "sin filas" centrado en la parte visible de la tabla. */
    private static void pintarMensajeVacio(Graphics2D g2, JTable tabla, String mensaje) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(tabla.getFont());
        g2.setColor(Tema.textoSecundario());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle visible = tabla.getVisibleRect();
        int x = visible.x + (visible.width - fm.stringWidth(mensaje)) / 2;
        int y = visible.y + Math.min(visible.height / 2, UIScale.scale(48)) + fm.getAscent() / 2;
        g2.drawString(mensaje, Math.max(visible.x, x), y);
        g2.dispose();
    }
}
