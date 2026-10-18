package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuditoriaService;
import com.parqueadero.servicio.AuthService;

import javax.swing.*;
import java.awt.*;

/**
 * Pide usuario y contraseña de un administrador para autorizar una acción sensible
 * (editar, cancelar o eliminar algo) cuando quien está usando el sistema es un cajero.
 * Un administrador que ya tiene la sesión iniciada no necesita volver a autenticarse:
 * usa {@link #solicitarAutorizacion} en vez de construir el diálogo directamente.
 */
public class DialogoAutorizacionAdmin extends JDialog {
    private final AuthService authService;
    private final String solicitante;
    private final String motivo;
    private final JTextField campoUsuario = new JTextField(15);
    private final JPasswordField campoContrasena = new JPasswordField(15);
    private String autorizadoPor = null;

    /**
     * @return el nombre del administrador que autoriza la acción (el propio usuario activo si ya es admin),
     * o {@code null} si se canceló o las credenciales no correspondían a un administrador.
     */
    public static String solicitarAutorizacion(Window propietario, AuthService authService,
                                               Usuario usuarioActivo, String motivo) {
        if (usuarioActivo.esAdmin()) {
            return usuarioActivo.getUsuario();
        }
        DialogoAutorizacionAdmin dialogo = new DialogoAutorizacionAdmin(propietario, authService,
                usuarioActivo.getUsuario(), motivo);
        dialogo.setVisible(true);
        return dialogo.autorizadoPor;
    }

    private DialogoAutorizacionAdmin(Window propietario, AuthService authService, String solicitante, String motivo) {
        super(propietario, "Autorización de administrador requerida", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.solicitante = solicitante;
        this.motivo = motivo;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        campoUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Usuario del administrador");
        campoContrasena.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        JButton botonAutorizar = Estilos.primario(new JButton("Autorizar"));
        JButton botonCancelar = new JButton("Cancelar");
        getRootPane().setDefaultButton(botonAutorizar);
        botonAutorizar.addActionListener(e -> intentar());
        botonCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new GridLayout(1, 2, 10, 0));
        botones.setOpaque(false);
        botones.add(botonCancelar);
        botones.add(botonAutorizar);

        Formulario formulario = new Formulario();
        formulario.agregar(Estilos.titulo("Autorización requerida"));
        formulario.agregar(new JLabel("<html><body style='width:280px'><b>" + motivo
                + "</b><br>Un administrador debe autorizar esta acción.</body></html>"));
        formulario.campo("Usuario administrador", campoUsuario);
        formulario.campo("Contraseña", campoContrasena);
        formulario.agregar(botones);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        contenido.add(formulario, BorderLayout.CENTER);
        setContentPane(contenido);
        pack();
        setMinimumSize(new Dimension(380, getHeight()));
        setLocationRelativeTo(propietario);
    }

    private void intentar() {
        String adminIngresado = campoUsuario.getText().trim();
        Usuario u = authService.autenticar(adminIngresado, new String(campoContrasena.getPassword()));
        if (u != null && u.esAdmin()) {
            autorizadoPor = u.getUsuario();
            dispose();
        } else {
            new AuditoriaService().registrar(solicitante, "AUTORIZACION_FALLIDA",
                    "Intento de autorizar «" + motivo + "» con el usuario «" + adminIngresado + "»");
            JOptionPane.showMessageDialog(this, "Credenciales inválidas o el usuario no es administrador",
                    "Error", JOptionPane.ERROR_MESSAGE);
            campoContrasena.setText("");
        }
    }
}
