package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.parqueadero.util.FiltroMayusculas;
import com.parqueadero.modelo.EstadoMensualidad;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.MensualidadService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.servicio.ReporteService;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Mi pantalla de inicio: un vistazo al estado del negocio en este momento. */
public class PanelInicio extends JPanel {
    /** Alto mínimo (sin escalar) de cada fila de tarjetas de abajo: tiene que caber el título, el encabezado y dos filas de tabla. */
    private static final int ALTO_MINIMO_FILA = 188;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE d", new java.util.Locale("es", "CO"));

    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final MensualidadService mensualidadService;
    private final ReporteService reporteService;

    private final TarjetaKpi kpiCupos = new TarjetaKpi("Cupos disponibles", "carro", Estilos.Tono.VERDE, true);
    private final TarjetaKpi kpiIngresos = new TarjetaKpi("Ingresos de hoy", "dinero", Estilos.Tono.ACENTO, false);
    private final TarjetaKpi kpiVehiculos = new TarjetaKpi("Vehículos adentro", "entrada", Estilos.Tono.AMBAR, false);
    private final TarjetaKpi kpiLavados = new TarjetaKpi("Lavados en curso", "lavado", Estilos.Tono.ACENTO, false);

    private final GraficoBarras grafico = new GraficoBarras();
    private final TablaTarjeta tablaVehiculos = new TablaTarjeta(
            new String[]{"Placa", "Tipo", "Entró", "Tiempo", "Plan"}, "No hay vehículos en el parqueadero");
    private final TablaTarjeta tablaMensualidades = new TablaTarjeta(
            new String[]{"Placa", "Cliente", "Vence", "Estado"}, "Ninguna mensualidad por vencer");
    private final TablaTarjeta tablaLavados = new TablaTarjeta(
            new String[]{"Placa", "Servicio", "Desde"}, "No hay lavados en curso");

    private final AccionesVehiculo acciones;
    private final JTextField campoPlaca = new JTextField();

    public PanelInicio(ParqueoService parqueoService, LavadoService lavadoService,
                       MensualidadService mensualidadService, ReporteService reporteService,
                       AccionesVehiculo acciones) {
        this.parqueoService = parqueoService;
        this.lavadoService = lavadoService;
        this.mensualidadService = mensualidadService;
        this.reporteService = reporteService;
        this.acciones = acciones;

        setLayout(new BorderLayout());
        setOpaque(false);

        // Cuadro de placa: lo dejo pequeño y centrado; el sistema detecta solo si el vehículo entra o sale
        FiltroMayusculas.aplicar(campoPlaca);
        campoPlaca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "PLACA");
        campoPlaca.putClientProperty(FlatClientProperties.STYLE,
                "font:bold +8; margin:8,14,8,14; arc:14; background:fade($Component.accentColor,10%);"
                        + "borderColor:fade($Component.accentColor,45%); focusedBorderColor:$Component.accentColor");
        campoPlaca.setHorizontalAlignment(JTextField.CENTER);
        campoPlaca.setPreferredSize(new Dimension(320, 52));
        campoPlaca.addActionListener(e -> abrirVehiculo());

        JLabel ayuda = Estilos.subtitulo("Digita la placa del vehículo");
        ayuda.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel cuerpoPlaca = new JPanel(new GridBagLayout());
        cuerpoPlaca.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 8, 0);
        cuerpoPlaca.add(campoPlaca, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        cuerpoPlaca.add(ayuda, gbc);
        Tarjeta tarjetaPlaca = new Tarjeta(new BorderLayout());
        tarjetaPlaca.add(cuerpoPlaca, BorderLayout.CENTER);
        JPanel panelPlaca = new JPanel(new BorderLayout());
        panelPlaca.setOpaque(false);
        panelPlaca.add(tarjetaPlaca, BorderLayout.CENTER);

        JPanel indicadores = new JPanel(new GridLayout(1, 4, 16, 0));
        indicadores.setOpaque(false);
        indicadores.add(kpiCupos);
        indicadores.add(kpiIngresos);
        indicadores.add(kpiVehiculos);
        indicadores.add(kpiLavados);

        JPanel arriba = new JPanel(new BorderLayout(0, 16));
        arriba.setOpaque(false);
        arriba.add(panelPlaca, BorderLayout.NORTH);
        arriba.add(indicadores, BorderLayout.CENTER);
        arriba.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel fila1 = new JPanel(new GridLayout(1, 2, 16, 0));
        fila1.setOpaque(false);
        fila1.add(tarjetaConTitulo("Ingresos de los últimos 7 días", "Parqueo, lavados y mensualidades", grafico));
        fila1.add(tarjetaConTitulo("Mensualidades por vencer", "Vencidas o que vencen en los próximos 7 días", tablaMensualidades));

        JPanel fila2 = new JPanel(new GridLayout(1, 2, 16, 0));
        fila2.setOpaque(false);
        fila2.add(tarjetaConTitulo("En el parqueadero ahora", "Vehículos con entrada registrada", tablaVehiculos));
        fila2.add(tarjetaConTitulo("Lavados en curso", "Pendientes de cobrar", tablaLavados));

        // Fijo el alto mínimo de las dos filas de tarjetas: a 1120x700 la pantalla se desplaza en vez de dejar una sola fila por tabla.
        JPanel filas = new JPanel(new GridLayout(2, 1, 0, 16));
        filas.setPreferredSize(new Dimension(0, UIScale.scale(2 * ALTO_MINIMO_FILA + 16)));
        filas.setOpaque(false);
        filas.add(fila1);
        filas.add(fila2);

        add(arriba, BorderLayout.NORTH);
        add(filas, BorderLayout.CENTER);
    }

    private static Tarjeta tarjetaConTitulo(String titulo, String subtitulo, Component cuerpo) {
        return Tarjeta.conTitulo(titulo, subtitulo, cuerpo);
    }

    /** Dejo el cursor en el cuadro de placa, listo para escribir o escanear. */
    public void enfocar() {
        SwingUtilities.invokeLater(campoPlaca::requestFocusInWindow);
    }

    private void abrirVehiculo() {
        String placa = campoPlaca.getText().trim().toUpperCase();
        campoPlaca.setText("");
        if (placa.isEmpty()) {
            return;
        }
        DialogoVehiculo.mostrar(this, acciones, placa);
        enfocar();
    }

    public void refrescar() {
        LocalDate hoy = LocalDate.now();

        int totales = parqueoService.cuposTotales();
        int disponibles = parqueoService.cuposDisponibles();
        double ocupacion = totales == 0 ? 0 : (double) (totales - disponibles) / totales;
        kpiCupos.actualizar(disponibles + " de " + totales, Math.round(ocupacion * 100) + "% de ocupación");
        kpiCupos.actualizarBarra(ocupacion,
                ocupacion >= 0.9 ? Estilos.Tono.ROJO : ocupacion >= 0.7 ? Estilos.Tono.AMBAR : Estilos.Tono.VERDE);

        int ingresosHoy = reporteService.ingresosTotalesEnRango(hoy, hoy);
        kpiIngresos.actualizar(Formato.dinero(ingresosHoy),
                "Todos los servicios");

        List<Vehiculo> activos = parqueoService.obtenerActivos();
        long mensuales = activos.stream().filter(Vehiculo::isEsMensual).count();
        kpiVehiculos.actualizar(String.valueOf(activos.size()), mensuales + " con mensualidad");

        List<RegistroLavado> lavados = lavadoService.obtenerPendientes();
        kpiLavados.actualizar(String.valueOf(lavados.size()), lavados.isEmpty() ? "Todo al día" : "Pendientes de cobrar");

        // Armo el gráfico de los últimos 7 días
        List<String> etiquetas = new ArrayList<>();
        List<Integer> valores = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            etiquetas.add(i == 0 ? "Hoy" : capitalizar(dia.format(DIA_CORTO)));
            valores.add(reporteService.ingresosTotalesEnRango(dia, dia));
        }
        grafico.establecerDatos(etiquetas, valores);

        // Lleno la tabla de vehículos en el parqueadero
        List<Object[]> filasVehiculos = new ArrayList<>();
        for (Vehiculo v : activos) {
            filasVehiculos.add(new Object[]{v.getPlaca(), v.getTipo(), v.getHoraEntrada().format(HORA),
                    tiempo(v.getHoraEntrada()), v.isEsMensual() ? "Mensual" : "Por hora"});
        }
        tablaVehiculos.establecerFilas(filasVehiculos);

        // Lleno la tabla de mensualidades por vencer (vencidas o que vencen en los próximos 7 días)
        List<Object[]> filasMensualidades = new ArrayList<>();
        for (Mensualidad m : mensualidadService.obtenerTodas()) {
            if (m.getEstado() != EstadoMensualidad.ACTIVA) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
            if (dias > 7) {
                continue;
            }
            String estado = dias < 0 ? "Vencida " + (-dias) + " d" : dias == 0 ? "Vence hoy" : "Vence en " + dias + " d";
            filasMensualidades.add(new Object[]{m.getPlaca(), m.getNombreCliente() != null ? m.getNombreCliente() : "",
                    m.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM")), estado});
        }
        tablaMensualidades.establecerFilas(filasMensualidades);
        tablaMensualidades.colorearUltimaColumna();

        // Lleno la tabla de lavados en curso
        List<Object[]> filasLavados = new ArrayList<>();
        for (RegistroLavado r : lavados) {
            filasLavados.add(new Object[]{r.getPlaca(), r.getServicio().getNombre(), r.getFechaInicio().format(HORA)});
        }
        tablaLavados.establecerFilas(filasLavados);
    }

    private static String tiempo(LocalDateTime desde) {
        Duration d = Duration.between(desde, LocalDateTime.now());
        long horas = d.toHours();
        long minutos = d.toMinutesPart();
        return horas > 0 ? horas + " h " + minutos + " min" : minutos + " min";
    }

    private static String capitalizar(String s) {
        s = s.replace(".", "");
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Aquí armo una tabla de solo lectura dentro de una tarjeta, con un mensaje amable cuando no hay datos. */
    private static class TablaTarjeta extends JPanel {
        private final DefaultTableModel modelo;
        private final JTable tabla;
        private final CardLayout cartas = new CardLayout();

        TablaTarjeta(String[] columnas, String mensajeVacio) {
            setLayout(cartas);
            setOpaque(false);
            modelo = new DefaultTableModel(columnas, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            tabla = Tablas.crear(modelo);
            tabla.setFocusable(false);
            tabla.setRowSelectionAllowed(false);
            tabla.getTableHeader().setReorderingAllowed(false);
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            add(scroll, "tabla");

            JLabel vacio = new JLabel(mensajeVacio, SwingConstants.CENTER);
            vacio.setForeground(Tema.textoSecundario());
            add(vacio, "vacio");
        }

        void establecerFilas(List<Object[]> filas) {
            modelo.setRowCount(0);
            filas.forEach(modelo::addRow);
            cartas.show(this, filas.isEmpty() ? "vacio" : "tabla");
        }

        void colorearUltimaColumna() {
            int ultima = modelo.getColumnCount() - 1;
            tabla.getColumnModel().getColumn(ultima).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                               boolean foco, int fila, int col) {
                    Component c = super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
                    String texto = String.valueOf(valor);
                    setForeground(texto.startsWith("Vencida") ? Tema.rojo() : Tema.ambar());
                    return c;
                }
            });
        }
    }
}
