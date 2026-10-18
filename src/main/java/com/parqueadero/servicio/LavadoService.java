package com.parqueadero.servicio;

import com.parqueadero.dao.LavadoDAO;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LavadoService {

    private final LavadoDAO lavadoDAO = new LavadoDAO();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final String usuarioActual;

    public LavadoService(String usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public List<ServicioLavado> obtenerServicios() {
        return lavadoDAO.obtenerServicios();
    }

    public RegistroLavado registrarLavado(String placa, ServicioLavado servicio, Integer vehiculoId,
                                          String nombreDueno, String telefonoDueno) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        String nombreLimpio = (nombreDueno == null || nombreDueno.isBlank()) ? null : nombreDueno.trim();
        String telefonoLimpio = (telefonoDueno == null || telefonoDueno.isBlank()) ? null : telefonoDueno.trim();
        RegistroLavado registro = new RegistroLavado(placa.trim().toUpperCase(), servicio, vehiculoId,
                LocalDateTime.now(), nombreLimpio, telefonoLimpio, usuarioActual);
        return lavadoDAO.insertar(registro);
    }

    public record PrevisualizacionCompletar(RegistroLavado registro, LocalDateTime fechaFin) {
    }

    public record ResultadoCompletar(LocalDateTime fechaFin, int montoPagado, int vuelto, String cajero) {
    }

    public PrevisualizacionCompletar previsualizarCompletar(int id) {
        RegistroLavado registro = lavadoDAO.obtenerPorId(id);
        if (registro == null) {
            throw new IllegalArgumentException("Lavado no encontrado");
        }
        return new PrevisualizacionCompletar(registro, LocalDateTime.now());
    }

    public ResultadoCompletar confirmarCompletar(PrevisualizacionCompletar previsualizacion, int montoPagado) {
        int total = previsualizacion.registro().getPrecio();
        if (montoPagado < total) {
            throw new IllegalArgumentException("El monto recibido es menor al total a pagar");
        }
        int vuelto = montoPagado - total;
        lavadoDAO.completar(previsualizacion.registro().getId(), previsualizacion.fechaFin(),
                usuarioActual, montoPagado, vuelto);
        return new ResultadoCompletar(previsualizacion.fechaFin(), montoPagado, vuelto, usuarioActual);
    }

    public ServicioLavado crearServicio(String nombre, int precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        ServicioLavado creado = lavadoDAO.crearServicio(nombre.trim(), precio);
        auditoria.registrar(usuarioActual, "SERVICIO_LAVADO_CREADO", nombre.trim() + " a $" + precio);
        return creado;
    }

    public void actualizarPrecioServicio(int servicioId, int precio) {
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        ServicioLavado anterior = lavadoDAO.obtenerServicios().stream()
                .filter(s -> s.getId() == servicioId).findFirst().orElse(null);
        if (anterior != null && anterior.getPrecio() == precio) {
            return;
        }
        lavadoDAO.actualizarPrecio(servicioId, precio);
        auditoria.registrar(usuarioActual, "PRECIO_LAVADO_CAMBIADO", (anterior != null ? anterior.getNombre() : "Servicio #" + servicioId)
                + ": $" + (anterior != null ? anterior.getPrecio() : 0) + " → $" + precio);
    }

    public void eliminarServicio(int servicioId) {
        ServicioLavado anterior = lavadoDAO.obtenerServicios().stream()
                .filter(s -> s.getId() == servicioId).findFirst().orElse(null);
        lavadoDAO.eliminarServicio(servicioId);
        auditoria.registrar(usuarioActual, "SERVICIO_LAVADO_ELIMINADO",
                anterior != null ? anterior.getNombre() + " ($" + anterior.getPrecio() + ")" : "Servicio #" + servicioId);
    }

    public List<RegistroLavado> obtenerPendientes() {
        return lavadoDAO.obtenerPendientes();
    }

    public List<RegistroLavado> obtenerPendientesPorPlaca(String placa) {
        return lavadoDAO.obtenerPendientesPorPlaca(placa.trim().toUpperCase());
    }

    public List<RegistroLavado> obtenerHistorial(String placaContiene, LocalDate desde, int limite) {
        return lavadoDAO.obtenerHistorial(placaContiene, desde, limite);
    }

    public List<RegistroLavado> obtenerHistorialEnRango(LocalDate desde, LocalDate hasta) {
        return lavadoDAO.obtenerHistorialEnRango(desde, hasta);
    }

    public int ingresosEnRango(LocalDate desde, LocalDate hasta) {
        return lavadoDAO.totalEnRango(desde, hasta);
    }
}
