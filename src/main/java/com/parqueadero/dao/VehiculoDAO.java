package com.parqueadero.dao;

import com.parqueadero.modelo.EstadoVehiculo;
import com.parqueadero.modelo.TipoVehiculo;
import com.parqueadero.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehiculoDAO {

    public boolean existeActivoPorPlaca(String placa) {
        String sql = "SELECT COUNT(*) AS c FROM vehiculos WHERE placa = ? AND estado = 'ACTIVO'";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar la placa", e);
        }
    }

    public boolean tieneLavadosAsociados(int vehiculoId) {
        String sql = "SELECT COUNT(*) AS c FROM lavados WHERE vehiculo_id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehiculoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("c") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar lavados asociados", e);
        }
    }

    /** Corrijo la placa y el tipo, y dejo constancia en la auditoría dentro de la misma transacción. */
    public void actualizarPlacaTipo(int id, String placa, TipoVehiculo tipo, String usuario, String autorizadoPor,
                                    String detalleAuditoria) {
        String sql = "UPDATE vehiculos SET placa = ?, tipo = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, placa);
                ps.setString(2, tipo.name());
                ps.setInt(3, id);
                ps.executeUpdate();
                AuditoriaDAO.registrar(con, usuario, autorizadoPor, "ENTRADA_EDITADA", detalleAuditoria);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la entrada del vehículo", e);
        }
    }

    /** Anulo la entrada (la dejo como CANCELADO, no la borro) y la registro en la auditoría. */
    public void cancelar(int id, String usuario, String autorizadoPor, String detalleAuditoria) {
        String sql = "UPDATE vehiculos SET estado = 'CANCELADO' WHERE id = ? AND estado = 'ACTIVO'";
        try (Connection con = ConexionBD.obtenerConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) {
                    throw new IllegalStateException("La entrada ya no está activa");
                }
                AuditoriaDAO.registrar(con, usuario, autorizadoPor, "ENTRADA_CANCELADA", detalleAuditoria);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cancelar la entrada del vehículo", e);
        }
    }

    public Vehiculo insertar(Vehiculo vehiculo) {
        String sql = "INSERT INTO vehiculos (placa, tipo, hora_entrada, estado, usuario_entrada, es_mensual) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vehiculo.getPlaca());
            ps.setString(2, vehiculo.getTipo().name());
            ps.setString(3, vehiculo.getHoraEntrada().toString());
            ps.setString(4, vehiculo.getEstado().name());
            ps.setString(5, vehiculo.getUsuarioEntrada());
            ps.setInt(6, vehiculo.isEsMensual() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    vehiculo.setId(rs.getInt(1));
                }
            }
            return vehiculo;
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la entrada del vehículo", e);
        }
    }

    public void registrarSalida(int id, LocalDateTime horaSalida, int totalParqueo, String usuarioSalida,
                                int montoPagado, int vuelto) {
        String sql = "UPDATE vehiculos SET hora_salida = ?, total_parqueo = ?, estado = ?, " +
                "usuario_salida = ?, monto_pagado = ?, vuelto = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, horaSalida.toString());
            ps.setInt(2, totalParqueo);
            ps.setString(3, EstadoVehiculo.FINALIZADO.name());
            ps.setString(4, usuarioSalida);
            ps.setInt(5, montoPagado);
            ps.setInt(6, vuelto);
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la salida del vehículo", e);
        }
    }

    public List<Vehiculo> obtenerActivos() {
        String sql = "SELECT * FROM vehiculos WHERE estado = 'ACTIVO' ORDER BY hora_entrada DESC";
        return consultar(sql);
    }

    /**
     * Devuelvo el historial de salidas, las más recientes primero. Todos los filtros son opcionales; siempre pongo un límite
     * para que la pantalla no se vuelva lenta cuando la base de datos acumule años de movimiento.
     */
    public List<Vehiculo> obtenerHistorial(String placaContiene, LocalDate desde, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM vehiculos WHERE estado = 'FINALIZADO'");
        List<Object> parametros = new ArrayList<>();
        if (placaContiene != null && !placaContiene.isBlank()) {
            sql.append(" AND placa LIKE ?");
            parametros.add("%" + placaContiene.trim().toUpperCase() + "%");
        }
        if (desde != null) {
            sql.append(" AND hora_salida >= ?");
            parametros.add(desde.toString());
        }
        sql.append(" ORDER BY hora_salida DESC LIMIT ?");
        parametros.add(limite);
        return consultar(sql.toString(), parametros);
    }

    public List<Vehiculo> obtenerHistorialEnRango(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT * FROM vehiculos WHERE estado = 'FINALIZADO' AND substr(hora_salida, 1, 10) BETWEEN ? AND ? "
                + "ORDER BY hora_salida", List.of(desde.toString(), hasta.toString()));
    }

    /** Devuelvo el total cobrado por parqueo en las salidas del rango; lo sumo en la base de datos, sin cargar filas. */
    public int totalEnRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(total_parqueo), 0) AS t FROM vehiculos "
                + "WHERE estado = 'FINALIZADO' AND substr(hora_salida, 1, 10) BETWEEN ? AND ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("t");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al sumar los ingresos de parqueo", e);
        }
    }

    public Vehiculo obtenerActivoPorPlaca(String placa) {
        String sql = "SELECT * FROM vehiculos WHERE placa = ? AND estado = 'ACTIVO' ORDER BY hora_entrada DESC LIMIT 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el vehículo por placa", e);
        }
    }

    public Vehiculo obtenerPorId(int id) {
        String sql = "SELECT * FROM vehiculos WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el vehículo", e);
        }
    }

    private List<Vehiculo> consultar(String sql) {
        return consultar(sql, List.of());
    }

    private List<Vehiculo> consultar(String sql, List<Object> parametros) {
        List<Vehiculo> vehiculos = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehiculos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar vehículos", e);
        }
        return vehiculos;
    }

    private Vehiculo mapear(ResultSet rs) throws SQLException {
        String horaSalidaStr = rs.getString("hora_salida");
        Object totalParqueoObj = rs.getObject("total_parqueo");
        Object montoPagadoObj = rs.getObject("monto_pagado");
        Object vueltoObj = rs.getObject("vuelto");
        return new Vehiculo(
                rs.getInt("id"),
                rs.getString("placa"),
                TipoVehiculo.de(rs.getString("tipo")),
                LocalDateTime.parse(rs.getString("hora_entrada")),
                horaSalidaStr != null ? LocalDateTime.parse(horaSalidaStr) : null,
                totalParqueoObj != null ? rs.getInt("total_parqueo") : null,
                EstadoVehiculo.valueOf(rs.getString("estado")),
                rs.getString("usuario_entrada"),
                rs.getString("usuario_salida"),
                montoPagadoObj != null ? rs.getInt("monto_pagado") : null,
                vueltoObj != null ? rs.getInt("vuelto") : null,
                rs.getInt("es_mensual") != 0
        );
    }
}
