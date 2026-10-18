package com.parqueadero;

import com.parqueadero.dao.ConexionBD;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.servicio.LicenciaService;
import com.parqueadero.servicio.RespaldoService;
import com.parqueadero.ui.DialogoActivacion;
import com.parqueadero.ui.DialogoLogin;
import com.parqueadero.ui.DialogoRegistroInicial;
import com.parqueadero.ui.VentanaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.parqueadero.ui.Tema;

public class Main {
    public static void main(String[] args) {
        aplicarAparienciaModerna();
        ConexionBD.inicializar();
        try {
            new RespaldoService().respaldarSiNoExisteHoy();
        } catch (Exception e) {
            // No se detiene el arranque de la aplicación si el respaldo automático falla
            // (por ejemplo, por falta de permisos de escritura); solo se avisa al usuario.
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "No se pudo generar el respaldo automático de la base de datos: " + e.getMessage(),
                    "Aviso", JOptionPane.WARNING_MESSAGE));
        }
        SwingUtilities.invokeLater(Main::iniciar);
    }

    private static void aplicarAparienciaModerna() {
        try {
            Tema.instalar();
        } catch (Exception e) {
            // Si algo falla, seguimos con la apariencia por defecto de Swing en vez de bloquear el arranque.
            e.printStackTrace();
        }
    }

    private static void iniciar() {
        LicenciaService licenciaService = new LicenciaService();
        licenciaService.verificarAlIniciar();

        if (licenciaService.estaBloqueado()) {
            new DialogoActivacion(null, licenciaService, true).setVisible(true);
            // Si llega aquí es porque se activó correctamente (si no, "Salir" ya cerró la app).
        }

        AuthService authService = new AuthService();

        if (!authService.existeAlMenosUnUsuario()) {
            new DialogoRegistroInicial(authService).setVisible(true);
            if (!authService.existeAlMenosUnUsuario()) {
                System.exit(0);
                return;
            }
        }

        mostrarLogin(authService, licenciaService);
    }

    private static void mostrarLogin(AuthService authService, LicenciaService licenciaService) {
        DialogoLogin login = new DialogoLogin(authService);
        login.setVisible(true);
        if (!login.isAutenticado()) {
            System.exit(0);
            return;
        }

        Usuario usuario = login.getUsuarioAutenticado();
        new com.parqueadero.servicio.AuditoriaService().registrar(usuario.getUsuario(), "INICIO_SESION", "");
        VentanaPrincipal ventana = new VentanaPrincipal(usuario, licenciaService, () -> mostrarLogin(authService, licenciaService));
        ventana.setVisible(true);
    }
}
