package com.parqueadero.ui;

import com.parqueadero.util.GeneradorCodigos;
import com.parqueadero.util.ImpresionUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Ticket que se entrega al cliente al registrar una entrada de parqueo o un lavado.
 * Incluye un código QR con la placa, para que al escanearlo en el mostrador de salida
 * el sistema encuentre y cierre automáticamente el parqueo y/o el lavado pendientes.
 */
public class DialogoTicket extends JDialog {

    public DialogoTicket(Window propietario, String titulo, String textoTicket, String placa) {
        super(propietario, titulo, ModalityType.APPLICATION_MODAL);

        // Panel imprimible: solo el texto y el QR, sin botones — es lo único que debe salir en el papel.
        JPanel panelImprimible = new JPanel(new BorderLayout(10, 10));
        panelImprimible.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea area = new JTextArea(textoTicket);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        panelImprimible.add(area, BorderLayout.NORTH);

        BufferedImage qr = GeneradorCodigos.generarQR(placa, 200);
        JLabel labelQR = new JLabel(new ImageIcon(qr));
        labelQR.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel panelQR = new JPanel(new BorderLayout());
        panelQR.add(labelQR, BorderLayout.CENTER);
        JLabel labelInstruccion = new JLabel("Conserva este código para la salida", SwingConstants.CENTER);
        panelQR.add(labelInstruccion, BorderLayout.SOUTH);
        panelImprimible.add(panelQR, BorderLayout.CENTER);

        JButton botonImprimir = new JButton("Imprimir ticket");
        JButton botonCerrar = new JButton("Cerrar");
        botonImprimir.addActionListener(e -> {
            try {
                ImpresionUtil.imprimirComponente(panelImprimible);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        botonCerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(botonImprimir);
        botones.add(botonCerrar);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.add(panelImprimible, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        setAlwaysOnTop(true);
        pack();
        setLocationRelativeTo(propietario);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            toFront();
            requestFocus();
        }
    }
}
