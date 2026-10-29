package com.parqueadero.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

public class DialogoRecibo extends JDialog {

    public DialogoRecibo(Window propietario, String titulo, String contenido) {
        super(propietario, titulo, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        JTextArea area = new JTextArea(contenido);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        // El recibo es un papel: blanco con letra negra en cualquier tema, así también sale bien al imprimir.
        area.setBackground(Color.WHITE);
        area.setForeground(Color.BLACK);
        area.setMargin(new Insets(10, 10, 10, 10));
        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton botonImprimir = new JButton("Imprimir");
        JButton botonCerrar = new JButton("Cerrar");
        botonImprimir.addActionListener(e -> {
            try {
                area.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        botonCerrar.addActionListener(e -> dispose());
        // Enter y Esc cierran: con el lector de códigos o el teclado el cajero sigue con el siguiente vehículo sin tocar el ratón.
        getRootPane().setDefaultButton(botonCerrar);
        Estilos.cerrarConEscape(this);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(botonImprimir);
        botones.add(botonCerrar);
        add(botones, BorderLayout.SOUTH);

        setSize(400, 420);
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
