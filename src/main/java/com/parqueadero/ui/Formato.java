package com.parqueadero.ui;

import com.parqueadero.modelo.ServicioLavado;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos que repito en todas las pantallas para que el dinero y las fechas se vean igual en todas partes:
 * dinero con punto de miles ($25.000) y fechas con el día primero y el año corto (27/10/26 19:03).
 */
public final class Formato {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final DateTimeFormatter FECHA_HORA_SEGUNDOS = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    private Formato() {
    }

    /** Devuelvo el dinero con signo y punto de miles: 25000 pasa a "$25.000". */
    public static String dinero(int valor) {
        // Uso Locale.ROOT para que el separador sea siempre la coma (que cambio por punto), sin importar la región
        // del computador: en Francia o Suiza "%,d" separa con espacio o apóstrofo y salía "$25 000" o "$25'000".
        return "$" + String.format(Locale.ROOT, "%,d", valor).replace(',', '.');
    }

    /** Hago lo mismo que {@link #dinero(int)}, pero devuelvo vacío si el valor no existe (por ejemplo, un pago sin registrar). */
    public static String dineroONada(Integer valor) {
        return valor == null ? "" : dinero(valor);
    }

    /**
     * Convierto lo que escribe el cajero en un número entero y acepto el punto de miles, la coma, el signo $ y
     * los espacios ("20.000", "$20000" o "20 000" dan 20000). Si no es un número, lanzo NumberFormatException.
     */
    public static int entero(String texto) {
        return Integer.parseInt(texto.trim().replaceAll("[\\s.,$]", ""));
    }

    public static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    /** Devuelvo la fecha y hora cortas: "27/10 19:03" si es de este año y "27/10/25 19:03" si es de otro (así cabe en las tablas). */
    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora.getYear() == LocalDate.now().getYear()
                ? fechaHora.format(DIA_MES_HORA) : fechaHora.format(FECHA_HORA);
    }

    public static String fechaHoraConSegundos(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA_HORA_SEGUNDOS);
    }

    /** Hago que la lista de servicios de lavado muestre el precio con punto de miles ("Lavado básico - $15.000"). */
    public static void conPreciosFormateados(JComboBox<ServicioLavado> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean seleccionado, boolean conFoco) {
                Object texto = valor instanceof ServicioLavado s ? s.getNombre() + " - " + dinero(s.getPrecio()) : valor;
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, conFoco);
            }
        });
    }
}
