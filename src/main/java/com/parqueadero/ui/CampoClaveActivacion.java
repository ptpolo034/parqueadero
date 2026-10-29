package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Campo donde pego la clave de activación. Como la clave es una firma larga (unos 120
 * caracteres con guiones), uso un área de varias líneas con ajuste de línea para que se
 * vea completa y no se recorte. Acepto que la peguen con espacios, saltos de línea o minúsculas;
 * quien valida la clave (ValidadorLicencia) se encarga de limpiarla. Con Enter activo
 * directamente, igual que si fuera un campo de una sola línea.
 */
public class CampoClaveActivacion extends JScrollPane {
    private final JTextArea area = new JTextArea(4, 20);

    public CampoClaveActivacion(Runnable alPresionarEnter) {
        super(null, VERTICAL_SCROLLBAR_AS_NEEDED,
                HORIZONTAL_SCROLLBAR_NEVER);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Pega aquí el código de activación completo");
        area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activar");
        area.getActionMap().put("activar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alPresionarEnter.run();
            }
        });
        setViewportView(area);
        // Fijo la altura (4 líneas) para que el formulario no la comprima: el texto largo se ve completo y desplaza si hace falta
        setPreferredSize(new Dimension(200, 86));
        setMinimumSize(new Dimension(100, 86));
    }

    public String getTexto() {
        return area.getText();
    }

    public void setTexto(String texto) {
        area.setText(texto);
    }

    @Override
    public void setEnabled(boolean habilitado) {
        super.setEnabled(habilitado);
        area.setEnabled(habilitado);
    }

    public JTextArea area() {
        return area;
    }
}
