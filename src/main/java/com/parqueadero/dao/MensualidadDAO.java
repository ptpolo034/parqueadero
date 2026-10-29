package com.parqueadero.dao;

import com.parqueadero.modelo.EstadoMensualidad;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.PagoMensualidad;
import com.parqueadero.modelo.TipoVehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MensualidadDAO {

    /** Creo la mensualidad y registro su primer pago en una sola transacción: o quedan las dos cosas, o ninguna. */
    public Mensualidad crearConPago(Mensualidad mensualidad, PagoMensualidad pago) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                insertarMensualidad(con, mensualidad);
                pago.setMensualidadId(mensualidad.getId());
                insertarPago(con, pago);
                con.commit();
                return mensualidad;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la mensualidad", e);
        }
    }

    /** Extiendo el vencimiento y registro el pago en una sola transacción. */
    public void renovarConPago(int id, LocalDate nuevaFechaVencimiento, PagoMensualidad pago) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE mensualidades SET fecha_vencimiento = ? WHERE id = ?")) {
                    ps.setString(1, nuevaFechaVencimiento.toString());
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                insertarPago(con, pago);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al renovar la mensualidad", e);
        }
    }

    /** Dejo la mensualidad como cancelada (no la borro) y dejo constancia en la auditoría dentro de la misma transacción. */
    public void cancelar(int id, String usuario, String autorizadoPor, String detalleAuditoria) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE mensualidades SET estado = 'CANCELADA' WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                AuditoriaDAO.registrar(con, usuario, autorizadoPor, "MENSUALIDAD_CANCELADA", detalleAuditoria);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cancelar la mensualidad", e);
        }
    }

    public Mensualidad obtenerActivaPorPlaca(String placa) {
        String sql = "SELECT * FROM mensualidades WHERE placa = ? AND estado = 'ACTIVA' " +
                "ORDER BY fecha_vencimiento DESC LIMIT 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar la mensualidad", e);
        }
    }

    public List<Mensualidad> obtenerTodas() {
        String sql = "SELECT * FROM mensualidades ORDER BY fecha_vencimiento ASC";
        List<Mensualidad> mensualidades = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                mensualidades.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las mensualidades", e);
        }
        return mensualidades;
    }

    /** Devuelvo el total cobrado en mensualidades (nuevas y renovaciones) entre dos fechas; lo sumo en la base de datos. */
    public int totalPagosEnRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(monto), 0) AS t FROM pagos_mensualidad "
                + "WHERE substr(fecha_pago, 1, 10) BETWEEN ? AND ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("t");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los pagos de mensualidad", e);
        }
    }

    private void insertarMensualidad(Connection con, Mensualidad mensualidad) throws SQLException {
        String sql = "INSERT INTO mensualidades (placa, nombre_cliente, telefono, tipo_vehiculo, monto_mensual, " +
                "fecha_inicio, fecha_vencimiento, estado, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mensualidad.getPlaca());
            ps.setString(2, mensualidad.getNombreCliente());
            ps.setString(3, mensualidad.getTelefono());
            ps.setString(4, mensualidad.getTipoVehiculo().name());
            ps.setInt(5, mensualidad.getMontoMensual());
            ps.setString(6, mensualidad.getFechaInicio().toString());
            ps.setString(7, mensualidad.getFechaVencimiento().toString());
            ps.setString(8, mensualidad.getEstado().name());
            ps.setString(9, mensualidad.getUsuarioRegistro());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    mensualidad.setId(rs.getInt(1));
                }
            }
        }
    }

    private void insertarPago(Connection con, PagoMensualidad pago) throws SQLException {
        String sql = "INSERT INTO pagos_mensualidad (mensualidad_id, placa, monto, fecha_pago, " +
                "fecha_vencimiento_nueva, usuario) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pago.getMensualidadId());
            ps.setString(2, pago.getPlaca());
            ps.setInt(3, pago.getMonto());
            ps.setString(4, pago.getFechaPago().toString());
            ps.setString(5, pago.getFechaVencimientoNueva().toString());
            ps.setString(6, pago.getUsuario());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pago.setId(rs.getInt(1));
                }
            }
        }
    }

    private Mensualidad mapear(ResultSet rs) throws SQLException {
        return new Mensualidad(
                rs.getInt("id"),
                rs.getString("placa"),
                rs.getString("nombre_cliente"),
                rs.getString("telefono"),
                TipoVehiculo.de(rs.getString("tipo_vehiculo")),
                rs.getInt("monto_mensual"),
                LocalDate.parse(rs.getString("fecha_inicio")),
                LocalDate.parse(rs.getString("fecha_vencimiento")),
                EstadoMensualidad.valueOf(rs.getString("estado")),
                rs.getString("usuario_registro"));
    }
}
