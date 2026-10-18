package com.parqueadero.ui;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/** Tablas con el aspecto de la aplicación: encabezados alineados con sus datos y sin selección de columnas. */
public final class Tablas {
    private Tablas() {
    }

    public static JTable crear(TableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.getTableHeader().setReorderingAllowed(false);
        if (tabla.getTableHeader().getDefaultRenderer() instanceof DefaultTableCellRenderer r) {
            r.setHorizontalAlignment(SwingConstants.LEFT);
        }
        return tabla;
    }
}
