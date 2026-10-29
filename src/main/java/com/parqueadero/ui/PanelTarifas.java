package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.modelo.ServicioLavado;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.ParqueoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/** Aquí reúno los precios del parqueo (por tipo de vehículo), las reglas de cobro y los servicios de lavado, todo editable por el administrador. */
public class PanelTarifas extends JPanel {
    private static final Integer[] FRACCIONES = {60, 30, 20, 15};

    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final ConfiguracionService configuracionService;
    private final Runnable onCambio;

    private final DefaultTableModel modeloTarifasParqueo;
    private final JTable tablaTarifasParqueo;
    private final JTextField campoNombreTipo = new JTextField(14);
    private final JTextField campoPrecioHoraTipo = new JTextField(7);
    private final JTextField campoTopeDiaTipo = new JTextField(7);

    private final DefaultTableModel modeloServicios;
    private final JTable tablaServicios;
    private List<ServicioLavado> servicios = List.of();
    private final JTextField campoNombreServicio = new JTextField(18);
    private final JTextField campoPrecioServicio = new JTextField(8);

    private final JSpinner spinnerGracia = new JSpinner(new SpinnerNumberModel(0, 0, 240, 5));
    private final JComboBox<Integer> comboFraccion = new JComboBox<>(FRACCIONES);

    public PanelTarifas(ParqueoService parqueoService, LavadoService lavadoService,
                        ConfiguracionService configuracionService, Runnable onCambio) {
        this.parqueoService = parqueoService;
        this.lavadoService = lavadoService;
        this.configuracionService = configuracionService;
        this.onCambio = onCambio;

        setLayout(new GridLayout(1, 2, 16, 0));
        setOpaque(false);

        // --- Tarifas de parqueo por tipo de vehículo ---
        modeloTarifasParqueo = new DefaultTableModel(
                new Object[]{"Tipo de vehículo", "Precio por hora", "Tope por día"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col >= 1;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return col >= 1 ? Integer.class : String.class;
            }
        };
        tablaTarifasParqueo = Tablas.crear(modeloTarifasParqueo, "Aún no hay tipos de vehículo");
        // Muestro los precios con punto de miles ($3.000) y el tope 0 como "Sin tope"; al editarlos se escribe el número.
        tablaTarifasParqueo.getColumnModel().getColumn(1).setCellRenderer(new RenderDinero(false));
        tablaTarifasParqueo.getColumnModel().getColumn(2).setCellRenderer(new RenderDinero(true));

        campoNombreTipo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nuevo tipo");
        campoPrecioHoraTipo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Hora");
        campoTopeDiaTipo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tope");
        JButton botonAgregarTipo = Estilos.primario(new JButton("Agregar tipo"));
        JButton botonGuardarTarifas = new JButton("Guardar tarifas");
        JButton botonEliminarTipo = new JButton("Eliminar seleccionado");
        botonAgregarTipo.addActionListener(e -> agregarTipo());
        botonGuardarTarifas.addActionListener(e -> guardarTarifasParqueo());
        botonEliminarTipo.addActionListener(e -> eliminarTipo());

        JPanel formularioTipo = filaCampos(botonAgregarTipo, campoNombreTipo, campoPrecioHoraTipo, campoTopeDiaTipo);
        formularioTipo.add(botonAgregarTipo);
        JPanel accionesTipo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accionesTipo.setOpaque(false);
        accionesTipo.add(botonGuardarTarifas);
        accionesTipo.add(botonEliminarTipo);
        JPanel pieParqueo = new JPanel(new GridLayout(2, 1, 0, 8));
        pieParqueo.setOpaque(false);
        pieParqueo.add(formularioTipo);
        pieParqueo.add(accionesTipo);

        JPanel cuerpoParqueo = new JPanel(new BorderLayout(0, 12));
        cuerpoParqueo.setOpaque(false);
        cuerpoParqueo.add(sinBorde(new JScrollPane(tablaTarifasParqueo)), BorderLayout.CENTER);
        cuerpoParqueo.add(pieParqueo, BorderLayout.SOUTH);
        Tarjeta tarjetaParqueo = Tarjeta.conTitulo("Tarifas de parqueo",
                "Doble clic en un valor para editarlo", cuerpoParqueo);

        // --- Reglas de cobro ---
        JButton botonGuardarReglas = Estilos.primario(new JButton("Guardar reglas"));
        botonGuardarReglas.addActionListener(e -> guardarReglas());
        Formulario formReglas = new Formulario();
        formReglas.campo("Minutos de gracia (no se cobra si sale antes)", spinnerGracia);
        formReglas.campo("Se cobra por bloques de (minutos)", comboFraccion);
        JPanel filaGuardarReglas = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaGuardarReglas.setOpaque(false);
        filaGuardarReglas.add(botonGuardarReglas);
        formReglas.agregar(filaGuardarReglas);
        JPanel envoltorioReglas = new JPanel(new BorderLayout());
        envoltorioReglas.setOpaque(false);
        envoltorioReglas.add(formReglas, BorderLayout.NORTH);
        Tarjeta tarjetaReglas = Tarjeta.conTitulo("Reglas de cobro",
                "60 = hora o fracción; 30 = media hora o fracción", envoltorioReglas);

        JPanel columnaIzquierda = new JPanel(new BorderLayout(0, 16));
        columnaIzquierda.setOpaque(false);
        columnaIzquierda.add(tarjetaParqueo, BorderLayout.CENTER);
        columnaIzquierda.add(tarjetaReglas, BorderLayout.SOUTH);

        // --- Servicios de lavado ---
        modeloServicios = new DefaultTableModel(new Object[]{"ID", "Servicio", "Precio"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 || col == 2 ? Integer.class : String.class;
            }
        };
        tablaServicios = Tablas.crear(modeloServicios, "Aún no hay servicios de lavado");
        tablaServicios.getColumnModel().getColumn(2).setCellRenderer(new RenderDinero(false));
        // El número interno (ID) no le sirve a quien edita precios: lo escondo de la vista pero queda en el modelo.
        tablaServicios.removeColumn(tablaServicios.getColumnModel().getColumn(0));

        campoNombreServicio.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del servicio");
        campoPrecioServicio.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Precio");
        JButton botonAgregar = Estilos.primario(new JButton("Agregar servicio"));
        JButton botonGuardarPrecios = new JButton("Guardar precios");
        JButton botonEliminar = new JButton("Eliminar seleccionado");
        botonAgregar.addActionListener(e -> agregarServicio());
        botonGuardarPrecios.addActionListener(e -> guardarPreciosServicios());
        botonEliminar.addActionListener(e -> eliminarServicio());

        JPanel formularioNuevo = filaCampos(botonAgregar, campoNombreServicio, campoPrecioServicio);
        JPanel accionesServicios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accionesServicios.setOpaque(false);
        accionesServicios.add(botonGuardarPrecios);
        accionesServicios.add(botonEliminar);
        JPanel pieServicios = new JPanel(new GridLayout(2, 1, 0, 8));
        pieServicios.setOpaque(false);
        pieServicios.add(formularioNuevo);
        pieServicios.add(accionesServicios);

        JPanel cuerpoServicios = new JPanel(new BorderLayout(0, 12));
        cuerpoServicios.setOpaque(false);
        cuerpoServicios.add(sinBorde(new JScrollPane(tablaServicios)), BorderLayout.CENTER);
        cuerpoServicios.add(pieServicios, BorderLayout.SOUTH);

        add(columnaIzquierda);
        add(Tarjeta.conTitulo("Servicios y precios de lavado", "Agrega servicios nuevos o ajusta sus precios",
                cuerpoServicios));
    }

    /** Armo una fila de campos que se reparten el ancho (el primero se estira) con el botón al final. */
    private static JPanel filaCampos(JButton boton, JTextField... campos) {
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 8);
        for (int i = 0; i < campos.length; i++) {
            campos[i].setColumns(1);
            // Pongo un mínimo para que ningún campo quede diminuto en ventanas pequeñas.
            campos[i].setMinimumSize(new Dimension(UIScale.scale(i == 0 ? 90 : 78), campos[i].getMinimumSize().height));
            gbc.gridx = i;
            gbc.weightx = i == 0 ? 1 : 0.35;
            fila.add(campos[i], gbc);
        }
        gbc.gridx = campos.length;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        fila.add(boton, gbc);
        return fila;
    }

    private static JScrollPane sinBorde(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    public void refrescar() {
        detenerEdicion(tablaTarifasParqueo);
        detenerEdicion(tablaServicios);

        modeloTarifasParqueo.setRowCount(0);
        Map<String, Integer> tarifas = parqueoService.obtenerTarifas();
        Map<String, Integer> topes = parqueoService.obtenerTopesPorDia();
        for (Map.Entry<String, Integer> t : tarifas.entrySet()) {
            modeloTarifasParqueo.addRow(new Object[]{t.getKey(), t.getValue(), topes.getOrDefault(t.getKey(), 0)});
        }

        servicios = lavadoService.obtenerServicios();
        modeloServicios.setRowCount(0);
        for (ServicioLavado s : servicios) {
            modeloServicios.addRow(new Object[]{s.getId(), s.getNombre(), s.getPrecio()});
        }

        ConfiguracionNegocio negocio = configuracionService.obtener();
        spinnerGracia.setValue(negocio.getMinutosGracia());
        comboFraccion.setSelectedItem(negocio.getMinutosFraccion());
    }

    private void agregarTipo() {
        try {
            int precioHora = Integer.parseInt(campoPrecioHoraTipo.getText().trim());
            String tope = campoTopeDiaTipo.getText().trim();
            int precioDia = tope.isEmpty() ? 0 : Integer.parseInt(tope);
            parqueoService.crearTipoVehiculo(campoNombreTipo.getText(), precioHora, precioDia);
            campoNombreTipo.setText("");
            campoPrecioHoraTipo.setText("");
            campoTopeDiaTipo.setText("");
            refrescar();
            onCambio.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El precio por hora y el tope deben ser números", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarTipo() {
        int fila = tablaTarifasParqueo.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un tipo de vehículo de la tabla");
            return;
        }
        String tipo = (String) modeloTarifasParqueo.getValueAt(fila, 0);
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el tipo \"" + tipo + "\"? Los registros anteriores se conservan.", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            parqueoService.eliminarTipoVehiculo(tipo);
            refrescar();
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarTarifasParqueo() {
        detenerEdicion(tablaTarifasParqueo);
        try {
            for (int fila = 0; fila < modeloTarifasParqueo.getRowCount(); fila++) {
                String tipo = (String) modeloTarifasParqueo.getValueAt(fila, 0);
                int precio = ((Number) modeloTarifasParqueo.getValueAt(fila, 1)).intValue();
                int tope = ((Number) modeloTarifasParqueo.getValueAt(fila, 2)).intValue();
                parqueoService.actualizarTarifa(tipo, precio, tope);
            }
            JOptionPane.showMessageDialog(this, "Tarifas de parqueo actualizadas");
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarReglas() {
        try {
            spinnerGracia.commitEdit();
            configuracionService.actualizarReglasCobro((Integer) spinnerGracia.getValue(),
                    (Integer) comboFraccion.getSelectedItem());
            JOptionPane.showMessageDialog(this, "Reglas de cobro actualizadas");
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarPreciosServicios() {
        detenerEdicion(tablaServicios);
        try {
            for (int fila = 0; fila < modeloServicios.getRowCount(); fila++) {
                int id = ((Number) modeloServicios.getValueAt(fila, 0)).intValue();
                int precio = ((Number) modeloServicios.getValueAt(fila, 2)).intValue();
                lavadoService.actualizarPrecioServicio(id, precio);
            }
            JOptionPane.showMessageDialog(this, "Precios de lavado actualizados");
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarServicio() {
        try {
            String nombre = campoNombreServicio.getText();
            int precio = Integer.parseInt(campoPrecioServicio.getText().trim());
            lavadoService.crearServicio(nombre, precio);
            campoNombreServicio.setText("");
            campoPrecioServicio.setText("");
            refrescar();
            onCambio.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El precio debe ser un número", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarServicio() {
        int fila = tablaServicios.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un servicio de la tabla");
            return;
        }
        ServicioLavado s = servicios.get(fila);
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + s.getNombre() + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            lavadoService.eliminarServicio(s.getId());
            refrescar();
            onCambio.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void detenerEdicion(JTable tabla) {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
    }

    /** Muestro los enteros como dinero a la derecha; si {@code ceroEsSinTope}, el 0 lo muestro como "Sin tope". */
    private static class RenderDinero extends javax.swing.table.DefaultTableCellRenderer {
        private final boolean ceroEsSinTope;

        RenderDinero(boolean ceroEsSinTope) {
            this.ceroEsSinTope = ceroEsSinTope;
            setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object valor) {
            if (valor instanceof Number n) {
                setText(ceroEsSinTope && n.intValue() == 0 ? "Sin tope" : Formato.dinero(n.intValue()));
            } else {
                super.setValue(valor);
            }
        }
    }
}
