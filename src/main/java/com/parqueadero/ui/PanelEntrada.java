package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.MensualidadService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.util.FiltroMayusculas;
import com.parqueadero.util.GeneradorRecibos;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Punto único de ingreso: cuando llega un vehículo, aquí mismo se decide si es
 * para parqueo, para lavado, o para ambos — sin tener que ir a otra pestaña.
 * También es donde se registra un nuevo cliente de parqueo mensual, y donde se
 * puede verificar si una placa ya tiene una mensualidad activa.
 */
public class PanelEntrada extends JPanel {
    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final ConfiguracionService configuracionService;
    private final MensualidadService mensualidadService;
    private final Runnable onCambio;

    private final JTextField campoPlaca;
    private final JCheckBox checkParqueo;
    private final JCheckBox checkLavado;
    private final JCheckBox checkMensual;
    private final JComboBox<TipoVehiculo> comboTipo;
    private final JComboBox<ServicioLavado> comboServicio;
    private final JTextField campoNombreDueno;
    private final JTextField campoTelefonoDueno;
    private final JTextField campoNombreMensual;
    private final JTextField campoTelefonoMensual;
    private final JTextField campoMontoMensual;
    private final JSpinner spinnerMeses;
    private final JLabel labelTipo;
    private final JLabel labelServicio;
    private final JLabel labelNombreDueno;
    private final JLabel labelTelefonoDueno;
    private final JLabel labelNombreMensual;
    private final JLabel labelTelefonoMensual;
    private final JLabel labelMontoMensual;
    private final JLabel labelMeses;

    public PanelEntrada(ParqueoService parqueoService, LavadoService lavadoService,
                        ConfiguracionService configuracionService, MensualidadService mensualidadService,
                        Runnable onCambio) {
        this.parqueoService = parqueoService;
        this.lavadoService = lavadoService;
        this.configuracionService = configuracionService;
        this.mensualidadService = mensualidadService;
        this.onCambio = onCambio;

        setLayout(new BorderLayout());
        setOpaque(false);

        // Vehículo
        campoPlaca = new JTextField();
        FiltroMayusculas.aplicar(campoPlaca);
        campoPlaca.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ABC123");
        campoPlaca.putClientProperty(FlatClientProperties.STYLE, "font:bold +8; margin:6,14,6,14");
        JButton botonVerificarMensual = new JButton("Verificar mensualidad", Iconos.neutro("mensualidades", 16));
        botonVerificarMensual.addActionListener(e -> verificarMensualidad());
        comboTipo = new JComboBox<>(parqueoService.obtenerTiposVehiculo().toArray(new TipoVehiculo[0]));

        Formulario formVehiculo = new Formulario();
        formVehiculo.campo("Placa del vehículo", campoPlaca);
        JPanel filaVerificar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaVerificar.setOpaque(false);
        filaVerificar.add(botonVerificarMensual);
        formVehiculo.agregar(filaVerificar);
        labelTipo = formVehiculo.campo("Tipo de vehículo", comboTipo);

        // Servicios
        checkParqueo = new JCheckBox("Parqueo", true);
        checkLavado = new JCheckBox("Lavado", false);
        checkMensual = new JCheckBox("Cliente mensual (nuevo)", false);
        JPanel panelChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelChecks.setOpaque(false);
        panelChecks.add(checkParqueo);
        panelChecks.add(Box.createHorizontalStrut(18));
        panelChecks.add(checkLavado);
        panelChecks.add(Box.createHorizontalStrut(18));
        panelChecks.add(checkMensual);

        comboServicio = new JComboBox<>(lavadoService.obtenerServicios().toArray(new ServicioLavado[0]));
        campoNombreDueno = new JTextField();
        campoTelefonoDueno = new JTextField();
        Formulario formServicios = new Formulario();
        formServicios.agregar(panelChecks);
        labelServicio = formServicios.campo("Servicio de lavado", comboServicio);
        labelNombreDueno = formServicios.campo("Nombre del dueño (opcional)", campoNombreDueno);
        labelTelefonoDueno = formServicios.campo("Teléfono del dueño (opcional)", campoTelefonoDueno);

        // Cliente mensual
        campoNombreMensual = new JTextField();
        campoTelefonoMensual = new JTextField();
        campoMontoMensual = new JTextField();
        spinnerMeses = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        Formulario formMensual = new Formulario();
        labelNombreMensual = formMensual.campo("Nombre del cliente", campoNombreMensual);
        labelTelefonoMensual = formMensual.campo("Teléfono del cliente", campoTelefonoMensual);
        labelMontoMensual = formMensual.campo("Monto mensual a cobrar", campoMontoMensual);
        labelMeses = formMensual.campo("Meses que paga ahora", spinnerMeses);

        JButton boton = Estilos.primarioGrande(new JButton("Registrar entrada", Iconos.color("entrada", 18, Color.WHITE)));
        boton.addActionListener(e -> registrar());

        JPanel izquierda = new JPanel(new GridBagLayout());
        izquierda.setOpaque(false);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridx = 0;
        gi.weightx = 1;
        gi.fill = GridBagConstraints.HORIZONTAL;
        gi.anchor = GridBagConstraints.NORTH;
        gi.insets = new Insets(0, 0, 16, 0);
        izquierda.add(Tarjeta.conTitulo("Vehículo", "Escribe o escanea la placa", formVehiculo), gi);
        gi.insets = new Insets(0, 0, 0, 0);
        gi.weighty = 1;
        izquierda.add(Tarjeta.conTitulo("¿Qué necesita?", "Puedes combinar parqueo y lavado", formServicios), gi);

        JPanel derecha = new JPanel(new GridBagLayout());
        derecha.setOpaque(false);
        GridBagConstraints gd = new GridBagConstraints();
        gd.gridx = 0;
        gd.weightx = 1;
        gd.fill = GridBagConstraints.HORIZONTAL;
        gd.anchor = GridBagConstraints.NORTH;
        gd.insets = new Insets(0, 0, 16, 0);
        derecha.add(Tarjeta.conTitulo("Parqueo mensual", "Solo si marcas «Cliente mensual»", formMensual), gd);
        gd.insets = new Insets(0, 0, 0, 0);
        gd.weighty = 1;
        derecha.add(boton, gd);

        JPanel columnas = new JPanel(new GridLayout(1, 2, 16, 0));
        columnas.setOpaque(false);
        columnas.add(izquierda);
        columnas.add(derecha);

        add(Pagina.desplazable(columnas), BorderLayout.CENTER);

        checkParqueo.addItemListener(e -> actualizarCamposVisibles());
        checkLavado.addItemListener(e -> actualizarCamposVisibles());
        checkMensual.addItemListener(e -> actualizarCamposVisibles());
        actualizarCamposVisibles();
    }

    private void actualizarCamposVisibles() {
        boolean parqueo = checkParqueo.isSelected();
        boolean lavado = checkLavado.isSelected();
        boolean mensual = checkMensual.isSelected();
        labelTipo.setEnabled(parqueo);
        comboTipo.setEnabled(parqueo);
        labelServicio.setEnabled(lavado);
        comboServicio.setEnabled(lavado);
        labelNombreDueno.setEnabled(lavado);
        campoNombreDueno.setEnabled(lavado);
        labelTelefonoDueno.setEnabled(lavado);
        campoTelefonoDueno.setEnabled(lavado);
        labelNombreMensual.setEnabled(mensual);
        campoNombreMensual.setEnabled(mensual);
        labelTelefonoMensual.setEnabled(mensual);
        campoTelefonoMensual.setEnabled(mensual);
        labelMontoMensual.setEnabled(mensual);
        campoMontoMensual.setEnabled(mensual);
        labelMeses.setEnabled(mensual);
        spinnerMeses.setEnabled(mensual);
    }

    private void verificarMensualidad() {
        String placa = campoPlaca.getText().trim();
        if (placa.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe primero la placa a verificar");
            return;
        }
        Mensualidad mensualidad = mensualidadService.obtenerVigentePorPlaca(placa);
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (mensualidad == null) {
            JOptionPane.showMessageDialog(this, "La placa " + placa.toUpperCase() +
                    " no tiene una mensualidad activa.\nMarca \"Registrar como cliente mensual\" para crear una.");
        } else {
            JOptionPane.showMessageDialog(this, "✅ La placa " + mensualidad.getPlaca() +
                    " tiene mensualidad activa hasta el " + mensualidad.getFechaVencimiento().format(fmtFecha) +
                    ".\nNo se le cobrará por parqueo mientras esté vigente.");
        }
    }

    private void registrar() {
        try {
            boolean parqueo = checkParqueo.isSelected();
            boolean lavado = checkLavado.isSelected();
            boolean mensual = checkMensual.isSelected();
            if (!parqueo && !lavado && !mensual) {
                throw new IllegalArgumentException("Selecciona al menos Parqueo, Lavado o Mensualidad");
            }

            String placa = campoPlaca.getText();
            Vehiculo vehiculoRegistrado = null;
            RegistroLavado lavadoRegistrado = null;
            Mensualidad mensualidadRegistrada = null;

            if (mensual) {
                int monto = Integer.parseInt(campoMontoMensual.getText().trim());
                int meses = (Integer) spinnerMeses.getValue();
                mensualidadRegistrada = mensualidadService.registrar(placa, campoNombreMensual.getText(),
                        campoTelefonoMensual.getText(), (TipoVehiculo) comboTipo.getSelectedItem(), monto, meses);
                placa = mensualidadRegistrada.getPlaca();
            }
            if (parqueo) {
                vehiculoRegistrado = parqueoService.registrarEntrada(placa, (TipoVehiculo) comboTipo.getSelectedItem());
                placa = vehiculoRegistrado.getPlaca();
            }
            if (lavado) {
                ServicioLavado servicio = (ServicioLavado) comboServicio.getSelectedItem();
                Integer vehiculoId = vehiculoRegistrado != null ? vehiculoRegistrado.getId() : null;
                lavadoRegistrado = lavadoService.registrarLavado(placa, servicio, vehiculoId,
                        campoNombreDueno.getText(), campoTelefonoDueno.getText());
                placa = lavadoRegistrado.getPlaca();
            }

            campoPlaca.setText("");
            campoNombreDueno.setText("");
            campoTelefonoDueno.setText("");
            campoNombreMensual.setText("");
            campoTelefonoMensual.setText("");
            campoMontoMensual.setText("");
            checkMensual.setSelected(false);
            onCambio.run();

            ConfiguracionNegocio negocio = configuracionService.obtener();
            String textoTicket = GeneradorRecibos.ticketEntrada(negocio, placa, vehiculoRegistrado,
                    lavadoRegistrado, mensualidadRegistrada);

            new DialogoTicket(SwingUtilities.getWindowAncestor(this), "Ticket de entrada", textoTicket, placa)
                    .setVisible(true);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El monto mensual debe ser un número", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Se llama cuando otras pestañas modifican datos que afectan este formulario (p. ej. nuevos servicios de lavado). */
    public void refrescar() {
        Object tipoSeleccionado = comboTipo.getSelectedItem();
        comboTipo.removeAllItems();
        for (TipoVehiculo t : parqueoService.obtenerTiposVehiculo()) {
            comboTipo.addItem(t);
        }
        if (tipoSeleccionado != null) {
            comboTipo.setSelectedItem(tipoSeleccionado);
        }
        ServicioLavado seleccionado = (ServicioLavado) comboServicio.getSelectedItem();
        comboServicio.removeAllItems();
        for (ServicioLavado s : lavadoService.obtenerServicios()) {
            comboServicio.addItem(s);
        }
        if (seleccionado != null) {
            comboServicio.setSelectedItem(seleccionado);
        }
    }
}
