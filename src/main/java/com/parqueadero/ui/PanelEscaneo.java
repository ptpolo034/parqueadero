package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.util.FiltroMayusculas;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Aquí reúno la salida rápida: el cajero (o un lector de código de barras/QR conectado como teclado) escribe o
 * escanea la placa y presiona Enter. Busco el parqueo activo y/o los lavados pendientes de esa placa y los
 * cierro uno por uno, pidiendo el pago de cada uno e imprimiendo su recibo. (Desde la pantalla de Inicio puedo
 * hacer lo mismo y además elegir otras acciones.)
 *
 * También dejo botones para editar o cancelar la entrada activa de la placa escrita en el campo. Los ve
 * cualquier usuario, pero si quien los usa es un cajero, pido que un administrador autorice.
 */
public class PanelEscaneo extends JPanel {
    private final AccionesVehiculo acciones;
    private final JTextField campoEscaneo;
    private final DefaultListModel<String> modeloRegistro = new DefaultListModel<>();

    public PanelEscaneo(AccionesVehiculo acciones) {
        this.acciones = acciones;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        campoEscaneo = new JTextField();
        FiltroMayusculas.aplicar(campoEscaneo);
        campoEscaneo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Escanea o escribe la placa y presiona Enter");
        campoEscaneo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; margin:10,16,10,16; arc:14");
        campoEscaneo.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.neutro("salida", 22));
        campoEscaneo.addActionListener(e -> procesarEscaneo());

        JButton botonEditar = new JButton("Editar entrada de esta placa...");
        JButton botonCancelar = new JButton("Cancelar entrada de esta placa");
        botonEditar.addActionListener(e -> editarEntrada());
        botonCancelar.addActionListener(e -> cancelarEntrada());
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelAcciones.setOpaque(false);
        panelAcciones.add(botonEditar);
        panelAcciones.add(Box.createHorizontalStrut(10));
        panelAcciones.add(botonCancelar);

        JPanel contenidoSuperior = new JPanel(new BorderLayout(0, 12));
        contenidoSuperior.setOpaque(false);
        contenidoSuperior.add(campoEscaneo, BorderLayout.NORTH);
        contenidoSuperior.add(panelAcciones, BorderLayout.CENTER);
        add(Tarjeta.conTitulo("Salida de vehículo",
                "Con el lector de códigos el cursor ya queda listo: solo escanea el ticket", contenidoSuperior),
                BorderLayout.NORTH);

        JList<String> listaRegistro = new JList<>(modeloRegistro);
        listaRegistro.setFixedCellHeight(34);
        listaRegistro.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        listaRegistro.setOpaque(false);
        JScrollPane scrollRegistro = new JScrollPane(listaRegistro);
        scrollRegistro.setBorder(BorderFactory.createEmptyBorder());
        scrollRegistro.setOpaque(false);
        scrollRegistro.getViewport().setOpaque(false);
        add(Tarjeta.conTitulo("Actividad reciente", "Lo último que se cobró o canceló en esta sesión", scrollRegistro),
                BorderLayout.CENTER);

        SwingUtilities.invokeLater(campoEscaneo::requestFocusInWindow);
    }

    /** Lo llamo cada vez que se muestra esta pestaña, para que el cursor quede listo para escanear. */
    public void enfocar() {
        campoEscaneo.requestFocusInWindow();
    }

    private void procesarEscaneo() {
        String placa = campoEscaneo.getText().trim();
        campoEscaneo.setText("");
        if (placa.isEmpty()) {
            return;
        }
        acciones.cobrarSalida(this, placa, this::agregarAlRegistro);
        campoEscaneo.requestFocusInWindow();
    }

    private Vehiculo buscarVehiculoDelCampo() {
        String placa = campoEscaneo.getText().trim();
        if (placa.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe o escanea primero la placa del vehículo");
            return null;
        }
        Vehiculo vehiculo = acciones.parqueo().obtenerActivoPorPlaca(placa);
        if (vehiculo == null) {
            JOptionPane.showMessageDialog(this, "No hay un vehículo activo con la placa " + placa);
        }
        return vehiculo;
    }

    private void editarEntrada() {
        Vehiculo v = buscarVehiculoDelCampo();
        if (v != null) {
            acciones.editarEntrada(this, v);
            campoEscaneo.setText("");
            campoEscaneo.requestFocusInWindow();
        }
    }

    private void cancelarEntrada() {
        Vehiculo v = buscarVehiculoDelCampo();
        if (v != null) {
            acciones.cancelarEntrada(this, v);
            campoEscaneo.setText("");
            campoEscaneo.requestFocusInWindow();
        }
    }

    private void agregarAlRegistro(String texto) {
        String hora = DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalDateTime.now());
        modeloRegistro.add(0, "[" + hora + "] " + texto);
    }
}
