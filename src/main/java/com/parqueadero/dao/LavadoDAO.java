package com.parqueadero.dao;

import com.parqueadero.modelo.EstadoLavado;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.ServicioLavado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LavadoDAO {

    public List<ServicioLavado> obtenerServicios() {
        String sql = "SELECT * FROM servicios_lavado ORDER BY id";
        List<ServicioLavado> servicios = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                servicios.add(new ServicioLavado(rs.getInt("id"), rs.getString("nombre"), rs.getInt("precio")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los servicios de lavado", e);
        }
        return servicios;
    }

    public ServicioLavado crearServicio(String nombre, int precio) {
        String sql = "INSERT INTO servicios_lavado (nombre, precio) VALUES (?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new ServicioLavado(rs.getInt(1), nombre, precio);
                }
            }
            throw new SQLException("No se pudo obtener el id del servicio creado");
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el servicio de lavado", e);
        }
    }

    public void actualizarPrecio(int servicioId, int precio) {
        String sql = "UPDATE servicios_lavado SET precio = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, precio);
            ps.setInt(2, servicioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el precio del servicio", e);
        }
    }

    public void eliminarServicio(int servicioId) {
        String sql = "DELETE FROM servicios_lavado WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, servicioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se puede eliminar: el servicio ya tiene lavados registrados", e);
        }
    }

    public RegistroLavado insertar(RegistroLavado registro) {
        String sql = "INSERT INTO lavados (placa, servicio_id, vehiculo_id, precio, fecha_inicio, estado, " +
                "nombre_dueno, telefono_dueno, usuario_inicio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, registro.getPlaca());
            ps.setInt(2, registro.getServicio().getId());
            if (registro.getVehiculoId() != null) {
                ps.setInt(3, registro.getVehiculoId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setInt(4, registro.getPrecio());
            ps.setString(5, registro.getFechaInicio().toString());
            ps.setString(6, registro.getEstado().name());
            ps.setString(7, registro.getNombreDueno());
            ps.setString(8, registro.getTelefonoDueno());
            ps.setString(9, registro.getUsuarioInicio());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    registro.setId(rs.getInt(1));
                }
            }
            return registro;
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el lavado", e);
        }
    }

    public void completar(int id, LocalDateTime fechaFin, String usuarioFin, int montoPagado, int vuelto) {
        String sql = "UPDATE lavados SET fecha_fin = ?, estado = ?, usuario_fin = ?, monto_pagado = ?, vuelto = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fechaFin.toString());
            ps.setString(2, EstadoLavado.COMPLETADO.name());
            ps.setString(3, usuarioFin);
            ps.setInt(4, montoPagado);
            ps.setInt(5, vuelto);
            ps.setInt(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al completar el lavado", e);
        }
    }

    public RegistroLavado obtenerPorId(int id) {
        List<RegistroLavado> resultado = consultar("SELECT l.*, s.nombre AS servicio_nombre, s.precio AS servicio_precio " +
                "FROM lavados l JOIN servicios_lavado s ON l.servicio_id = s.id WHERE l.id = ?", String.valueOf(id));
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<RegistroLavado> obtenerPendientes() {
        return consultar("SELECT l.*, s.nombre AS servicio_nombre, s.precio AS servicio_precio " +
                "FROM lavados l JOIN servicios_lavado s ON l.servicio_id = s.id " +
                "WHERE l.estado = 'PENDIENTE' ORDER BY l.fecha_inicio DESC", null);
    }

    public List<RegistroLavado> obtenerPendientesPorPlaca(String placa) {
        return consultar("SELECT l.*, s.nombre AS servicio_nombre, s.precio AS servicio_precio " +
                "FROM lavados l JOIN servicios_lavado s ON l.servicio_id = s.id " +
                "WHERE l.estado = 'PENDIENTE' AND l.placa = ? ORDER BY l.fecha_inicio DESC", placa);
    }

    /** Historial de lavados completados (más recientes primero), con filtros opcionales y siempre con límite. */
    public List<RegistroLavado> obtenerHistorial(String placaContiene, LocalDate desde, int limite) {
        StringBuilder sql = new StringBuilder("SELECT l.*, s.nombre AS servicio_nombre, s.precio AS servicio_precio "
                + "FROM lavados l JOIN servicios_lavado s ON l.servicio_id = s.id WHERE l.estado = 'COMPLETADO'");
        List<Object> parametros = new ArrayList<>();
        if (placaContiene != null && !placaContiene.isBlank()) {
            sql.append(" AND l.placa LIKE ?");
            parametros.add("%" + placaContiene.trim().toUpperCase() + "%");
        }
        if (desde != null) {
            sql.append(" AND l.fecha_fin >= ?");
            parametros.add(desde.toString());
        }
        sql.append(" ORDER BY l.fecha_fin DESC LIMIT ?");
        parametros.add(limite);
        return consultarConParametros(sql.toString(), parametros);
    }

    public List<RegistroLavado> obtenerHistorialEnRango(LocalDate desde, LocalDate hasta) {
        return consultarConParametros("SELECT l.*, s.nombre AS servicio_nombre, s.precio AS servicio_precio "
                + "FROM lavados l JOIN servicios_lavado s ON l.servicio_id = s.id "
                + "WHERE l.estado = 'COMPLETADO' AND substr(l.fecha_fin, 1, 10) BETWEEN ? AND ? ORDER BY l.fecha_fin",
                List.of(desde.toString(), hasta.toString()));
    }

    public int totalEnRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(precio), 0) AS t FROM lavados "
                + "WHERE estado = 'COMPLETADO' AND substr(fecha_fin, 1, 10) BETWEEN ? AND ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("t");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al sumar los ingresos de lavado", e);
        }
    }

    private List<RegistroLavado> consultarConParametros(String sql, List<Object> parametros) {
        List<RegistroLavado> registros = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar lavados", e);
        }
        return registros;
    }

    private List<RegistroLavado> consultar(String sql, String parametro) {
        List<RegistroLavado> registros = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                if (sql.contains("l.id = ?")) {
                    ps.setInt(1, Integer.parseInt(parametro));
                } else {
                    ps.setString(1, parametro);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar lavados", e);
        }
        return registros;
    }

    private RegistroLavado mapear(ResultSet rs) throws SQLException {
        ServicioLavado servicio = new ServicioLavado(
                rs.getInt("servicio_id"), rs.getString("servicio_nombre"), rs.getInt("servicio_precio"));
        String fechaFinStr = rs.getString("fecha_fin");
        Object vehiculoIdObj = rs.getObject("vehiculo_id");
        Object montoPagadoObj = rs.getObject("monto_pagado");
        Object vueltoObj = rs.getObject("vuelto");
        return new RegistroLavado(
                rs.getInt("id"),
                rs.getString("placa"),
                servicio,
                vehiculoIdObj != null ? rs.getInt("vehiculo_id") : null,
                rs.getInt("precio"),
                LocalDateTime.parse(rs.getString("fecha_inicio")),
                fechaFinStr != null ? LocalDateTime.parse(fechaFinStr) : null,
                EstadoLavado.valueOf(rs.getString("estado")),
                rs.getString("nombre_dueno"),
                rs.getString("telefono_dueno"),
                rs.getString("usuario_inicio"),
                rs.getString("usuario_fin"),
                montoPagadoObj != null ? rs.getInt("monto_pagado") : null,
                vueltoObj != null ? rs.getInt("vuelto") : null
        );
    }
}
