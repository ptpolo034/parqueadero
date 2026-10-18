package com.parqueadero.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Formulario de una columna: cada campo lleva su etiqueta encima, con espacios uniformes. */
public class Formulario extends JPanel {
    private final GridBagConstraints gbc = new GridBagConstraints();
    private boolean primero = true;

    public Formulario() {
        super(new GridBagLayout());
        setOpaque(false);
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
    }

    /** Agrega un campo con su etiqueta; devuelve la etiqueta (por si se necesita habilitarla o ocultarla). */
    public JLabel campo(String etiqueta, JComponent campo) {
        JLabel l = new JLabel(etiqueta);
        l.setForeground(Tema.textoSecundario());
        gbc.insets = new Insets(primero ? 0 : 14, 0, 4, 0);
        add(l, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        add(campo, gbc);
        primero = false;
        return l;
    }

    /** Agrega un componente sin etiqueta (botones, casillas, textos). */
    public void agregar(Component c) {
        gbc.insets = new Insets(primero ? 0 : 14, 0, 0, 0);
        add(c, gbc);
        primero = false;
    }
}
