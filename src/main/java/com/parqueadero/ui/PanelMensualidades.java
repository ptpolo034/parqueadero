package com.parqueadero.ui;

import com.parqueadero.modelo.EstadoMensualidad;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.servicio.MensualidadService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PanelMensualidades extends JPanel {
    private final MensualidadService mensualidadService;
    private final Runnable onCambio;
    private final AuthService authService;
    private final Usuario usuarioActivo;

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private List<Mensualidad> mensualidades = List.of();

    public PanelMensualidades(MensualidadService mensualidadService, AuthService authService, Usuario usuarioActivo,
                              Runnable onCambio) {
        this.authService = authService;
        this.usuarioActivo = usuarioActivo;
        this.mensualidadService = mensualidadService;
        this.onCambio = onCambio;

        setLayout(new BorderLayout());
        setOpaque(false);

        modelo = new DefaultTableModel(new Object[]{"Placa", "Cliente", "Teléfono", "Tipo", "Monto/mes",
                "Vence el", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabla = Tablas.crear(modelo, "Todavía no hay clientes con parqueo mensual");
        Tablas.alinearDerecha(tabla, "Monto/mes");
        tabla.getColumnModel().getColumn(6).setCellRenderer(new RenderEstado());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JButton botonRenovar = new JButton("Renovar (cobrar otro mes)...");
        JButton botonCancelar = new JButton("Cancelar mensualidad");
        botones.add(botonRenovar);
        botones.add(Box.createHorizontalStrut(10));
        botones.add(botonCancelar);
        botonRenovar.addActionListener(e -> renovar());
        botonCancelar.addActionListener(e -> cancelar());

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(scroll, BorderLayout.CENTER);
        cuerpo.add(botones, BorderLayout.SOUTH);
        add(Tarjeta.conTitulo("Clientes con parqueo mensual", "Selecciona una fila para renovar o cancelar", cuerpo),
                BorderLayout.CENTER);

        refrescar();
    }

    public void refrescar() {
        mensualidades = mensualidadService.obtenerTodas();
        modelo.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate hoy = LocalDate.now();
        for (Mensualidad m : mensualidades) {
            String estado;
            if (m.getEstado() == EstadoMensualidad.CANCELADA) {
                estado = "Cancelada";
            } else if (m.getFechaVencimiento().isBefore(hoy)) {
                estado = "Vencida (" + java.time.temporal.ChronoUnit.DAYS.between(m.getFechaVencimiento(), hoy) + " d)";
            } else {
                long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
                estado = "Activa (" + diasRestantes + " d)";
            }
            modelo.addRow(new Object[]{
                    m.getPlaca(), m.getNombreCliente() != null ? m.getNombreCliente() : "",
                    m.getTelefono() != null ? m.getTelefono() : "", m.getTipoVehiculo(),
                    Formato.dinero(m.getMontoMensual()), m.getFechaVencimiento().format(fmt), estado
            });
        }
    }

    private Mensualidad seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una mensualidad de la tabla");
            return null;
        }
        return mensualidades.get(fila);
    }

    private void renovar() {
        Mensualidad m = seleccionada();
        if (m == null) {
            return;
        }
        if (m.getEstado() == EstadoMensualidad.CANCELADA) {
            JOptionPane.showMessageDialog(this, "Esta mensualidad está cancelada, no se puede renovar");
            return;
        }
        JSpinner spinnerMeses = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        Object[] mensaje = {"¿Cuántos meses paga " + m.getPlaca() + "?", spinnerMeses};
        int resultado = JOptionPane.showConfirmDialog(this, mensaje, "Renovar mensualidad", JOptionPane.OK_CANCEL_OPTION);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }
        int meses = (Integer) spinnerMeses.getValue();
        int totalAPagar = m.getMontoMensual() * meses;
        Integer montoPagado = DialogoPago.pedirMontoPagado(this,
                "Renovación de mensualidad - " + m.getPlaca() + " (" + meses + " mes(es))", totalAPagar);
        if (montoPagado == null) {
            return;
        }
        try {
            Mensualidad renovada = mensualidadService.renovar(m.getId(), meses, montoPagado);
            JOptionPane.showMessageDialog(this, "Mensualidad renovada. Nuevo vencimiento: " +
                    renovada.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            refrescar();
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelar() {
        Mensualidad m = seleccionada();
        if (m == null) {
            return;
        }
        String autorizadoPor = DialogoAutorizacionAdmin.solicitarAutorizacion(SwingUtilities.getWindowAncestor(this),
                authService, usuarioActivo, "Cancelar la mensualidad de " + m.getPlaca());
        if (autorizadoPor == null) {
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Cancelar la mensualidad de " + m.getPlaca() + "? A partir de ahora se le cobrará por horas.",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            mensualidadService.cancelar(m.getId(), autorizadoPor);
            refrescar();
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pinto el estado con color: verde si está vigente, rojo si venció y gris si fue cancelada. */
    private static class RenderEstado extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            if (!seleccionada) {
                String texto = String.valueOf(valor);
                setForeground(texto.startsWith("Vencida") ? Tema.rojo()
                        : texto.startsWith("Activa") ? Tema.verde() : Tema.textoSecundario());
            }
            return c;
        }
    }
}
