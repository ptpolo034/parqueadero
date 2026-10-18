package com.parqueadero.servicio;

import com.parqueadero.dao.TarifaDAO;
import com.parqueadero.dao.VehiculoDAO;
import com.parqueadero.modelo.EstadoVehiculo;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Vehiculo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ParqueoService {

    private final VehiculoDAO vehiculoDAO = new VehiculoDAO();
    private final TarifaDAO tarifaDAO = new TarifaDAO();
    private final ConfiguracionService configuracionService;
    private final MensualidadService mensualidadService;
    private final String usuarioActual;

    public ParqueoService(String usuarioActual, ConfiguracionService configuracionService,
                          MensualidadService mensualidadService) {
        this.usuarioActual = usuarioActual;
        this.configuracionService = configuracionService;
        this.mensualidadService = mensualidadService;
    }

    public Vehiculo registrarEntrada(String placa, TipoVehiculo tipo) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        String placaNormalizada = placa.trim().toUpperCase();
        if (vehiculoDAO.existeActivoPorPlaca(placaNormalizada)) {
            throw new IllegalStateException("Ya hay un vehículo activo con la placa " + placaNormalizada);
        }
        if (obtenerActivos().size() >= cuposTotales()) {
            throw new IllegalStateException("No hay cupos disponibles");
        }
        boolean esMensual = mensualidadService.obtenerVigentePorPlaca(placaNormalizada) != null;
        Vehiculo vehiculo = new Vehiculo(placaNormalizada, tipo, LocalDateTime.now(), usuarioActual, esMensual);
        return vehiculoDAO.insertar(vehiculo);
    }

    /** Cobro calculado para un vehículo activo, con la hora de salida ya "congelada" para el cobro y el recibo. */
    public record PrevisualizacionSalida(Vehiculo vehiculo, LocalDateTime horaSalida, int total) {
    }

    public record ResultadoSalida(LocalDateTime horaSalida, int total, int montoPagado, int vuelto, String cajero) {
    }

    public PrevisualizacionSalida previsualizarSalida(int vehiculoId) {
        Vehiculo vehiculo = vehiculoDAO.obtenerPorId(vehiculoId);
        if (vehiculo == null) {
            throw new IllegalArgumentException("Vehículo no encontrado");
        }
        LocalDateTime horaSalida = LocalDateTime.now();
        int total = vehiculo.isEsMensual() ? 0 : calcularCobro(vehiculo.getTipo(), vehiculo.getHoraEntrada(), horaSalida);
        return new PrevisualizacionSalida(vehiculo, horaSalida, total);
    }

    public ResultadoSalida confirmarSalida(PrevisualizacionSalida previsualizacion, int montoPagado) {
        if (montoPagado < previsualizacion.total()) {
            throw new IllegalArgumentException("El monto recibido es menor al total a pagar");
        }
        int vuelto = montoPagado - previsualizacion.total();
        vehiculoDAO.registrarSalida(previsualizacion.vehiculo().getId(), previsualizacion.horaSalida(),
                previsualizacion.total(), usuarioActual, montoPagado, vuelto);
        return new ResultadoSalida(previsualizacion.horaSalida(), previsualizacion.total(), montoPagado, vuelto, usuarioActual);
    }

    private int calcularCobro(TipoVehiculo tipo, LocalDateTime entrada, LocalDateTime salida) {
        long minutos = Duration.between(entrada, salida).toMinutes();
        var negocio = configuracionService.obtener();
        int precioHora = tarifaDAO.obtenerTarifas().getOrDefault(tipo.name(), 0);
        int precioDia = tarifaDAO.obtenerTopesPorDia().getOrDefault(tipo.name(), 0);
        return CalculadoraTarifa.calcular(minutos, precioHora, precioDia,
                negocio.getMinutosGracia(), negocio.getMinutosFraccion());
    }

    public List<Vehiculo> obtenerActivos() {
        return vehiculoDAO.obtenerActivos();
    }

    public Vehiculo obtenerActivoPorPlaca(String placa) {
        return vehiculoDAO.obtenerActivoPorPlaca(placa.trim().toUpperCase());
    }

    public List<Vehiculo> obtenerHistorial(String placaContiene, LocalDate desde, int limite) {
        return vehiculoDAO.obtenerHistorial(placaContiene, desde, limite);
    }

    public List<Vehiculo> obtenerHistorialEnRango(LocalDate desde, LocalDate hasta) {
        return vehiculoDAO.obtenerHistorialEnRango(desde, hasta);
    }

    public int ingresosEnRango(LocalDate desde, LocalDate hasta) {
        return vehiculoDAO.totalEnRango(desde, hasta);
    }

    public int cuposTotales() {
        return configuracionService.obtenerCuposTotales();
    }

    public int cuposDisponibles() {
        return cuposTotales() - obtenerActivos().size();
    }

    /**
     * Corrige la placa o el tipo de una entrada activa (p. ej. un error de digitación).
     * Queda en la auditoría con el valor anterior, el nuevo y quién lo autorizó.
     */
    public void editarEntrada(int vehiculoId, String nuevaPlaca, TipoVehiculo nuevoTipo, String autorizadoPor) {
        if (nuevaPlaca == null || nuevaPlaca.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        Vehiculo anterior = vehiculoDAO.obtenerPorId(vehiculoId);
        if (anterior == null || anterior.getEstado() != EstadoVehiculo.ACTIVO) {
            throw new IllegalStateException("La entrada ya no está activa");
        }
        String placaNormalizada = nuevaPlaca.trim().toUpperCase();
        Vehiculo actual = vehiculoDAO.obtenerActivoPorPlaca(placaNormalizada);
        if (actual != null && actual.getId() != vehiculoId) {
            throw new IllegalStateException("Ya hay otro vehículo activo con la placa " + placaNormalizada);
        }
        String detalle = "Entrada #" + vehiculoId + ": " + anterior.getPlaca() + " (" + anterior.getTipo() + ") → "
                + placaNormalizada + " (" + nuevoTipo + ")";
        vehiculoDAO.actualizarPlacaTipo(vehiculoId, placaNormalizada, nuevoTipo, usuarioActual, autorizadoPor, detalle);
    }

    /**
     * Anula una entrada activa que nunca llegó a cobrarse. El registro NO se borra: queda marcado como
     * cancelado y en la auditoría con el motivo, para que siempre se pueda saber qué pasó.
     */
    public void cancelarEntrada(int vehiculoId, String motivo, String autorizadoPor) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Escribe el motivo de la cancelación");
        }
        Vehiculo vehiculo = vehiculoDAO.obtenerPorId(vehiculoId);
        if (vehiculo == null) {
            throw new IllegalArgumentException("Vehículo no encontrado");
        }
        if (vehiculoDAO.tieneLavadosAsociados(vehiculoId)) {
            throw new IllegalStateException("No se puede cancelar: el vehículo tiene un lavado asociado");
        }
        String detalle = "Entrada #" + vehiculoId + " de " + vehiculo.getPlaca() + " (" + vehiculo.getTipo()
                + ", entró " + vehiculo.getHoraEntrada().toString().replace('T', ' ') + "). Motivo: " + motivo.trim();
        vehiculoDAO.cancelar(vehiculoId, usuarioActual, autorizadoPor, detalle);
    }

    /** Tipos de vehículo definidos en Tarifas, en el orden en que se crearon. */
    public List<TipoVehiculo> obtenerTiposVehiculo() {
        return tarifaDAO.obtenerTarifas().keySet().stream().map(TipoVehiculo::de).toList();
    }

    public void crearTipoVehiculo(String nombre, int precioHora, int precioDia) {
        TipoVehiculo tipo = TipoVehiculo.de(nombre);
        if (precioHora < 0 || precioDia < 0) {
            throw new IllegalArgumentException("Las tarifas no pueden ser negativas");
        }
        if (tarifaDAO.obtenerTarifas().containsKey(tipo.name())) {
            throw new IllegalStateException("Ya existe el tipo de vehículo " + tipo);
        }
        tarifaDAO.crearTipo(tipo.name(), precioHora, precioDia);
        new AuditoriaService().registrar(usuarioActual, "TIPO_VEHICULO_CREADO",
                tipo + " a $" + precioHora + " por hora, tope día $" + precioDia);
    }

    public void eliminarTipoVehiculo(String nombre) {
        Map<String, Integer> tarifas = tarifaDAO.obtenerTarifas();
        if (tarifas.size() <= 1) {
            throw new IllegalStateException("Debe quedar al menos un tipo de vehículo");
        }
        boolean enUso = vehiculoDAO.obtenerActivos().stream().anyMatch(v -> v.getTipo().name().equals(nombre));
        if (enUso) {
            throw new IllegalStateException("Hay vehículos de tipo " + nombre + " dentro del parqueadero ahora mismo");
        }
        tarifaDAO.eliminarTipo(nombre);
        new AuditoriaService().registrar(usuarioActual, "TIPO_VEHICULO_ELIMINADO", nombre);
    }

    public Map<String, Integer> obtenerTarifas() {
        return tarifaDAO.obtenerTarifas();
    }

    public Map<String, Integer> obtenerTopesPorDia() {
        return tarifaDAO.obtenerTopesPorDia();
    }

    public void actualizarTarifa(String tipo, int precioHora, int precioDia) {
        if (precioHora < 0 || precioDia < 0) {
            throw new IllegalArgumentException("Las tarifas no pueden ser negativas");
        }
        int horaAnterior = tarifaDAO.obtenerTarifas().getOrDefault(tipo, 0);
        int diaAnterior = tarifaDAO.obtenerTopesPorDia().getOrDefault(tipo, 0);
        if (horaAnterior == precioHora && diaAnterior == precioDia) {
            return;
        }
        tarifaDAO.actualizarTarifa(tipo, precioHora, precioDia);
        new AuditoriaService().registrar(usuarioActual, "TARIFA_CAMBIADA", tipo + ": hora $" + horaAnterior
                + " → $" + precioHora + ", tope día $" + diaAnterior + " → $" + precioDia);
    }
}
