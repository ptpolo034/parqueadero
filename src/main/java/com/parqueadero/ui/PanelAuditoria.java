package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.RegistroAuditoria;
import com.parqueadero.servicio.AuditoriaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Bitácora (solo administrador): quién canceló, editó o cambió qué, cuándo y con qué autorización. */
public class PanelAuditoria extends JPanel {
    private static final int LIMITE = 500;
    private static final String[] PERIODOS = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Últimos 90 días", "Todo"};

    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final DefaultTableModel modelo;
    private final JTextField campoBusqueda = new JTextField(22);
    private final JComboBox<String> comboPeriodo = new JComboBox<>(PERIODOS);
    private final JLabel labelResumen = new JLabel();

    public PanelAuditoria() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        campoBusqueda.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por usuario, acción o placa");
        campoBusqueda.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.neutro("auditoria", 16));
        campoBusqueda.addActionListener(e -> refrescar());
        comboPeriodo.setSelectedIndex(2);
        comboPeriodo.addActionListener(e -> refrescar());
        JButton botonBuscar = Estilos.primario(new JButton("Buscar"));
        botonBuscar.addActionListener(e -> refrescar());
        labelResumen.setForeground(Tema.textoSecundario());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filtros.setOpaque(false);
        filtros.add(campoBusqueda);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(comboPeriodo);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(botonBuscar);
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(labelResumen);
        add(Tarjeta.conTitulo("Auditoría", "Registro de cancelaciones, ediciones, cambios de tarifas, usuarios e inicios de sesión",
                filtros), BorderLayout.NORTH);

        modelo = new DefaultTableModel(new Object[]{"Fecha y hora", "Usuario", "Autorizó", "Acción", "Detalle"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tabla = Tablas.crear(modelo);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(520);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(Tarjeta.conTitulo("Movimientos registrados", null, scroll), BorderLayout.CENTER);
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<RegistroAuditoria> registros = auditoriaService.listar(fechaDesde(), campoBusqueda.getText(), LIMITE);
        modelo.setRowCount(0);
        for (RegistroAuditoria r : registros) {
            modelo.addRow(new Object[]{r.fechaHora().format(fmt), nz(r.usuario()), nz(r.autorizadoPor()),
                    r.accion().replace('_', ' '), nz(r.detalle())});
        }
        labelResumen.setText(registros.size() + " registros" + (registros.size() >= LIMITE ? " (límite alcanzado)" : ""));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
