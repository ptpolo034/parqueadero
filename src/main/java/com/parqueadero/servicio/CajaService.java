package com.parqueadero.servicio;

import com.parqueadero.dao.CajaDAO;
import com.parqueadero.modelo.CierreCaja;
import com.parqueadero.modelo.MovimientoCaja;
import com.parqueadero.modelo.TipoMovimientoCaja;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CajaService {

    private final CajaDAO cajaDAO = new CajaDAO();
    private final ReporteService reporteService = new ReporteService();
    private final String usuarioActual;

    public CajaService(String usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public MovimientoCaja registrarIngreso(String concepto, int monto) {
        return registrarMovimiento(TipoMovimientoCaja.INGRESO, concepto, monto);
    }

    public MovimientoCaja registrarEgreso(String concepto, int monto) {
        return registrarMovimiento(TipoMovimientoCaja.EGRESO, concepto, monto);
    }

    private MovimientoCaja registrarMovimiento(TipoMovimientoCaja tipo, String concepto, int monto) {
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto es obligatorio");
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        MovimientoCaja movimiento = new MovimientoCaja(tipo, concepto.trim(), monto, LocalDateTime.now(), usuarioActual);
        return cajaDAO.registrarMovimiento(movimiento);
    }

    public List<MovimientoCaja> obtenerMovimientosDeHoy() {
        return cajaDAO.obtenerMovimientosDelDia(LocalDate.now());
    }

    public int totalIngresosManualesHoy() {
        return obtenerMovimientosDeHoy().stream()
                .filter(m -> m.getTipo() == TipoMovimientoCaja.INGRESO)
                .mapToInt(MovimientoCaja::getMonto)
                .sum();
    }

    public int totalEgresosManualesHoy() {
        return obtenerMovimientosDeHoy().stream()
                .filter(m -> m.getTipo() == TipoMovimientoCaja.EGRESO)
                .mapToInt(MovimientoCaja::getMonto)
                .sum();
    }

    /** Cuánto debería haber en caja hoy, antes de contar físicamente el dinero. */
    public record ResumenCaja(int ingresosParqueo, int ingresosLavado, int ingresosMensualidades,
                               int ingresosManuales, int egresosManuales, int baseInicial, int totalEsperado) {
    }

    public ResumenCaja calcularResumen(int baseInicial) {
        int ingresosParqueo = reporteService.ingresosParqueoEnRango(LocalDate.now(), LocalDate.now());
        int ingresosLavado = reporteService.ingresosLavadoEnRango(LocalDate.now(), LocalDate.now());
        int ingresosMensualidades = reporteService.ingresosMensualidadesEnRango(LocalDate.now(), LocalDate.now());
        int ingresosManuales = totalIngresosManualesHoy();
        int egresosManuales = totalEgresosManualesHoy();
        int totalEsperado = baseInicial + ingresosParqueo + ingresosLavado + ingresosMensualidades
                + ingresosManuales - egresosManuales;
        return new ResumenCaja(ingresosParqueo, ingresosLavado, ingresosMensualidades, ingresosManuales,
                egresosManuales, baseInicial, totalEsperado);
    }

    public CierreCaja guardarCierre(int baseInicial, int montoContado) {
        if (baseInicial < 0) {
            throw new IllegalArgumentException("La base inicial no puede ser negativa");
        }
        if (montoContado < 0) {
            throw new IllegalArgumentException("El monto contado no puede ser negativo");
        }
        ResumenCaja resumen = calcularResumen(baseInicial);
        int diferencia = montoContado - resumen.totalEsperado();
        CierreCaja cierre = new CierreCaja(LocalDate.now(), usuarioActual, baseInicial,
                resumen.ingresosParqueo(), resumen.ingresosLavado(), resumen.ingresosMensualidades(),
                resumen.ingresosManuales(), resumen.egresosManuales(), resumen.totalEsperado(), montoContado,
                diferencia, LocalDateTime.now());
        return cajaDAO.guardarCierre(cierre);
    }

    public List<CierreCaja> obtenerHistorialCierres() {
        return cajaDAO.obtenerCierres();
    }
}
