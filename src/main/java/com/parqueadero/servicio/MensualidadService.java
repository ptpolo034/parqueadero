package com.parqueadero.servicio;

import com.parqueadero.dao.MensualidadDAO;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.PagoMensualidad;
import com.parqueadero.modelo.TipoVehiculo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MensualidadService {

    private final MensualidadDAO mensualidadDAO = new MensualidadDAO();
    private final String usuarioActual;

    public MensualidadService(String usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    /** Devuelvo la mensualidad en estado ACTIVA de esa placa aunque ya haya vencido por fecha, o {@code null} si no tiene. */
    public Mensualidad obtenerActivaPorPlaca(String placa) {
        return mensualidadDAO.obtenerActivaPorPlaca(placa.trim().toUpperCase());
    }

    /** Devuelvo la mensualidad vigente hoy de esa placa, o {@code null} si no tiene o ya venció. */
    public Mensualidad obtenerVigentePorPlaca(String placa) {
        Mensualidad mensualidad = mensualidadDAO.obtenerActivaPorPlaca(placa.trim().toUpperCase());
        return (mensualidad != null && mensualidad.estaVigente(LocalDate.now())) ? mensualidad : null;
    }

    public Mensualidad registrar(String placa, String nombreCliente, String telefono, TipoVehiculo tipoVehiculo,
                                 int montoMensual, int meses) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        if (montoMensual <= 0) {
            throw new IllegalArgumentException("El monto mensual debe ser mayor a cero");
        }
        if (meses <= 0) {
            throw new IllegalArgumentException("La cantidad de meses debe ser mayor a cero");
        }
        String placaNormalizada = placa.trim().toUpperCase();
        if (obtenerVigentePorPlaca(placaNormalizada) != null) {
            throw new IllegalStateException("Ya hay una mensualidad activa para la placa " + placaNormalizada);
        }
        LocalDate hoy = LocalDate.now();
        Mensualidad mensualidad = new Mensualidad(placaNormalizada,
                nombreCliente == null || nombreCliente.isBlank() ? null : nombreCliente.trim(),
                telefono == null || telefono.isBlank() ? null : telefono.trim(),
                tipoVehiculo, montoMensual, hoy, hoy.plusMonths(meses), usuarioActual);
        PagoMensualidad pago = new PagoMensualidad(0, placaNormalizada, montoMensual,
                LocalDateTime.now(), mensualidad.getFechaVencimiento(), usuarioActual);
        mensualidadDAO.crearConPago(mensualidad, pago);
        return mensualidad;
    }

    public List<Mensualidad> obtenerTodas() {
        return mensualidadDAO.obtenerTodas();
    }

    /** Extiendo el vencimiento (desde hoy si ya venció, o desde el vencimiento actual si sigue vigente) y cobro. */
    public Mensualidad renovar(int mensualidadId, int meses, int montoPagado) {
        if (meses <= 0) {
            throw new IllegalArgumentException("La cantidad de meses debe ser mayor a cero");
        }
        Mensualidad mensualidad = obtenerPorId(mensualidadId);
        LocalDate base = mensualidad.getFechaVencimiento().isBefore(LocalDate.now())
                ? LocalDate.now() : mensualidad.getFechaVencimiento();
        LocalDate nuevaFecha = base.plusMonths(meses);
        PagoMensualidad pago = new PagoMensualidad(mensualidadId, mensualidad.getPlaca(), montoPagado,
                LocalDateTime.now(), nuevaFecha, usuarioActual);
        mensualidadDAO.renovarConPago(mensualidadId, nuevaFecha, pago);

        mensualidad.setFechaVencimiento(nuevaFecha);
        return mensualidad;
    }

    /** Dejo la mensualidad como cancelada, no la borro; en la auditoría queda quién la autorizó. */
    public void cancelar(int mensualidadId, String autorizadoPor) {
        Mensualidad m = obtenerPorId(mensualidadId);
        mensualidadDAO.cancelar(mensualidadId, usuarioActual, autorizadoPor,
                "Mensualidad #" + mensualidadId + " de " + m.getPlaca()
                        + (m.getNombreCliente() != null ? " (" + m.getNombreCliente() + ")" : "")
                        + ", vencía el " + m.getFechaVencimiento());
    }

    public int ingresosEnRango(LocalDate desde, LocalDate hasta) {
        return mensualidadDAO.totalPagosEnRango(desde, hasta);
    }

    private Mensualidad obtenerPorId(int id) {
        return mensualidadDAO.obtenerTodas().stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));
    }
}
