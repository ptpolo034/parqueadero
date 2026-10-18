package com.parqueadero.dao;

import com.parqueadero.modelo.CierreCaja;
import com.parqueadero.modelo.MovimientoCaja;
import com.parqueadero.modelo.TipoMovimientoCaja;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CajaDAO {

    public MovimientoCaja registrarMovimiento(MovimientoCaja movimiento) {
        String sql = "INSERT INTO movimientos_caja (tipo, concepto, monto, fecha, usuario) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, movimiento.getTipo().name());
            ps.setString(2, movimiento.getConcepto());
            ps.setInt(3, movimiento.getMonto());
            ps.setString(4, movimiento.getFecha().toString());
            ps.setString(5, movimiento.getUsuario());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    movimiento.setId(rs.getInt(1));
                }
            }
            return movimiento;
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el movimiento de caja", e);
        }
    }

    public List<MovimientoCaja> obtenerMovimientosDelDia(LocalDate fecha) {
        String sql = "SELECT * FROM movimientos_caja WHERE fecha LIKE ? ORDER BY fecha DESC";
        List<MovimientoCaja> movimientos = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fecha + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(new MovimientoCaja(
                            rs.getInt("id"),
                            TipoMovimientoCaja.valueOf(rs.getString("tipo")),
                            rs.getString("concepto"),
                            rs.getInt("monto"),
                            LocalDateTime.parse(rs.getString("fecha")),
                            rs.getString("usuario")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los movimientos de caja", e);
        }
        return movimientos;
    }

    public CierreCaja guardarCierre(CierreCaja cierre) {
        String sql = "INSERT INTO cierres_caja (fecha, usuario, base_inicial, ingresos_parqueo, ingresos_lavado, " +
                "ingresos_mensualidades, ingresos_manuales, egresos_manuales, total_esperado, monto_contado, " +
                "diferencia, fecha_hora_cierre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cierre.getFecha().toString());
            ps.setString(2, cierre.getUsuario());
            ps.setInt(3, cierre.getBaseInicial());
            ps.setInt(4, cierre.getIngresosParqueo());
            ps.setInt(5, cierre.getIngresosLavado());
            ps.setInt(6, cierre.getIngresosMensualidades());
            ps.setInt(7, cierre.getIngresosManuales());
            ps.setInt(8, cierre.getEgresosManuales());
            ps.setInt(9, cierre.getTotalEsperado());
            ps.setInt(10, cierre.getMontoContado());
            ps.setInt(11, cierre.getDiferencia());
            ps.setString(12, cierre.getFechaHoraCierre().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cierre.setId(rs.getInt(1));
                }
            }
            return cierre;
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el cierre de caja", e);
        }
    }

    public List<CierreCaja> obtenerCierres() {
        String sql = "SELECT * FROM cierres_caja ORDER BY fecha_hora_cierre DESC";
        List<CierreCaja> cierres = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cierres.add(new CierreCaja(
                        rs.getInt("id"),
                        LocalDate.parse(rs.getString("fecha")),
                        rs.getString("usuario"),
                        rs.getInt("base_inicial"),
                        rs.getInt("ingresos_parqueo"),
                        rs.getInt("ingresos_lavado"),
                        rs.getInt("ingresos_mensualidades"),
                        rs.getInt("ingresos_manuales"),
                        rs.getInt("egresos_manuales"),
                        rs.getInt("total_esperado"),
                        rs.getInt("monto_contado"),
                        rs.getInt("diferencia"),
                        LocalDateTime.parse(rs.getString("fecha_hora_cierre"))));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los cierres de caja", e);
        }
        return cierres;
    }
}
