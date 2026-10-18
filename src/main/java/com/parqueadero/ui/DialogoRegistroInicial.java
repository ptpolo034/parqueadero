package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.util.IconoApp;

import javax.swing.*;
import java.awt.*;

/**
 * Se muestra solo la primera vez que se ejecuta la aplicación (cuando aún no
 * existe ningún usuario registrado), para crear la cuenta de administrador.
 */
public class DialogoRegistroInicial extends JDialog {
    private final AuthService authService;
    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoContrasena = new JPasswordField();
    private final JPasswordField campoConfirmar = new JPasswordField();

    public DialogoRegistroInicial(AuthService authService) {
        super((Frame) null, "Crear usuario administrador", true);
        this.authService = authService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (IconoApp.obtener() != null) {
            setIconImage(IconoApp.obtener());
        }

        campoContrasena.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        campoConfirmar.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");

        JButton boton = Estilos.primarioGrande(new JButton("Crear usuario"));
        getRootPane().setDefaultButton(boton);
        boton.addActionListener(e -> crear());

        Formulario formulario = new Formulario();
        formulario.agregar(Estilos.titulo("Crea tu cuenta de administrador"));
        formulario.agregar(Estilos.subtitulo("No hay usuarios registrados todavía."));
        formulario.campo("Usuario", campoUsuario);
        formulario.campo("Contraseña", campoContrasena);
        formulario.campo("Confirmar contraseña", campoConfirmar);
        formulario.agregar(boton);

        JPanel derecha = new JPanel(new GridBagLayout());
        derecha.setBorder(BorderFactory.createEmptyBorder(30, 44, 30, 44));
        derecha.add(formulario, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        PanelMarca marca = new PanelMarca("Bienvenido", "Crea el usuario administrador para empezar a usar el sistema.");
        marca.setPreferredSize(new Dimension(320, 480));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(marca, BorderLayout.WEST);
        raiz.add(derecha, BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(780, 500);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void crear() {
        String usuario = campoUsuario.getText();
        String contrasena = new String(campoContrasena.getPassword());
        String confirmar = new String(campoConfirmar.getPassword());
        if (!contrasena.equals(confirmar)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            authService.registrarAdministrador(usuario, contrasena);
            JOptionPane.showMessageDialog(this, "Usuario creado correctamente. Ahora puedes iniciar sesión.");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
