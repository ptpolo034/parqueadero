package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.servicio.LicenciaService;

import javax.swing.*;
import java.awt.*;

/**
 * Pide el código de activación. Si {@code bloqueante} es true (la prueba de 30 días ya
 * venció), no deja continuar sin activar o salir de la aplicación; si es false, es solo
 * un recordatorio que se puede cerrar sin activar (se usa antes de que venza la prueba).
 */
public class DialogoActivacion extends JDialog {
    private final LicenciaService licenciaService;
    private final JTextField campoClave = new JTextField(20);
    private boolean activada;

    public DialogoActivacion(Window propietario, LicenciaService licenciaService, boolean bloqueante) {
        super(propietario, "Activación del software", ModalityType.APPLICATION_MODAL);
        this.licenciaService = licenciaService;
        setDefaultCloseOperation(bloqueante ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);

        String mensaje = bloqueante
                ? "Tu período de prueba de " + LicenciaService.DIAS_PRUEBA + " días terminó. "
                + "Ingresa tu código de activación para seguir usando el software."
                : "Estás usando la versión de prueba (quedan " + licenciaService.diasRestantesPrueba()
                + " días). Si ya tienes tu código de activación, ingrésalo aquí.";

        JTextField campoId = new JTextField(licenciaService.idInstalacion());
        campoId.setEditable(false);
        campoId.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        campoClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "XXXX-XXXX-XXXX-XXXX");

        JButton botonActivar = Estilos.primarioGrande(new JButton("Activar"));
        JButton botonSalir = new JButton(bloqueante ? "Salir" : "Cerrar");
        botonActivar.addActionListener(e -> activar());
        botonSalir.addActionListener(e -> {
            if (bloqueante) {
                System.exit(0);
            } else {
                dispose();
            }
        });
        getRootPane().setDefaultButton(botonActivar);

        JPanel botones = new JPanel(new GridLayout(1, 2, 10, 0));
        botones.setOpaque(false);
        botones.add(botonSalir);
        botones.add(botonActivar);

        JLabel texto = new JLabel("<html><body style='width:300px'>" + mensaje + "</body></html>");
        Formulario formulario = new Formulario();
        formulario.agregar(Estilos.titulo(bloqueante ? "Activa tu licencia" : "Activación"));
        formulario.agregar(texto);
        formulario.campo("ID de instalación", campoId);
        formulario.agregar(Estilos.subtitulo("Envíale este ID al vendedor para que te dé tu código."));
        formulario.campo("Código de activación", campoClave);
        formulario.agregar(botones);

        JPanel derecha = new JPanel(new GridBagLayout());
        derecha.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        derecha.add(formulario, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        PanelMarca marca = new PanelMarca("Un último paso",
                "Con tu código de activación el software queda habilitado de forma permanente.");
        marca.setPreferredSize(new Dimension(300, 470));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(marca, BorderLayout.WEST);
        raiz.add(derecha, BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(780, 500);
        setResizable(false);
        setLocationRelativeTo(propietario);
    }

    private void activar() {
        try {
            licenciaService.activar(campoClave.getText());
            activada = true;
            JOptionPane.showMessageDialog(this, "¡Software activado correctamente! Gracias por tu compra.");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isActivada() {
        return activada;
    }
}
