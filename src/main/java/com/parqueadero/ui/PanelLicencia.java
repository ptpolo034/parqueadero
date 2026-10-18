package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.servicio.LicenciaService;

import javax.swing.*;
import java.awt.*;

public class PanelLicencia extends JPanel {
    private final LicenciaService licenciaService;
    private final Pildora pildoraEstado = new Pildora("", Estilos.Tono.AMBAR);
    private final JTextField campoId = new JTextField(20);
    private final JTextField campoClave = new JTextField(20);
    private final JButton botonActivar = Estilos.primario(new JButton("Activar"));

    public PanelLicencia(LicenciaService licenciaService) {
        this.licenciaService = licenciaService;

        setLayout(new BorderLayout());
        setOpaque(false);

        campoId.setEditable(false);
        campoId.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        campoClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "XXXX-XXXX-XXXX-XXXX");
        botonActivar.addActionListener(e -> activar());

        JPanel filaEstado = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaEstado.setOpaque(false);
        filaEstado.add(pildoraEstado);

        Formulario form = new Formulario();
        form.agregar(filaEstado);
        form.campo("ID de instalación", campoId);
        form.agregar(Estilos.subtitulo("Envía este ID al vendedor del software para que te entregue tu código de activación."));
        form.campo("Código de activación", campoClave);
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaBoton.setOpaque(false);
        filaBoton.add(botonActivar);
        form.agregar(filaBoton);

        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(Tarjeta.conTitulo("Licencia del software", "Estado de tu licencia y activación", form),
                BorderLayout.NORTH);
        envoltorio.setPreferredSize(new Dimension(560, 0));
        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setOpaque(false);
        izquierda.add(envoltorio, BorderLayout.WEST);
        add(izquierda, BorderLayout.CENTER);

        refrescar();
    }

    public void refrescar() {
        campoId.setText(licenciaService.idInstalacion());
        if (licenciaService.estaActivada()) {
            pildoraEstado.establecer("Activado permanentemente", Estilos.Tono.VERDE);
            campoClave.setEnabled(false);
            botonActivar.setEnabled(false);
        } else {
            int dias = licenciaService.diasRestantesPrueba();
            pildoraEstado.establecer("Versión de prueba — quedan " + dias + " día(s)",
                    dias <= 5 ? Estilos.Tono.ROJO : Estilos.Tono.AMBAR);
            campoClave.setEnabled(true);
            botonActivar.setEnabled(true);
        }
    }

    private void activar() {
        try {
            licenciaService.activar(campoClave.getText());
            JOptionPane.showMessageDialog(this, "¡Software activado correctamente! Gracias por tu compra.");
            campoClave.setText("");
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
