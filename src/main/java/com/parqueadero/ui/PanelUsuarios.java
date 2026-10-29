package com.parqueadero.ui;

import com.parqueadero.modelo.RolUsuario;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuditoriaService;
import com.parqueadero.servicio.AuthService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PanelUsuarios extends JPanel {
    private final AuthService authService;
    private final Usuario usuarioActual;

    private final JTextField campoUsuario = new JTextField(15);
    private final JPasswordField campoContrasena = new JPasswordField(15);
    private final JPasswordField campoConfirmar = new JPasswordField(15);
    private final JComboBox<RolUsuario> comboRol = new JComboBox<>(RolUsuario.values());

    private final DefaultTableModel modeloUsuarios;
    private final JTable tablaUsuarios;
    private final JComboBox<RolUsuario> comboNuevoRol = new JComboBox<>(RolUsuario.values());
    private List<Usuario> usuarios = List.of();

    public PanelUsuarios(AuthService authService, Usuario usuarioActual) {
        this.authService = authService;
        this.usuarioActual = usuarioActual;

        setLayout(new BorderLayout(16, 0));
        setOpaque(false);

        JButton botonCrear = Estilos.primario(new JButton("Crear usuario", Iconos.color("mas", 16, Color.WHITE)));
        botonCrear.addActionListener(e -> crear());

        Formulario formulario = new Formulario();
        formulario.campo("Nombre de usuario", campoUsuario);
        formulario.campo("Contraseña", campoContrasena);
        formulario.campo("Confirmar contraseña", campoConfirmar);
        formulario.campo("Rol", comboRol);
        formulario.agregar(botonCrear);

        JPanel envoltorioForm = new JPanel(new BorderLayout());
        envoltorioForm.setOpaque(false);
        envoltorioForm.add(Tarjeta.conTitulo("Crear usuario", "Rol ADMIN o CAJERO", formulario),
                BorderLayout.NORTH);
        envoltorioForm.setPreferredSize(new Dimension(380, 0));
        add(envoltorioForm, BorderLayout.WEST);

        modeloUsuarios = new DefaultTableModel(new Object[]{"ID", "Usuario", "Rol"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaUsuarios = Tablas.crear(modeloUsuarios, "Aún no hay usuarios");
        JScrollPane scrollTabla = new JScrollPane(tablaUsuarios);
        scrollTabla.setBorder(BorderFactory.createEmptyBorder());

        JButton botonCambiarRol = new JButton("Cambiar rol");
        JButton botonRenombrar = new JButton("Renombrar...");
        JButton botonRestablecer = new JButton("Nueva contraseña...");
        JButton botonEliminar = Estilos.peligro(new JButton("Eliminar"));
        // Uso dos filas de botones: en una sola no caben en ventanas de 1120 de ancho y los últimos quedaban ocultos.
        JPanel filaRol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaRol.setOpaque(false);
        filaRol.add(comboNuevoRol);
        filaRol.add(botonCambiarRol);
        filaRol.add(botonRenombrar);
        JPanel filaCuenta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaCuenta.setOpaque(false);
        filaCuenta.add(botonRestablecer);
        filaCuenta.add(botonEliminar);
        JPanel acciones = new JPanel(new GridLayout(2, 1, 0, 8));
        acciones.setOpaque(false);
        acciones.add(filaRol);
        acciones.add(filaCuenta);

        botonCambiarRol.addActionListener(e -> cambiarRol());
        botonRenombrar.addActionListener(e -> renombrar());
        botonRestablecer.addActionListener(e -> restablecerContrasena());
        botonEliminar.addActionListener(e -> eliminar());

        JPanel cuerpoTabla = new JPanel(new BorderLayout(0, 12));
        cuerpoTabla.setOpaque(false);
        cuerpoTabla.add(scrollTabla, BorderLayout.CENTER);
        cuerpoTabla.add(acciones, BorderLayout.SOUTH);
        add(Tarjeta.conTitulo("Usuarios existentes", "Selecciona uno para cambiarle el rol, renombrarlo o eliminarlo",
                cuerpoTabla), BorderLayout.CENTER);

        refrescar();
    }

    public void refrescar() {
        usuarios = authService.listarUsuarios();
        modeloUsuarios.setRowCount(0);
        for (Usuario u : usuarios) {
            modeloUsuarios.addRow(new Object[]{u.getId(), u.getUsuario(), u.getRol()});
        }
    }

    private void auditar(String accion, String detalle) {
        new AuditoriaService().registrar(usuarioActual.getUsuario(), accion, detalle);
    }

    private Usuario seleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla");
            return null;
        }
        return usuarios.get(fila);
    }

    private void crear() {
        String contrasena = new String(campoContrasena.getPassword());
        String confirmar = new String(campoConfirmar.getPassword());
        if (!contrasena.equals(confirmar)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            RolUsuario rol = (RolUsuario) comboRol.getSelectedItem();
            authService.registrar(campoUsuario.getText(), contrasena, rol);
            auditar("USUARIO_CREADO", campoUsuario.getText().trim() + " (" + rol + ")");
            JOptionPane.showMessageDialog(this, "Usuario creado correctamente");
            campoUsuario.setText("");
            campoContrasena.setText("");
            campoConfirmar.setText("");
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarRol() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        try {
            authService.cambiarRol(u.getId(), (RolUsuario) comboNuevoRol.getSelectedItem());
            auditar("USUARIO_ROL_CAMBIADO", u.getUsuario() + ": " + u.getRol() + " → " + comboNuevoRol.getSelectedItem());
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renombrar() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        String nuevo = JOptionPane.showInputDialog(this, "Nuevo nombre de usuario:", u.getUsuario());
        if (nuevo == null) {
            return;
        }
        try {
            authService.renombrarUsuario(u.getId(), nuevo);
            auditar("USUARIO_RENOMBRADO", u.getUsuario() + " → " + nuevo.trim());
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restablecerContrasena() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        JPasswordField nueva = new JPasswordField();
        JPasswordField confirmar = new JPasswordField();
        Object[] mensaje = {"Nueva contraseña:", nueva, "Confirmar contraseña:", confirmar};
        int resultado = JOptionPane.showConfirmDialog(this, mensaje,
                "Restablecer contraseña de " + u.getUsuario(), JOptionPane.OK_CANCEL_OPTION);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }
        String c1 = new String(nueva.getPassword());
        String c2 = new String(confirmar.getPassword());
        if (!c1.equals(c2)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            authService.restablecerContrasena(u.getId(), c1);
            auditar("USUARIO_CONTRASENA_RESTABLECIDA", u.getUsuario());
            JOptionPane.showMessageDialog(this, "Contraseña actualizada correctamente");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        Usuario u = seleccionado();
        if (u == null) {
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Eliminar al usuario \"" + u.getUsuario() + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            authService.eliminarUsuario(u.getId(), usuarioActual.getId());
            auditar("USUARIO_ELIMINADO", u.getUsuario() + " (" + u.getRol() + ")");
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
