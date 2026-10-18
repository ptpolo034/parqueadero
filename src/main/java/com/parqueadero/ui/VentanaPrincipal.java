package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.servicio.CajaService;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.LicenciaService;
import com.parqueadero.servicio.MensualidadService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.servicio.ReporteService;
import com.parqueadero.util.IconoApp;
import com.parqueadero.util.RecursosApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VentanaPrincipal extends JFrame {
    private static final int ANCHO_BARRA = 236;

    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final ReporteService reporteService = new ReporteService();
    private final AuthService authService = new AuthService();
    private final ConfiguracionService configuracionService = new ConfiguracionService();
    private final LicenciaService licenciaService;

    private final PanelInicio panelInicio;
    private final PanelEntrada panelEntrada;
    private final PanelEscaneo panelEscaneo;
    private final PanelLavado panelLavado;
    private final PanelHistorial panelHistorial;
    private final PanelReportes panelReportes;
    private final PanelTarifas panelTarifas;
    private final PanelCaja panelCaja;
    private final PanelMensualidades panelMensualidades;
    private final PanelLicencia panelLicencia;
    private PanelUsuarios panelUsuarios;
    private PanelConfiguracion panelConfiguracion;
    private final PanelAuditoria panelAuditoria = new PanelAuditoria();
    private final Map<String, Runnable> refrescadores = new HashMap<>();

    private final CardLayout cartas = new CardLayout();
    private final JPanel zonaContenido = new JPanel(cartas);
    private final ButtonGroup grupoNavegacion = new ButtonGroup();
    private final Map<String, JToggleButton> botones = new HashMap<>();
    private final Map<String, String> titulos = new HashMap<>();
    private final JLabel tituloPagina = Estilos.titulo("");
    private final JLabel fechaPagina = Estilos.subtitulo("");
    private final Pildora pildoraCupos = new Pildora("", Estilos.Tono.VERDE);
    private final Pildora pildoraLicencia = new Pildora("", Estilos.Tono.AMBAR);
    private final JButton botonTema = new JButton();
    private final JLabel labelLogo = new JLabel();
    private String paginaActual = "inicio";

    public VentanaPrincipal(Usuario usuarioActivo, LicenciaService licenciaService, Runnable alCerrarSesion) {
        super("Sistema de Parqueadero y Lavado");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 800);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);

        this.licenciaService = licenciaService;
        MensualidadService mensualidadService = new MensualidadService(usuarioActivo.getUsuario());
        parqueoService = new ParqueoService(usuarioActivo.getUsuario(), configuracionService, mensualidadService);
        lavadoService = new LavadoService(usuarioActivo.getUsuario());

        AccionesVehiculo acciones = new AccionesVehiculo(parqueoService, lavadoService, configuracionService,
                mensualidadService, authService, usuarioActivo, this::refrescarTodo);
        panelInicio = new PanelInicio(parqueoService, lavadoService, mensualidadService, reporteService, acciones);
        panelEntrada = new PanelEntrada(parqueoService, lavadoService, configuracionService, mensualidadService, this::refrescarTodo);
        panelEscaneo = new PanelEscaneo(acciones);
        panelLavado = new PanelLavado(lavadoService, parqueoService, configuracionService, this::refrescarTodo);
        panelHistorial = new PanelHistorial(parqueoService, lavadoService);
        panelReportes = new PanelReportes(reporteService);
        panelTarifas = new PanelTarifas(parqueoService, lavadoService, configuracionService, this::refrescarTodo);
        panelCaja = new PanelCaja(new CajaService(usuarioActivo.getUsuario()));
        panelMensualidades = new PanelMensualidades(mensualidadService, authService, usuarioActivo, this::refrescarTodo);
        panelLicencia = new PanelLicencia(licenciaService);

        actualizarLogo();

        JPanel barra = construirBarraLateral(usuarioActivo, alCerrarSesion);

        JPanel derecha = new JPanel(new BorderLayout());
        derecha.add(construirCabecera(), BorderLayout.NORTH);
        zonaContenido.setOpaque(false);
        zonaContenido.setBorder(BorderFactory.createEmptyBorder(0, 28, 24, 28));
        derecha.add(zonaContenido, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(barra, BorderLayout.WEST);
        raiz.add(derecha, BorderLayout.CENTER);
        setContentPane(raiz);

        agregarPagina("inicio", "Inicio", panelInicio, panelInicio::refrescar);
        agregarPagina("entrada", "Entrada de vehículos", panelEntrada, panelEntrada::refrescar);
        agregarPagina("salida", "Escanear salida", panelEscaneo, null);
        agregarPagina("lavado", "Lavado", panelLavado, panelLavado::refrescar);
        agregarPagina("mensualidades", "Mensualidades", panelMensualidades, panelMensualidades::refrescar);
        agregarPagina("caja", "Caja", panelCaja, panelCaja::refrescar);
        agregarPagina("historial", "Historial", panelHistorial, panelHistorial::refrescar);
        agregarPagina("reportes", "Reportes", panelReportes, panelReportes::refrescar);
        if (usuarioActivo.esAdmin()) {
            panelUsuarios = new PanelUsuarios(authService, usuarioActivo);
            panelConfiguracion = new PanelConfiguracion(configuracionService, usuarioActivo.getUsuario(), this::actualizarLogo);
            agregarPagina("tarifas", "Tarifas", panelTarifas, panelTarifas::refrescar);
            agregarPagina("usuarios", "Usuarios", panelUsuarios, panelUsuarios::refrescar);
            agregarPagina("auditoria", "Auditoría", panelAuditoria, panelAuditoria::refrescar);
            agregarPagina("configuracion", "Configuración", panelConfiguracion, panelConfiguracion::refrescar);
        }
        agregarPagina("licencia", "Licencia", panelLicencia, panelLicencia::refrescar);

        mostrar("inicio");
    }

    // ---------------------------------------------------------------- estructura

    private JPanel construirBarraLateral(Usuario usuario, Runnable alCerrarSesion) {
        JPanel barra = new PanelBarra();
        barra.setLayout(new BorderLayout());
        barra.setPreferredSize(new Dimension(UIScale.scale(ANCHO_BARRA), 0));

        // Marca
        JPanel marca = new JPanel(new BorderLayout(12, 0));
        marca.setOpaque(false);
        marca.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 16));
        marca.add(labelLogo, BorderLayout.WEST);
        JPanel nombres = new JPanel(new BorderLayout());
        nombres.setOpaque(false);
        JLabel nombre = new JLabel("Parqueadero");
        nombre.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        nombres.add(nombre, BorderLayout.CENTER);
        nombres.add(Estilos.subtitulo("Parqueo y lavado"), BorderLayout.SOUTH);
        marca.add(nombres, BorderLayout.CENTER);
        barra.add(marca, BorderLayout.NORTH);

        // Navegación
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        nav.add(seccion("Operación"));
        nav.add(botonNav("inicio", "Inicio"));
        nav.add(botonNav("entrada", "Entrada"));
        nav.add(botonNav("salida", "Escanear salida"));
        nav.add(botonNav("lavado", "Lavado"));
        nav.add(botonNav("mensualidades", "Mensualidades"));
        nav.add(botonNav("caja", "Caja"));
        nav.add(seccion("Consultas"));
        nav.add(botonNav("historial", "Historial"));
        nav.add(botonNav("reportes", "Reportes"));
        if (usuario.esAdmin()) {
            nav.add(seccion("Administración"));
            nav.add(botonNav("tarifas", "Tarifas"));
            nav.add(botonNav("usuarios", "Usuarios"));
            nav.add(botonNav("auditoria", "Auditoría"));
            nav.add(botonNav("configuracion", "Configuración"));
        }
        nav.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(nav, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        barra.add(scroll, BorderLayout.CENTER);

        // Pie: licencia + usuario
        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        pie.setBorder(BorderFactory.createEmptyBorder(0, 12, 14, 12));
        pie.add(botonNav("licencia", "Licencia"));
        pie.add(Box.createVerticalStrut(10));
        pie.add(construirChipUsuario(usuario, alCerrarSesion));
        barra.add(pie, BorderLayout.SOUTH);
        return barra;
    }

    private JPanel construirChipUsuario(Usuario usuario, Runnable alCerrarSesion) {
        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel avatar = new JLabel(Estilos.avatar(usuario.getUsuario(), 36));
        chip.add(avatar, BorderLayout.WEST);

        JPanel datos = new JPanel(new BorderLayout());
        datos.setOpaque(false);
        JLabel nombre = new JLabel(usuario.getUsuario());
        nombre.putClientProperty(FlatClientProperties.STYLE, "font:semibold");
        datos.add(nombre, BorderLayout.CENTER);
        String rol = String.valueOf(usuario.getRol());
        datos.add(Estilos.subtitulo(rol.isEmpty() ? rol : rol.substring(0, 1).toUpperCase() + rol.substring(1).toLowerCase()),
                BorderLayout.SOUTH);
        chip.add(datos, BorderLayout.CENTER);

        JButton salir = new JButton(Iconos.neutro("salir", 18));
        salir.setToolTipText("Cerrar sesión");
        salir.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        salir.addActionListener(e -> cerrarSesion(alCerrarSesion));
        chip.add(salir, BorderLayout.EAST);
        return chip;
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout(16, 0));
        cabecera.setOpaque(false);
        cabecera.setBorder(BorderFactory.createEmptyBorder(22, 28, 18, 28));

        JPanel titulos = new JPanel(new BorderLayout());
        titulos.setOpaque(false);
        titulos.add(tituloPagina, BorderLayout.CENTER);
        titulos.add(fechaPagina, BorderLayout.SOUTH);
        cabecera.add(titulos, BorderLayout.CENTER);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(pildoraLicencia);
        derecha.add(pildoraCupos);

        botonTema.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        botonTema.addActionListener(e -> {
            Tema.alternar();
            actualizarBotonTema();
            refrescarTodo();
        });
        actualizarBotonTema();
        derecha.add(botonTema);
        cabecera.add(derecha, BorderLayout.EAST);
        return cabecera;
    }

    private void actualizarBotonTema() {
        botonTema.setIcon(Iconos.neutro(Tema.esOscuro() ? "sol" : "luna", 20));
        botonTema.setToolTipText(Tema.esOscuro() ? "Cambiar a tema claro" : "Cambiar a tema oscuro");
    }

    private JLabel seccion(String texto) {
        JLabel l = Estilos.seccion(texto);
        l.setBorder(BorderFactory.createEmptyBorder(12, 10, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JToggleButton botonNav(String clave, String texto) {
        JToggleButton b = new JToggleButton(texto);
        b.setIcon(new IconoNavegacion(clave));
        b.setIconTextGap(12);
        b.setHorizontalAlignment(JToggleButton.LEFT);
        b.setFocusable(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(36)));
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        b.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; margin:7,12,7,12; toolbar.margin:7,12,7,12; toolbar.spacingInsets:1,0,1,0;"
                        + "selectedForeground:$Component.accentColor;"
                        + "toolbar.selectedBackground:fade($Component.accentColor,14%);"
                        + "toolbar.hoverBackground:fade($Component.accentColor,7%);"
                        + "toolbar.pressedBackground:fade($Component.accentColor,10%);"
                        + "font:semibold");
        b.addActionListener(e -> mostrar(clave));
        grupoNavegacion.add(b);
        botones.put(clave, b);
        return b;
    }

    private void agregarPagina(String clave, String titulo, JComponent panel, Runnable refrescar) {
        if (refrescar != null) {
            refrescadores.put(clave, refrescar);
        }
        titulos.put(clave, titulo);
        panel.setOpaque(false);
        zonaContenido.add(panel, clave);
    }

    // ---------------------------------------------------------------- comportamiento

    /** Muestra una página por su clave (también la usan los atajos de la pantalla de inicio). */
    private void mostrar(String clave) {
        if (!titulos.containsKey(clave)) {
            return;
        }
        paginaActual = clave;
        cartas.show(zonaContenido, clave);
        JToggleButton b = botones.get(clave);
        if (b != null) {
            b.setSelected(true);
        }
        tituloPagina.setText(titulos.get(clave));
        refrescarTodo();
        if (clave.equals("salida")) {
            panelEscaneo.enfocar();
        } else if (clave.equals("inicio")) {
            panelInicio.enfocar();
        }
    }

    private void cerrarSesion(Runnable alCerrarSesion) {
        int confirmar = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }
        dispose();
        alCerrarSesion.run();
    }

    private void actualizarLogo() {
        ImageIcon logo = RecursosApp.cargarLogo();
        Image imagen = logo != null ? logo.getImage() : IconoApp.obtener();
        if (imagen != null) {
            setIconImage(imagen);
            labelLogo.setIcon(new ImageIcon(imagen.getScaledInstance(UIScale.scale(40), UIScale.scale(40), Image.SCALE_SMOOTH)));
        } else {
            labelLogo.setIcon(null);
        }
    }

    /** Actualiza la cabecera y solo la pantalla que se está viendo (las demás se actualizan al abrirlas). */
    private void refrescarTodo() {
        fechaPagina.setText(capitalizar(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "CO")))));
        actualizarCupos();
        actualizarPildoraLicencia();
        Runnable refrescarPagina = refrescadores.get(paginaActual);
        if (refrescarPagina != null) {
            refrescarPagina.run();
        }
    }

    private void actualizarCupos() {
        int total = parqueoService.cuposTotales();
        int libres = parqueoService.cuposDisponibles();
        double ocupacion = total == 0 ? 0 : (double) (total - libres) / total;
        Estilos.Tono tono = ocupacion >= 0.9 ? Estilos.Tono.ROJO : ocupacion >= 0.7 ? Estilos.Tono.AMBAR : Estilos.Tono.VERDE;
        pildoraCupos.establecer("Cupos libres: " + libres + " / " + total, tono);
    }

    private void actualizarPildoraLicencia() {
        if (licenciaService.estaActivada()) {
            pildoraLicencia.setVisible(false);
        } else {
            int dias = licenciaService.diasRestantesPrueba();
            pildoraLicencia.establecer("Prueba: quedan " + dias + " día(s)", dias <= 5 ? Estilos.Tono.ROJO : Estilos.Tono.AMBAR);
            pildoraLicencia.setVisible(true);
        }
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------------------------------------------------------------- piezas visuales

    /** Fondo de la barra lateral: color de tarjeta y una línea fina a la derecha. */
    private static class PanelBarra extends JPanel {
        PanelBarra() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Tema.fondoTarjeta());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Tema.borde());
            g.fillRect(getWidth() - 1, 0, 1, getHeight());
            super.paintComponent(g);
        }
    }

    /** Ícono del menú: gris en reposo y del color de acento cuando la opción está seleccionada. */
    private static class IconoNavegacion implements Icon {
        private final FlatSVGIcon reposo;
        private final FlatSVGIcon activo;

        IconoNavegacion(String nombre) {
            reposo = Iconos.neutro(nombre, 20);
            activo = new FlatSVGIcon("icons/" + nombre + ".svg", 20, 20);
            activo.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Tema.acento()));
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean seleccionado = c instanceof AbstractButton b && b.isSelected();
            (seleccionado ? activo : reposo).paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return reposo.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return reposo.getIconHeight();
        }
    }
}
