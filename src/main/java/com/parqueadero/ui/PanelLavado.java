package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.util.FiltroMayusculas;
import com.parqueadero.util.GeneradorRecibos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PanelLavado extends JPanel {
    private final LavadoService lavadoService;
    private final ParqueoService parqueoService;
    private final ConfiguracionService configuracionService;
    private final Runnable onCambio;

    private final JTextField campoPlaca;
    private final JComboBox<ServicioLavado> comboServicio;
    private final JComboBox<VehiculoItem> comboVehiculo;
    private final JTextField campoNombreDueno;
    private final JTextField campoTelefonoDueno;

    private final DefaultTableModel modeloPendientes;
    private final JTable tablaPendientes;
    private List<RegistroLavado> pendientes = List.of();

    public PanelLavado(LavadoService lavadoService, ParqueoService parqueoService,
                       ConfiguracionService configuracionService, Runnable onCambio) {
        this.lavadoService = lavadoService;
        this.parqueoService = parqueoService;
        this.configuracionService = configuracionService;
        this.onCambio = onCambio;

        setLayout(new BorderLayout(16, 0));
        setOpaque(false);

        campoPlaca = new JTextField();
        FiltroMayusculas.aplicar(campoPlaca);
        campoPlaca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ABC123");
        campoPlaca.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; margin:6,12,6,12");
        comboServicio = new JComboBox<>(lavadoService.obtenerServicios().toArray(new ServicioLavado[0]));
        Formato.conPreciosFormateados(comboServicio);
        comboVehiculo = new JComboBox<>();
        campoNombreDueno = new JTextField();
        campoTelefonoDueno = new JTextField();
        JButton botonRegistrar = Estilos.primarioGrande(new JButton("Registrar lavado", Iconos.color("lavado", 18, Color.WHITE)));
        botonRegistrar.addActionListener(e -> registrar());
        // Cuando presiono Enter en el nombre o el teléfono del dueño (los últimos campos), registro el lavado.
        campoNombreDueno.addActionListener(e -> registrar());
        campoTelefonoDueno.addActionListener(e -> registrar());

        Formulario formulario = new Formulario();
        formulario.campo("Placa", campoPlaca);
        formulario.campo("Servicio", comboServicio);
        formulario.campo("Vehículo parqueado (opcional)", comboVehiculo);
        formulario.campo("Nombre del dueño (opcional)", campoNombreDueno);
        formulario.campo("Teléfono del dueño (opcional)", campoTelefonoDueno);
        formulario.agregar(botonRegistrar);

        JPanel envoltorioForm = new JPanel(new BorderLayout());
        envoltorioForm.setOpaque(false);
        envoltorioForm.add(Tarjeta.conTitulo("Nuevo lavado", "Para vehículos que solo vienen a lavar", formulario),
                BorderLayout.NORTH);
        envoltorioForm.setPreferredSize(new Dimension(290, 0));
        add(envoltorioForm, BorderLayout.WEST);

        modeloPendientes = new DefaultTableModel(new Object[]{"ID", "Placa", "Servicio", "Precio", "Inicio", "Dueño", "Teléfono"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaPendientes = Tablas.crear(modeloPendientes, "No hay lavados en curso");
        Tablas.alinearDerecha(tablaPendientes, "Precio");
        // El número interno (ID) no le sirve al cajero: lo escondo de la vista pero sigo teniéndolo en el modelo.
        tablaPendientes.removeColumn(tablaPendientes.getColumnModel().getColumn(0));
        JScrollPane scrollTabla = new JScrollPane(tablaPendientes);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());

        JButton botonCompletar = new JButton("Marcar como completado", Iconos.neutro("caja", 16));
        botonCompletar.addActionListener(e -> completar());
        JPanel pieTabla = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pieTabla.setOpaque(false);
        pieTabla.add(botonCompletar);

        JPanel cuerpoTabla = new JPanel(new BorderLayout(0, 12));
        cuerpoTabla.setOpaque(false);
        cuerpoTabla.add(scrollTabla, BorderLayout.CENTER);
        cuerpoTabla.add(pieTabla, BorderLayout.SOUTH);
        add(Tarjeta.conTitulo("Lavados en curso", "Selecciona uno para cobrarlo y completarlo", cuerpoTabla),
                BorderLayout.CENTER);
    }

    /** Dejo el cursor en la placa, listo para escribir o escanear (lo llamo al abrir esta pantalla). */
    public void enfocar() {
        SwingUtilities.invokeLater(campoPlaca::requestFocusInWindow);
    }

    public void refrescar() {
        comboVehiculo.removeAllItems();
        comboVehiculo.addItem(new VehiculoItem(null, "Ninguno"));
        for (Vehiculo v : parqueoService.obtenerActivos()) {
            comboVehiculo.addItem(new VehiculoItem(v.getId(), v.getPlaca() + " (" + v.getTipo() + ")"));
        }

        pendientes = lavadoService.obtenerPendientes();
        modeloPendientes.setRowCount(0);
        for (RegistroLavado r : pendientes) {
            modeloPendientes.addRow(new Object[]{
                    r.getId(), r.getPlaca(), r.getServicio().getNombre(), Formato.dinero(r.getPrecio()),
                    Formato.fechaHora(r.getFechaInicio()),
                    r.getNombreDueno() != null ? r.getNombreDueno() : "",
                    r.getTelefonoDueno() != null ? r.getTelefonoDueno() : ""
            });
        }
    }

    private void registrar() {
        try {
            ServicioLavado servicio = (ServicioLavado) comboServicio.getSelectedItem();
            VehiculoItem vehiculoItem = (VehiculoItem) comboVehiculo.getSelectedItem();
            Integer vehiculoId = vehiculoItem != null ? vehiculoItem.id : null;
            RegistroLavado registro = lavadoService.registrarLavado(campoPlaca.getText(), servicio, vehiculoId,
                    campoNombreDueno.getText(), campoTelefonoDueno.getText());
            campoPlaca.setText("");
            campoNombreDueno.setText("");
            campoTelefonoDueno.setText("");
            onCambio.run();

            ConfiguracionNegocio negocio = configuracionService.obtener();
            String texto = GeneradorRecibos.ticketPendiente(negocio, registro.getId(), registro.getPlaca(),
                    "Servicio : " + servicio.getNombre() + "\n", registro.getUsuarioInicio());
            new DialogoTicket(SwingUtilities.getWindowAncestor(this), "Ticket de lavado", texto, registro.getPlaca())
                    .setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completar() {
        int fila = tablaPendientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un lavado de la tabla");
            return;
        }
        RegistroLavado r = pendientes.get(fila);
        try {
            LavadoService.PrevisualizacionCompletar previsualizacion = lavadoService.previsualizarCompletar(r.getId());
            Integer montoPagado = DialogoPago.pedirMontoPagado(this, "Lavado de " + r.getPlaca(), r.getPrecio());
            if (montoPagado == null) {
                return;
            }
            LavadoService.ResultadoCompletar resultado = lavadoService.confirmarCompletar(previsualizacion, montoPagado);
            onCambio.run();

            ConfiguracionNegocio negocio = configuracionService.obtener();
            String recibo = GeneradorRecibos.reciboLavado(negocio, r.getId(), r.getPlaca(), r.getServicio().getNombre(),
                    r.getPrecio(), r.getFechaInicio(), resultado.fechaFin(), r.getNombreDueno(), r.getTelefonoDueno(),
                    resultado.cajero(), resultado.montoPagado(), resultado.vuelto());
            new DialogoRecibo(SwingUtilities.getWindowAncestor(this), "Recibo de lavado", recibo).setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record VehiculoItem(Integer id, String etiqueta) {
        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
