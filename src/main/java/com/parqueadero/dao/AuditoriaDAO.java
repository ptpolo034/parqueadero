package com.parqueadero.dao;

import com.parqueadero.modelo.RegistroAuditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaDAO {

    /** Inserta usando una conexión ya abierta, para que quede dentro de la misma transacción que la acción auditada. */
    public static void registrar(Connection con, String usuario, String autorizadoPor, String accion, String detalle)
            throws SQLException {
        String sql = "INSERT INTO auditoria (fecha_hora, usuario, autorizado_por, accion, detalle) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().withNano(0).toString());
            ps.setString(2, usuario);
            ps.setString(3, autorizadoPor);
            ps.setString(4, accion);
            ps.setString(5, detalle);
            ps.executeUpdate();
        }
    }

    public void registrar(String usuario, String autorizadoPor, String accion, String detalle) {
        try (Connection con = ConexionBD.obtenerConexion()) {
            registrar(con, usuario, autorizadoPor, accion, detalle);
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el registro de auditoría", e);
        }
    }

    /** Los registros más recientes, opcionalmente filtrados por fecha mínima y por texto (usuario, acción o detalle). */
    public List<RegistroAuditoria> listar(LocalDate desde, String texto, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM auditoria WHERE 1=1");
        List<Object> parametros = new ArrayList<>();
        if (desde != null) {
            sql.append(" AND fecha_hora >= ?");
            parametros.add(desde.toString());
        }
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (usuario LIKE ? OR autorizado_por LIKE ? OR accion LIKE ? OR detalle LIKE ?)");
            String patron = "%" + texto.trim() + "%";
            for (int i = 0; i < 4; i++) {
                parametros.add(patron);
            }
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        parametros.add(limite);

        List<RegistroAuditoria> registros = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registros.add(new RegistroAuditoria(rs.getInt("id"), LocalDateTime.parse(rs.getString("fecha_hora")),
                            rs.getString("usuario"), rs.getString("autorizado_por"), rs.getString("accion"),
                            rs.getString("detalle")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar la auditoría", e);
        }
        return registros;
    }
}
