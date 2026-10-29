package com.parqueadero.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class TarifaDAO {

    public Map<String, Integer> obtenerTarifas() {
        return leerColumna("precio_hora");
    }

    /** Devuelvo el tope máximo que cobro por cada 24 horas, por tipo de vehículo (0 = sin tope). */
    public Map<String, Integer> obtenerTopesPorDia() {
        return leerColumna("precio_dia");
    }

    private Map<String, Integer> leerColumna(String columna) {
        Map<String, Integer> valores = new LinkedHashMap<>();
        String sql = "SELECT tipo, " + columna + " FROM tarifas_parqueo ORDER BY rowid";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                valores.put(rs.getString("tipo"), rs.getInt(columna));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las tarifas de parqueo", e);
        }
        return valores;
    }

    public void crearTipo(String tipo, int precioHora, int precioDia) {
        String sql = "INSERT INTO tarifas_parqueo (tipo, precio_hora, precio_dia) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setInt(2, precioHora);
            ps.setInt(3, precioDia);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el tipo de vehículo", e);
        }
    }

    public void eliminarTipo(String tipo) {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tarifas_parqueo WHERE tipo = ?")) {
            ps.setString(1, tipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el tipo de vehículo", e);
        }
    }

    public void actualizarTarifa(String tipo, int precioHora, int precioDia) {
        String sql = "UPDATE tarifas_parqueo SET precio_hora = ?, precio_dia = ? WHERE tipo = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, precioHora);
            ps.setInt(2, precioDia);
            ps.setString(3, tipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la tarifa de parqueo", e);
        }
    }
}
