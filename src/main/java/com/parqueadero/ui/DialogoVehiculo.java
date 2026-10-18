package com.parqueadero.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.ParqueoService;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "¿Qué quieres hacer con este vehículo?": se abre al escribir o escanear una placa en Inicio y, según
 * el estado del vehículo (dentro del parqueadero, con lavado pendiente, o que llega ahora), ofrece las
 * acciones posibles. Al elegir una, se cierra y la acción sigue su flujo (cobro, ticket, autorización...).
 */
public class DialogoVehiculo extends JDialog {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final AccionesVehiculo acciones;
    private final String placa;
    private final Component padre;

    private DialogoVehiculo(Window propietario, Component padre, AccionesVehiculo acciones, String placa) {
        super(propietario, "Vehículo " + placa, ModalityType.APPLICATION_MODAL);
        this.acciones = acciones;
        this.placa = placa;
        this.padre = padre;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Vehiculo dentro = acciones.parqueo().obtenerActivoPorPlaca(placa);
        List<RegistroLavado> lavadosPendientes = acciones.lavado().obtenerPendientesPorPlaca(placa);
        Mensualidad mensualidad = acciones.mensualidades().obtenerVigentePorPlaca(placa);

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        contenido.add(construirCabecera(dentro, lavadosPendientes, mensualidad), BorderLayout.NORTH);

        if (dentro != null) {
            contenido.add(construirCuerpoDentro(dentro, lavadosPendientes), BorderLayout.CENTER);
        } else {
            contenido.add(construirCuerpoNuevo(mensualidad, lavadosPendientes), BorderLayout.CENTER);
        }

        setContentPane(contenido);
        pack();
        setMinimumSize(new Dimension(500, getHeight()));
        setLocationRelativeTo(propietario);
    }

    /** Abre el diálogo para la placa indicada. */
    public static void mostrar(Component padre, AccionesVehiculo acciones, String placa) {
        Window propietario = padre instanceof Window w ? w : SwingUtilities.getWindowAncestor(padre);
        new DialogoVehiculo(propietario, padre, acciones, placa).setVisible(true);
    }

    // ---------------------------------------------------------------- piezas

    private JPanel construirCabecera(Vehiculo dentro, List<RegistroLavado> pendientes, Mensualidad mensualidad) {
        JLabel labelPlaca = new JLabel(placa);
        labelPlaca.putClientProperty(FlatClientProperties.STYLE, "font:bold +14");

        Pildora estado;
        if (dentro != null) {
            estado = new Pildora("Dentro del parqueadero", Estilos.Tono.VERDE);
        } else if (!pendientes.isEmpty()) {
            estado = new Pildora("Lavado pendiente de cobrar", Estilos.Tono.AMBAR);
        } else {
            estado = new Pildora("No está en el parqueadero", Estilos.Tono.NEUTRO);
        }

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fila.setOpaque(false);
        fila.add(estado);

        JPanel cab = new JPanel(new BorderLayout(0, 8));
        cab.add(labelPlaca, BorderLayout.NORTH);
        cab.add(fila, BorderLayout.CENTER);
        Mensualidad activa = acciones.mensualidades().obtenerActivaPorPlaca(placa);
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (mensualidad != null) {
            cab.add(aviso("Cliente con mensualidad vigente hasta el " + mensualidad.getFechaVencimiento().format(fmtFecha)
                    + ". No se le cobra parqueo; solo ocupa su cupo.", Estilos.Tono.VERDE), BorderLayout.SOUTH);
        } else if (activa != null) {
            cab.add(aviso("Su mensualidad venció el " + activa.getFechaVencimiento().format(fmtFecha)
                    + ". Se cobra por horas; renuévala en Mensualidades.", Estilos.Tono.AMBAR), BorderLayout.SOUTH);
        }
        return cab;
    }

    private static JComponent aviso(String texto, Estilos.Tono tono) {
        Pildora p = new Pildora("<html><body style='width:380px'>" + texto + "</body></html>", tono);
        JPanel envoltorio = new JPanel(new BorderLayout());
        envoltorio.setOpaque(false);
        envoltorio.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        envoltorio.add(p, BorderLayout.CENTER);
        return envoltorio;
    }

    private JPanel construirCuerpoDentro(Vehiculo v, List<RegistroLavado> pendientes) {
        ParqueoService.PrevisualizacionSalida previa = acciones.parqueo().previsualizarSalida(v.getId());
        Duration d = Duration.between(v.getHoraEntrada(), LocalDateTime.now());
        String tiempo = d.toHours() > 0 ? d.toHours() + " h " + d.toMinutesPart() + " min" : d.toMinutes() + " min";

        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 6));
        datos.setOpaque(false);
        datos.add(fila("Tipo", String.valueOf(v.getTipo())));
        datos.add(fila("Entró", v.getHoraEntrada().format(HORA)));
        datos.add(fila("Tiempo", tiempo));
        JLabel total = new JLabel(v.isEsMensual() ? "Sin cobro (mensualidad)" : "$" + formato(previa.total()));
        total.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        datos.add(filaConComponente("A cobrar ahora", total));
        for (RegistroLavado r : pendientes) {
            datos.add(fila("Lavado pendiente", r.getServicio().getNombre() + " — $" + formato(r.getPrecio())));
        }

        JButton cobrar = Estilos.primarioGrande(new JButton(v.isEsMensual() ? "Registrar salida (sin cobro)" : "Cobrar salida", Iconos.color("salida", 18, Color.WHITE)));
        cobrar.addActionListener(e -> ejecutar(() -> acciones.cobrarSalida(padre, placa, null)));
        getRootPane().setDefaultButton(cobrar);

        JComboBox<ServicioLavado> comboServicio = comboServicios();
        JButton agregarLavado = new JButton("Agregar lavado", Iconos.neutro("lavado", 16));
        agregarLavado.addActionListener(e -> {
            ServicioLavado s = (ServicioLavado) comboServicio.getSelectedItem();
            if (s != null) {
                ejecutar(() -> acciones.registrarLavado(padre, placa, s, v));
            }
        });
        JPanel filaLavado = new JPanel(new BorderLayout(8, 0));
        filaLavado.setOpaque(false);
        filaLavado.add(comboServicio, BorderLayout.CENTER);
        filaLavado.add(agregarLavado, BorderLayout.EAST);

        JButton editar = new JButton("Editar entrada");
        editar.addActionListener(e -> ejecutar(() -> acciones.editarEntrada(padre, v)));
        JButton cancelar = new JButton("Cancelar entrada");
        cancelar.addActionListener(e -> ejecutar(() -> acciones.cancelarEntrada(padre, v)));
        JPanel filaEditar = new JPanel(new GridLayout(1, 2, 8, 0));
        filaEditar.setOpaque(false);
        filaEditar.add(editar);
        filaEditar.add(cancelar);

        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 10));
        botones.setOpaque(false);
        botones.add(cobrar);
        botones.add(filaLavado);
        botones.add(filaEditar);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 16));
        cuerpo.setOpaque(false);
        cuerpo.add(datos, BorderLayout.CENTER);
        cuerpo.add(botones, BorderLayout.SOUTH);
        return cuerpo;
    }

    /** Vehículo que no está dentro: registrar entrada (solo parqueo, parqueo + lavado o solo lavado) y, si hay, cobrar un lavado pendiente. */
    private JPanel construirCuerpoNuevo(Mensualidad mensualidad, List<RegistroLavado> pendientes) {
        JComboBox<TipoVehiculo> comboTipo = comboTipos(mensualidad);
        JComboBox<ServicioLavado> comboServicio = comboServicios();

        JPanel superior = new JPanel(new BorderLayout(0, 12));
        superior.setOpaque(false);
        if (!pendientes.isEmpty()) {
            JPanel datos = new JPanel(new GridLayout(0, 1, 0, 6));
            datos.setOpaque(false);
            for (RegistroLavado r : pendientes) {
                datos.add(fila("Lavado pendiente", r.getServicio().getNombre() + " — $" + formato(r.getPrecio())
                        + " (desde " + r.getFechaInicio().format(HORA) + ")"));
            }
            superior.add(datos, BorderLayout.NORTH);
        }
        Formulario form = new Formulario();
        form.campo("Tipo de vehículo", comboTipo);
        form.campo("Servicio de lavado (solo si lo necesita)", comboServicio);
        superior.add(form, BorderLayout.CENTER);

        boolean hayPendientes = !pendientes.isEmpty();
        JButton soloParqueo = new JButton("Solo parqueo — registrar entrada",
                hayPendientes ? Iconos.neutro("entrada", 16) : Iconos.color("entrada", 18, Color.WHITE));
        if (!hayPendientes) {
            Estilos.primarioGrande(soloParqueo);
        }
        soloParqueo.addActionListener(e -> ejecutar(() ->
                acciones.registrarEntrada(padre, placa, (TipoVehiculo) comboTipo.getSelectedItem(), null)));

        JButton parqueoYLavado = new JButton("Parqueo + lavado", Iconos.neutro("lavado", 16));
        parqueoYLavado.addActionListener(e -> {
            ServicioLavado s = (ServicioLavado) comboServicio.getSelectedItem();
            if (s != null) {
                ejecutar(() -> acciones.registrarEntrada(padre, placa, (TipoVehiculo) comboTipo.getSelectedItem(), s));
            }
        });
        JButton soloLavado = new JButton("Solo lavado", Iconos.neutro("lavado", 16));
        soloLavado.addActionListener(e -> {
            ServicioLavado s = (ServicioLavado) comboServicio.getSelectedItem();
            if (s != null) {
                ejecutar(() -> acciones.registrarLavado(padre, placa, s, null));
            }
        });
        JPanel filaLavados = new JPanel(new GridLayout(1, 2, 8, 0));
        filaLavados.setOpaque(false);
        filaLavados.add(parqueoYLavado);
        filaLavados.add(soloLavado);

        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 10));
        botones.setOpaque(false);
        if (!pendientes.isEmpty()) {
            JButton cobrar = Estilos.primarioGrande(new JButton("Cobrar lavado pendiente", Iconos.color("caja", 18, Color.WHITE)));
            cobrar.addActionListener(e -> ejecutar(() -> acciones.cobrarSalida(padre, placa, null)));
            botones.add(cobrar);
            getRootPane().setDefaultButton(cobrar);
            botones.add(soloParqueo);
        } else {
            botones.add(soloParqueo);
            getRootPane().setDefaultButton(soloParqueo);
        }
        botones.add(filaLavados);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 16));
        cuerpo.setOpaque(false);
        cuerpo.add(superior, BorderLayout.CENTER);
        cuerpo.add(botones, BorderLayout.SOUTH);
        return cuerpo;
    }


    private JComboBox<TipoVehiculo> comboTipos(Mensualidad mensualidad) {
        JComboBox<TipoVehiculo> combo = new JComboBox<>(acciones.parqueo().obtenerTiposVehiculo().toArray(new TipoVehiculo[0]));
        if (mensualidad != null) {
            combo.setSelectedItem(mensualidad.getTipoVehiculo());
        }
        return combo;
    }

    private JComboBox<ServicioLavado> comboServicios() {
        return new JComboBox<>(acciones.lavado().obtenerServicios().toArray(new ServicioLavado[0]));
    }

    private static JPanel fila(String etiqueta, String valor) {
        return filaConComponente(etiqueta, new JLabel(valor));
    }

    private static JPanel filaConComponente(String etiqueta, JComponent valor) {
        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.setOpaque(false);
        JLabel l = new JLabel(etiqueta);
        l.setForeground(Tema.textoSecundario());
        fila.add(l, BorderLayout.WEST);
        valor.setAlignmentX(Component.RIGHT_ALIGNMENT);
        fila.add(valor, BorderLayout.EAST);
        return fila;
    }

    private static String formato(int valor) {
        return String.format("%,d", valor).replace(',', '.');
    }

    /** Cierra este diálogo y, ya con la pantalla libre, ejecuta la acción elegida. */
    private void ejecutar(Runnable accion) {
        dispose();
        SwingUtilities.invokeLater(accion);
    }
}
