package com.parqueadero.ui;

import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Usuario;
import com.parqueadero.modelo.Vehiculo;
import com.parqueadero.servicio.AuthService;
import com.parqueadero.servicio.ConfiguracionService;
import com.parqueadero.servicio.LavadoService;
import com.parqueadero.servicio.MensualidadService;
import com.parqueadero.servicio.ParqueoService;
import com.parqueadero.util.FiltroMayusculas;
import com.parqueadero.util.GeneradorRecibos;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

/**
 * Aquí reúno las operaciones que puedo hacer con un vehículo (cobrar salida, registrar entrada o lavado, editar, cancelar),
 * con sus diálogos de cobro, tickets y autorizaciones. Las uso tanto desde la pantalla de inicio como desde "Escanear salida".
 */
public class AccionesVehiculo {
    private final ParqueoService parqueoService;
    private final LavadoService lavadoService;
    private final ConfiguracionService configuracionService;
    private final MensualidadService mensualidadService;
    private final AuthService authService;
    private final Usuario usuarioActivo;
    private final Runnable alCambiar;

    public AccionesVehiculo(ParqueoService parqueoService, LavadoService lavadoService,
                            ConfiguracionService configuracionService, MensualidadService mensualidadService,
                            AuthService authService, Usuario usuarioActivo, Runnable alCambiar) {
        this.parqueoService = parqueoService;
        this.lavadoService = lavadoService;
        this.configuracionService = configuracionService;
        this.mensualidadService = mensualidadService;
        this.authService = authService;
        this.usuarioActivo = usuarioActivo;
        this.alCambiar = alCambiar;
    }

    public ParqueoService parqueo() {
        return parqueoService;
    }

    public LavadoService lavado() {
        return lavadoService;
    }

    public MensualidadService mensualidades() {
        return mensualidadService;
    }

    public Usuario usuario() {
        return usuarioActivo;
    }

    // ---------------------------------------------------------------- salida

    /** Cobro el parqueo activo y/o los lavados pendientes de la placa, uno por uno, con su recibo. */
    public void cobrarSalida(Component padre, String placa, Consumer<String> registro) {
        try {
            Vehiculo vehiculoActivo = parqueoService.obtenerActivoPorPlaca(placa);
            List<RegistroLavado> lavadosPendientes = lavadoService.obtenerPendientesPorPlaca(placa);
            if (vehiculoActivo == null && lavadosPendientes.isEmpty()) {
                throw new IllegalStateException("No se encontró ningún parqueo o lavado activo para la placa " + placa);
            }
            ConfiguracionNegocio negocio = configuracionService.obtener();
            if (vehiculoActivo != null) {
                cerrarParqueo(padre, vehiculoActivo, negocio, registro);
            }
            for (RegistroLavado pendiente : lavadosPendientes) {
                cerrarLavado(padre, pendiente, negocio, registro);
            }
        } catch (Exception ex) {
            avisar(registro, placa + " — " + ex.getMessage());
            JOptionPane.showMessageDialog(padre, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            alCambiar.run();
        }
    }

    private void cerrarParqueo(Component padre, Vehiculo vehiculo, ConfiguracionNegocio negocio, Consumer<String> registro) {
        ParqueoService.PrevisualizacionSalida previsualizacion = parqueoService.previsualizarSalida(vehiculo.getId());
        Integer montoPagado;
        if (vehiculo.isEsMensual()) {
            montoPagado = 0;
        } else {
            montoPagado = DialogoPago.pedirMontoPagado(padre,
                    "Salida de parqueo - " + vehiculo.getPlaca(), previsualizacion.total());
            if (montoPagado == null) {
                avisar(registro, vehiculo.getPlaca() + " — salida de parqueo cancelada");
                return;
            }
        }
        ParqueoService.ResultadoSalida resultado = parqueoService.confirmarSalida(previsualizacion, montoPagado);
        avisar(registro, vehiculo.getPlaca() + " — salida de parqueo registrada: $" + resultado.total()
                + (vehiculo.isEsMensual() ? " (mensualidad)" : ""));

        if (vehiculo.isEsMensual()) {
            JOptionPane.showMessageDialog(padre, "Salida registrada: " + vehiculo.getPlaca()
                    + " tiene mensualidad, no se cobra parqueo.", "Mensualidad", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String recibo = GeneradorRecibos.reciboParqueo(negocio, vehiculo.getId(), vehiculo.getPlaca(),
                vehiculo.getTipo().name(), vehiculo.getHoraEntrada(), resultado.horaSalida(), resultado.total(),
                resultado.cajero(), resultado.montoPagado(), resultado.vuelto());
        new DialogoRecibo(ventana(padre), "Recibo de parqueo - " + vehiculo.getPlaca(), recibo).setVisible(true);
    }

    private void cerrarLavado(Component padre, RegistroLavado registro, ConfiguracionNegocio negocio, Consumer<String> log) {
        LavadoService.PrevisualizacionCompletar previsualizacion = lavadoService.previsualizarCompletar(registro.getId());
        Integer montoPagado = DialogoPago.pedirMontoPagado(padre,
                "Lavado - " + registro.getPlaca() + " (" + registro.getServicio().getNombre() + ")", registro.getPrecio());
        if (montoPagado == null) {
            avisar(log, registro.getPlaca() + " — lavado pendiente de cobrar (cancelado)");
            return;
        }
        LavadoService.ResultadoCompletar resultado = lavadoService.confirmarCompletar(previsualizacion, montoPagado);
        avisar(log, registro.getPlaca() + " — lavado completado: $" + registro.getPrecio());

        String recibo = GeneradorRecibos.reciboLavado(negocio, registro.getId(), registro.getPlaca(),
                registro.getServicio().getNombre(), registro.getPrecio(), registro.getFechaInicio(), resultado.fechaFin(),
                registro.getNombreDueno(), registro.getTelefonoDueno(), resultado.cajero(),
                resultado.montoPagado(), resultado.vuelto());
        new DialogoRecibo(ventana(padre), "Recibo de lavado - " + registro.getPlaca(), recibo).setVisible(true);
    }

    // ---------------------------------------------------------------- entrada y lavado

    /** Registro la entrada de parqueo (y, si elijo un servicio, también el lavado) y muestro el ticket con su QR. */
    public void registrarEntrada(Component padre, String placa, TipoVehiculo tipo, ServicioLavado servicioLavado) {
        try {
            Vehiculo vehiculo = parqueoService.registrarEntrada(placa, tipo);
            RegistroLavado lavado = null;
            if (servicioLavado != null) {
                lavado = lavadoService.registrarLavado(vehiculo.getPlaca(), servicioLavado, vehiculo.getId(), null, null);
            }
            alCambiar.run();
            mostrarTicket(padre, vehiculo.getPlaca(), vehiculo, lavado);
        } catch (Exception ex) {
            mostrarError(padre, ex);
        }
    }

    /** Registro solo un lavado (el vehículo no se queda en el parqueadero) o le sumo un lavado a un vehículo que ya está dentro. */
    public void registrarLavado(Component padre, String placa, ServicioLavado servicio, Vehiculo vehiculoDentro) {
        try {
            RegistroLavado lavado = lavadoService.registrarLavado(placa, servicio,
                    vehiculoDentro != null ? vehiculoDentro.getId() : null, null, null);
            alCambiar.run();
            mostrarTicket(padre, lavado.getPlaca(), null, lavado);
        } catch (Exception ex) {
            mostrarError(padre, ex);
        }
    }

    private void mostrarTicket(Component padre, String placa, Vehiculo vehiculo, RegistroLavado lavado) {
        String texto = GeneradorRecibos.ticketEntrada(configuracionService.obtener(), placa, vehiculo, lavado, null);
        new DialogoTicket(ventana(padre), "Ticket de entrada", texto, placa).setVisible(true);
    }

    // ---------------------------------------------------------------- editar y cancelar (con autorización)

    public void editarEntrada(Component padre, Vehiculo v) {
        String autorizadoPor = DialogoAutorizacionAdmin.solicitarAutorizacion(ventana(padre), authService,
                usuarioActivo, "Editar la entrada de " + v.getPlaca());
        if (autorizadoPor == null) {
            return;
        }
        JTextField campoPlaca = new JTextField(v.getPlaca());
        FiltroMayusculas.aplicar(campoPlaca);
        JComboBox<TipoVehiculo> comboTipo = new JComboBox<>(parqueoService.obtenerTiposVehiculo().toArray(new TipoVehiculo[0]));
        comboTipo.setSelectedItem(v.getTipo());
        Object[] mensaje = {"Placa:", campoPlaca, "Tipo de vehículo:", comboTipo};
        int resultado = JOptionPane.showConfirmDialog(padre, mensaje, "Editar entrada", JOptionPane.OK_CANCEL_OPTION);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            parqueoService.editarEntrada(v.getId(), campoPlaca.getText(), (TipoVehiculo) comboTipo.getSelectedItem(),
                    autorizadoPor);
            alCambiar.run();
            JOptionPane.showMessageDialog(padre, "Entrada actualizada correctamente");
        } catch (Exception ex) {
            mostrarError(padre, ex);
        }
    }

    public void cancelarEntrada(Component padre, Vehiculo v) {
        String autorizadoPor = DialogoAutorizacionAdmin.solicitarAutorizacion(ventana(padre), authService,
                usuarioActivo, "Cancelar la entrada de " + v.getPlaca());
        if (autorizadoPor == null) {
            return;
        }
        String motivo = JOptionPane.showInputDialog(padre,
                "Motivo de la cancelación de " + v.getPlaca() + " (queda registrado):",
                "Cancelar entrada", JOptionPane.WARNING_MESSAGE);
        if (motivo == null) {
            return;
        }
        try {
            parqueoService.cancelarEntrada(v.getId(), motivo, autorizadoPor);
            alCambiar.run();
            JOptionPane.showMessageDialog(padre, "Entrada cancelada. Quedó registrada en la auditoría.");
        } catch (Exception ex) {
            mostrarError(padre, ex);
        }
    }

    // ---------------------------------------------------------------- utilidades

    private static void avisar(Consumer<String> registro, String texto) {
        if (registro != null) {
            registro.accept(texto);
        }
    }

    private static Window ventana(Component padre) {
        return padre instanceof Window w ? w : SwingUtilities.getWindowAncestor(padre);
    }

    private static void mostrarError(Component padre, Exception ex) {
        ex.printStackTrace();
        String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        JOptionPane.showMessageDialog(padre, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
