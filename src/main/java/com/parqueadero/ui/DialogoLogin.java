package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuditoriaService;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.util.IconoApp;

import javax.swing.*;
import java.awt.*;

public class DialogoLogin extends JDialog {
    private final AuthService authService;
    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoContrasena = new JPasswordField();
    private Usuario usuarioAutenticado;
    private final JButton botonIngresar = Estilos.primarioGrande(new JButton("Ingresar"));
    private int intentosFallidos = 0;

    public DialogoLogin(AuthService authService) {
        super((Frame) null, "Iniciar sesión - Parqueadero y Lavado", true);
        this.authService = authService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (IconoApp.obtener() != null) {
            setIconImage(IconoApp.obtener());
        }

        campoUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tu usuario");
        campoUsuario.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.neutro("usuarios", 18));
        campoUsuario.putClientProperty(FlatClientProperties.STYLE, "margin:6,10,6,10");
        campoContrasena.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tu contraseña");
        campoContrasena.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true; margin:6,10,6,10");


        getRootPane().setDefaultButton(botonIngresar);
        botonIngresar.addActionListener(e -> intentarIngresar());

        JLabel bienvenida = Estilos.titulo("Bienvenido");
        bienvenida.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");

        Formulario formulario = new Formulario();
        formulario.agregar(bienvenida);
        formulario.agregar(Estilos.subtitulo("Ingresa tus datos para continuar"));
        formulario.campo("Usuario", campoUsuario);
        formulario.campo("Contraseña", campoContrasena);
        formulario.agregar(botonIngresar);

        JLabel labelFirma = new JLabel("Desarrollado por Henry Polo", SwingConstants.CENTER);
        labelFirma.setForeground(Tema.textoSecundario());
        labelFirma.putClientProperty(FlatClientProperties.STYLE, "font:-2");

        JPanel derecha = new JPanel(new BorderLayout(0, 18));
        derecha.setBorder(BorderFactory.createEmptyBorder(36, 44, 22, 44));
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(formulario, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        derecha.add(centro, BorderLayout.CENTER);
        derecha.add(labelFirma, BorderLayout.SOUTH);

        PanelMarca marca = new PanelMarca("Parqueadero<br>y Lavado", "Controla entradas, salidas, caja y lavados en un solo lugar.");
        marca.setPreferredSize(new Dimension(320, 440));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(marca, BorderLayout.WEST);
        raiz.add(derecha, BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(780, 460);
        setResizable(false);
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(campoUsuario::requestFocusInWindow);
    }

    private void intentarIngresar() {
        String usuario = campoUsuario.getText();
        String contrasena = new String(campoContrasena.getPassword());
        Usuario u = authService.autenticar(usuario, contrasena);
        if (u != null) {
            usuarioAutenticado = u;
            dispose();
        } else {
            new AuditoriaService().registrar(usuario.trim(), "LOGIN_FALLIDO", "Contraseña o usuario incorrectos");
            intentosFallidos++;
            if (intentosFallidos >= 5) {
                bloquearTemporalmente();
            }
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos", "Error", JOptionPane.ERROR_MESSAGE);
            campoContrasena.setText("");
        }
    }

    /** Tras 5 intentos fallidos seguidos bloqueo el botón 30 segundos antes de dejar intentar de nuevo. */
    private void bloquearTemporalmente() {
        botonIngresar.setEnabled(false);
        botonIngresar.setText("Espera 30 s...");
        Timer timer = new Timer(30_000, e -> {
            botonIngresar.setEnabled(true);
            botonIngresar.setText("Ingresar");
            intentosFallidos = 0;
        });
        timer.setRepeats(false);
        timer.start();
    }

    public boolean isAutenticado() {
        return usuarioAutenticado != null;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
