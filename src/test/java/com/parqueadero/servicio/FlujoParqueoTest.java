package com.parqueadero.servicio;

import com.parqueadero.dao.ConexionBD;
import com.parqueadero.modelo.EstadoVehiculo;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.RegistroAuditoria;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Vehiculo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el flujo completo contra una base de datos temporal (nunca toca la base real del usuario). */
class FlujoParqueoTest {
    private static MensualidadService mensualidades;
    private static ParqueoService parqueo;
    private static LavadoService lavado;
    private static final AuditoriaService AUDITORIA = new AuditoriaService();

    @BeforeAll
    static void prepararBaseTemporal() throws Exception {
        Path casa = Files.createTempDirectory("parqueadero-test");
        System.setProperty("user.home", casa.toString());
        ConexionBD.inicializar();
        mensualidades = new MensualidadService("cajero1");
        parqueo = new ParqueoService("cajero1", new ConfiguracionService(), mensualidades);
        lavado = new LavadoService("cajero1");
    }

    @Test
    void entradaYSalidaCobranYSumanEnReportes() throws Exception {
        Vehiculo v = parqueo.registrarEntrada("tst111", TipoVehiculo.CARRO);
        assertEquals("TST111", v.getPlaca());
        // Entró hace 2 horas y 10 minutos: 3 horas a 3000
        try (Connection con = ConexionBD.obtenerConexion(); Statement st = con.createStatement()) {
            st.executeUpdate("UPDATE vehiculos SET hora_entrada = strftime('%Y-%m-%dT%H:%M:%S','now','-130 minutes','localtime') WHERE id = " + v.getId());
        }
        var previa = parqueo.previsualizarSalida(v.getId());
        assertEquals(9000, previa.total());
        parqueo.confirmarSalida(previa, 10000);

        assertNull(parqueo.obtenerActivoPorPlaca("TST111"));
        assertEquals(9000, parqueo.ingresosEnRango(LocalDate.now(), LocalDate.now()));
        assertEquals(1, parqueo.obtenerHistorial("TST", LocalDate.now(), 50).size());
        assertTrue(parqueo.obtenerHistorial("NOEXISTE", null, 50).isEmpty());
    }

    @Test
    void cancelarConservaElRegistroYDejaAuditoria() {
        Vehiculo v = parqueo.registrarEntrada("CAN222", TipoVehiculo.MOTO);
        assertThrows(IllegalArgumentException.class, () -> parqueo.cancelarEntrada(v.getId(), " ", "admin"));

        parqueo.cancelarEntrada(v.getId(), "Digitación equivocada", "admin");

        assertNull(parqueo.obtenerActivoPorPlaca("CAN222"));
        // Ya no ocupa cupo y se puede volver a registrar la placa
        assertNotNull(parqueo.registrarEntrada("CAN222", TipoVehiculo.MOTO));

        List<RegistroAuditoria> registros = AUDITORIA.listar(null, "CAN222", 10);
        assertEquals(1, registros.size());
        assertEquals("ENTRADA_CANCELADA", registros.get(0).accion());
        assertEquals("cajero1", registros.get(0).usuario());
        assertEquals("admin", registros.get(0).autorizadoPor());
        assertTrue(registros.get(0).detalle().contains("Digitación equivocada"));
    }

    @Test
    void editarEntradaQuedaEnAuditoria() {
        Vehiculo v = parqueo.registrarEntrada("EDI333", TipoVehiculo.CARRO);
        parqueo.editarEntrada(v.getId(), "edi334", TipoVehiculo.MOTO, "admin");
        Vehiculo editado = parqueo.obtenerActivoPorPlaca("EDI334");
        assertNotNull(editado);
        assertEquals(TipoVehiculo.MOTO, editado.getTipo());
        assertEquals(EstadoVehiculo.ACTIVO, editado.getEstado());
        assertEquals(1, AUDITORIA.listar(null, "EDI333", 10).size());
    }

    @Test
    void mensualidadSeCreaConSuPagoYSeCancelaConAuditoria() {
        Mensualidad m = mensualidades.registrar("men444", "Ana", "300", TipoVehiculo.CARRO, 100000, 1);
        assertTrue(m.getId() > 0);
        assertEquals(100000, mensualidades.ingresosEnRango(LocalDate.now(), LocalDate.now()));
        assertNotNull(mensualidades.obtenerVigentePorPlaca("MEN444"));

        mensualidades.renovar(m.getId(), 1, 100000);
        assertEquals(200000, mensualidades.ingresosEnRango(LocalDate.now(), LocalDate.now()));

        mensualidades.cancelar(m.getId(), "admin");
        assertNull(mensualidades.obtenerVigentePorPlaca("MEN444"));
        assertEquals(1, AUDITORIA.listar(null, "MEN444", 10).size());
    }

    @Test
    void tiposDeVehiculoNuevosSePuedenCrearYCobran() {
        parqueo.crearTipoVehiculo("Camioneta", 5000, 30000);
        assertTrue(parqueo.obtenerTiposVehiculo().contains(TipoVehiculo.de("CAMIONETA")));
        assertThrows(IllegalStateException.class, () -> parqueo.crearTipoVehiculo("camioneta", 1, 0));

        Vehiculo v = parqueo.registrarEntrada("CAM555", TipoVehiculo.de("Camioneta"));
        assertEquals(TipoVehiculo.de("CAMIONETA"), parqueo.obtenerActivoPorPlaca("CAM555").getTipo());
        // No se puede eliminar un tipo con vehículos adentro
        assertThrows(IllegalStateException.class, () -> parqueo.eliminarTipoVehiculo("CAMIONETA"));
        parqueo.cancelarEntrada(v.getId(), "prueba", "admin");
        parqueo.eliminarTipoVehiculo("CAMIONETA");
        assertTrue(!parqueo.obtenerTiposVehiculo().contains(TipoVehiculo.de("CAMIONETA")));
    }

    @Test
    void lavadoSumaEnReportes() {
        var servicio = lavado.obtenerServicios().get(0);
        var registro = lavado.registrarLavado("lav666", servicio, null, null, null);
        var previa = lavado.previsualizarCompletar(registro.getId());
        lavado.confirmarCompletar(previa, servicio.getPrecio());
        assertEquals(servicio.getPrecio(), lavado.ingresosEnRango(LocalDate.now(), LocalDate.now()));
    }
}
