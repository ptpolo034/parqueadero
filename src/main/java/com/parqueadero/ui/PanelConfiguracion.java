package com.parqueadero.ui;

import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.RespaldoService;
import com.parqueadero.util.RecursosApp;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PanelConfiguracion extends JPanel {
    private final ConfiguracionService configuracionService;
    private final RespaldoService respaldoService = new RespaldoService();
    private final Runnable onCambioLogo;
    private final String usuarioActual;

    private final JTextField campoNombreNegocio = new JTextField(25);
    private final JTextField campoNit = new JTextField(15);
    private final JTextField campoDireccion = new JTextField(25);
    private final JTextField campoTelefono = new JTextField(15);
    private final JTextField campoRegimenTributario = new JTextField(25);
    private final JTextArea campoAvisoLegal = new JTextArea(3, 25);
    private final JSpinner campoCuposTotales = new JSpinner(new SpinnerNumberModel(20, 1, 9999, 1));

    private final JLabel etiquetaLogo = new JLabel();
    private final DefaultListModel<String> modeloRespaldos = new DefaultListModel<>();

    public PanelConfiguracion(ConfiguracionService configuracionService, String usuarioActual, Runnable onCambioLogo) {
        this.usuarioActual = usuarioActual;
        this.configuracionService = configuracionService;
        this.onCambioLogo = onCambioLogo;

        setLayout(new BorderLayout());
        setOpaque(false);

        // --- Aquí armo los datos del negocio (aparecen en los tickets y recibos) ---
        campoAvisoLegal.setLineWrap(true);
        campoAvisoLegal.setWrapStyleWord(true);
        JButton botonGuardarNegocio = Estilos.primario(new JButton("Guardar datos del negocio"));
        botonGuardarNegocio.addActionListener(e -> guardarDatosNegocio());

        Formulario formNegocio = new Formulario();
        formNegocio.campo("Nombre del negocio", campoNombreNegocio);
        formNegocio.campo("NIT", campoNit);
        formNegocio.campo("Dirección", campoDireccion);
        formNegocio.campo("Teléfono", campoTelefono);
        formNegocio.campo("Cupos totales de parqueo", campoCuposTotales);
        formNegocio.campo("Régimen / nota fiscal", campoRegimenTributario);
        formNegocio.campo("Aviso legal", new JScrollPane(campoAvisoLegal));
        JPanel filaGuardar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaGuardar.setOpaque(false);
        filaGuardar.add(botonGuardarNegocio);
        formNegocio.agregar(filaGuardar);

        JPanel envoltorioNegocio = new JPanel(new BorderLayout());
        envoltorioNegocio.setOpaque(false);
        envoltorioNegocio.add(Tarjeta.conTitulo("Datos del negocio", "Aparecen en los tiquetes y recibos", formNegocio),
                BorderLayout.NORTH);

        // --- Aquí armo el logo ---
        etiquetaLogo.setHorizontalAlignment(SwingConstants.CENTER);
        etiquetaLogo.setPreferredSize(new Dimension(150, 150));
        JButton botonCambiarLogo = new JButton("Cambiar logo...");
        JButton botonQuitarLogo = new JButton("Quitar logo");
        botonCambiarLogo.addActionListener(e -> cambiarLogo());
        botonQuitarLogo.addActionListener(e -> quitarLogo());
        JPanel botonesLogo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botonesLogo.setOpaque(false);
        botonesLogo.add(botonCambiarLogo);
        botonesLogo.add(Box.createHorizontalStrut(10));
        botonesLogo.add(botonQuitarLogo);
        JPanel cuerpoLogo = new JPanel(new BorderLayout(0, 12));
        cuerpoLogo.setOpaque(false);
        cuerpoLogo.add(etiquetaLogo, BorderLayout.CENTER);
        cuerpoLogo.add(botonesLogo, BorderLayout.SOUTH);

        // --- Aquí armo los respaldos ---
        JList<String> listaRespaldos = new JList<>(modeloRespaldos);
        listaRespaldos.setOpaque(false);
        listaRespaldos.setFixedCellHeight(30);
        JScrollPane scrollRespaldos = new JScrollPane(listaRespaldos);
        scrollRespaldos.setBorder(BorderFactory.createEmptyBorder());
        scrollRespaldos.setOpaque(false);
        scrollRespaldos.getViewport().setOpaque(false);
        JButton botonRespaldar = new JButton("Respaldar ahora");
        botonRespaldar.addActionListener(e -> respaldarAhora());
        JPanel pieRespaldos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pieRespaldos.setOpaque(false);
        pieRespaldos.add(botonRespaldar);
        JButton botonRespaldarFuera = new JButton("Copiar a USB o carpeta...");
        botonRespaldarFuera.addActionListener(e -> respaldarEnOtraCarpeta());
        pieRespaldos.add(Box.createHorizontalStrut(10));
        pieRespaldos.add(botonRespaldarFuera);
        JPanel cuerpoRespaldos = new JPanel(new BorderLayout(0, 12));
        cuerpoRespaldos.setOpaque(false);
        cuerpoRespaldos.add(scrollRespaldos, BorderLayout.CENTER);
        cuerpoRespaldos.add(pieRespaldos, BorderLayout.SOUTH);

        JPanel columnaDerecha = new JPanel(new GridLayout(2, 1, 0, 16));
        columnaDerecha.setOpaque(false);
        columnaDerecha.add(Tarjeta.conTitulo("Logo del negocio", "Se muestra en el menú y en los tiquetes", cuerpoLogo));
        columnaDerecha.add(Tarjeta.conTitulo("Copias de seguridad", "Se guarda una automática cada día", cuerpoRespaldos));

        JPanel columnas = new JPanel(new GridLayout(1, 2, 16, 0));
        columnas.setOpaque(false);
        columnas.add(envoltorioNegocio);
        columnas.add(columnaDerecha);
        add(columnas, BorderLayout.CENTER);

        refrescar();
    }

    public void refrescar() {
        ConfiguracionNegocio negocio = configuracionService.obtener();
        campoNombreNegocio.setText(negocio.getNombre());
        campoNit.setText(negocio.getNit());
        campoDireccion.setText(negocio.getDireccion());
        campoTelefono.setText(negocio.getTelefono());
        campoRegimenTributario.setText(negocio.getRegimenTributario());
        campoAvisoLegal.setText(negocio.getAvisoLegal());
        campoCuposTotales.setValue(negocio.getCuposTotales());

        ImageIcon logo = RecursosApp.cargarLogo();
        if (logo != null) {
            Image escalada = logo.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
            etiquetaLogo.setIcon(new ImageIcon(escalada));
            etiquetaLogo.setText(null);
        } else {
            etiquetaLogo.setIcon(null);
            etiquetaLogo.setText("Sin logo configurado");
        }

        modeloRespaldos.clear();
        List<Path> respaldos = respaldoService.listarRespaldos();
        for (Path p : respaldos) {
            modeloRespaldos.addElement(p.getFileName().toString());
        }
        if (respaldos.isEmpty()) {
            // Estado vacío: así se entiende que la lista está bien y que simplemente aún no hay copias.
            modeloRespaldos.addElement("Todavía no hay copias de seguridad");
        }
    }

    private void guardarDatosNegocio() {
        try {
            if (campoNombreNegocio.getText().isBlank()) {
                throw new IllegalArgumentException("El nombre del negocio es obligatorio");
            }
            campoCuposTotales.commitEdit();
            ConfiguracionNegocio negocio = new ConfiguracionNegocio(
                    campoNombreNegocio.getText().trim(), campoNit.getText().trim(),
                    campoDireccion.getText().trim(), campoTelefono.getText().trim(),
                    campoRegimenTributario.getText().trim(), campoAvisoLegal.getText().trim(),
                    (Integer) campoCuposTotales.getValue());
            ConfiguracionNegocio anterior = configuracionService.obtener();
            configuracionService.actualizar(negocio);
            new com.parqueadero.servicio.AuditoriaService().registrar(usuarioActual, "NEGOCIO_ACTUALIZADO",
                    "Cupos " + anterior.getCuposTotales() + " → " + negocio.getCuposTotales()
                            + (anterior.getNombre().equals(negocio.getNombre()) ? "" : "; nombre «" + anterior.getNombre() + "» → «" + negocio.getNombre() + "»")
                            + (java.util.Objects.equals(anterior.getNit(), negocio.getNit()) ? "" : "; NIT cambiado"));
            JOptionPane.showMessageDialog(this, "Datos del negocio guardados");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarLogo() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (png, jpg, jpeg, gif, bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        int resultado = selector.showOpenDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            RecursosApp.guardarLogo(selector.getSelectedFile().toPath());
            refrescar();
            onCambioLogo.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el logo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitarLogo() {
        try {
            RecursosApp.eliminarLogo();
            refrescar();
            onCambioLogo.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo quitar el logo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void respaldarEnOtraCarpeta() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige la memoria USB o carpeta donde guardar la copia");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path destino = respaldoService.respaldarEnCarpeta(selector.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "Copia guardada en:\n" + destino);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void respaldarAhora() {
        try {
            Path destino = respaldoService.respaldarAhora();
            JOptionPane.showMessageDialog(this, "Respaldo creado:\n" + destino);
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
