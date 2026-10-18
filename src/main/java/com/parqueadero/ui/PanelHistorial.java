package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.util.FiltroMayusculas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Historial de parqueos y lavados ya cobrados, con búsqueda por placa y período (siempre limitado en cantidad). */
public class PanelHistorial extends JPanel {
    private static final int LIMITE = 500;
    private static final String[] PERIODOS = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Últimos 90 días", "Todo"};

    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final DefaultTableModel modeloParqueo;
    private final DefaultTableModel modeloLavado;
    private final JTextField campoPlaca = new JTextField(12);
    private final JComboBox<String> comboPeriodo = new JComboBox<>(PERIODOS);
    private final JLabel labelResumen = new JLabel();

    public PanelHistorial(ParqueoService parqueoService, LavadoService lavadoService) {
        this.parqueoService = parqueoService;
        this.lavadoService = lavadoService;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        FiltroMayusculas.aplicar(campoPlaca);
        campoPlaca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar placa");
        campoPlaca.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.neutro("historial", 16));
        campoPlaca.addActionListener(e -> refrescar());
        comboPeriodo.setSelectedIndex(2);
        comboPeriodo.addActionListener(e -> refrescar());
        JButton botonBuscar = Estilos.primario(new JButton("Buscar"));
        botonBuscar.addActionListener(e -> refrescar());
        labelResumen.setForeground(Tema.textoSecundario());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setOpaque(false);
        filtros.add(campoPlaca);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(comboPeriodo);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(botonBuscar);
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(labelResumen);
        add(Tarjeta.conTitulo("Buscar en el historial",
                "Se muestran hasta " + LIMITE + " registros de cada tipo, los más recientes primero", filtros),
                BorderLayout.NORTH);

        modeloParqueo = new DefaultTableModel(new Object[]{"Placa", "Tipo", "Entrada", "Salida", "Total", "Pagó", "Vuelto", "Cajero", "Mensual"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tablaParqueo = Tablas.crear(modeloParqueo);
        Tarjeta panelParqueo = Tarjeta.conTitulo("Historial de parqueo", "Vehículos que ya salieron",
                sinBorde(new JScrollPane(tablaParqueo)));

        modeloLavado = new DefaultTableModel(new Object[]{"Placa", "Servicio", "Precio", "Inicio", "Fin", "Dueño", "Teléfono", "Pagó", "Vuelto", "Cajero"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tablaLavado = Tablas.crear(modeloLavado);
        Tarjeta panelLavado = Tarjeta.conTitulo("Historial de lavados", "Servicios de lavado completados",
                sinBorde(new JScrollPane(tablaLavado)));

        JPanel tablas = new JPanel(new GridLayout(2, 1, 0, 16));
        tablas.setOpaque(false);
        tablas.add(panelParqueo);
        tablas.add(panelLavado);
        add(tablas, BorderLayout.CENTER);
    }

    private static JScrollPane sinBorde(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private LocalDate fechaDesde() {
        LocalDate hoy = LocalDate.now();
        return switch (comboPeriodo.getSelectedIndex()) {
            case 0 -> hoy;
            case 1 -> hoy.minusDays(6);
            case 2 -> hoy.minusDays(29);
            case 3 -> hoy.minusDays(89);
            default -> null;
        };
    }

    public void refrescar() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String placa = campoPlaca.getText();
        LocalDate desde = fechaDesde();

        List<Vehiculo> vehiculos = parqueoService.obtenerHistorial(placa, desde, LIMITE);
        modeloParqueo.setRowCount(0);
        for (Vehiculo v : vehiculos) {
            modeloParqueo.addRow(new Object[]{
                    v.getPlaca(), v.getTipo(), v.getHoraEntrada().format(fmt),
                    v.getHoraSalida().format(fmt), "$" + v.getTotalParqueo(),
                    v.getMontoPagado() != null ? "$" + v.getMontoPagado() : "",
                    v.getVuelto() != null ? "$" + v.getVuelto() : "",
                    v.getUsuarioSalida() != null ? v.getUsuarioSalida() : "",
                    v.isEsMensual() ? "Sí" : "No"
            });
        }

        List<RegistroLavado> lavados = lavadoService.obtenerHistorial(placa, desde, LIMITE);
        modeloLavado.setRowCount(0);
        for (RegistroLavado r : lavados) {
            modeloLavado.addRow(new Object[]{
                    r.getPlaca(), r.getServicio().getNombre(), "$" + r.getPrecio(),
                    r.getFechaInicio().format(fmt), r.getFechaFin().format(fmt),
                    r.getNombreDueno() != null ? r.getNombreDueno() : "",
                    r.getTelefonoDueno() != null ? r.getTelefonoDueno() : "",
                    r.getMontoPagado() != null ? "$" + r.getMontoPagado() : "",
                    r.getVuelto() != null ? "$" + r.getVuelto() : "",
                    r.getUsuarioFin() != null ? r.getUsuarioFin() : ""
            });
        }
        labelResumen.setText(vehiculos.size() + " parqueos · " + lavados.size() + " lavados"
                + (vehiculos.size() >= LIMITE || lavados.size() >= LIMITE ? " (límite alcanzado: afina la búsqueda)" : ""));
    }
}
