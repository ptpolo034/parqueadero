package com.parqueadero.dao;

import com.parqueadero.modelo.ConfiguracionNegocio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfiguracionDAO {

    public ConfiguracionNegocio obtener() {
        String sql = "SELECT * FROM configuracion_negocio WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new ConfiguracionNegocio(
                        rs.getString("nombre"), rs.getString("nit"), rs.getString("direccion"),
                        rs.getString("telefono"), rs.getString("regimen_tributario"), rs.getString("aviso_legal"),
                        rs.getInt("cupos_totales"), rs.getInt("minutos_gracia"), rs.getInt("minutos_fraccion"));
            }
            return new ConfiguracionNegocio("Sistema de Parqueadero y Lavado", "", "", "", "", "", 20);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los datos del negocio", e);
        }
    }

    public void actualizar(ConfiguracionNegocio configuracion) {
        String sql = "UPDATE configuracion_negocio SET nombre = ?, nit = ?, direccion = ?, telefono = ?, " +
                "regimen_tributario = ?, aviso_legal = ?, cupos_totales = ? WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, configuracion.getNombre());
            ps.setString(2, configuracion.getNit());
            ps.setString(3, configuracion.getDireccion());
            ps.setString(4, configuracion.getTelefono());
            ps.setString(5, configuracion.getRegimenTributario());
            ps.setString(6, configuracion.getAvisoLegal());
            ps.setInt(7, configuracion.getCuposTotales());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar los datos del negocio", e);
        }
    }

    public void actualizarReglasCobro(int minutosGracia, int minutosFraccion) {
        String sql = "UPDATE configuracion_negocio SET minutos_gracia = ?, minutos_fraccion = ? WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, minutosGracia);
            ps.setInt(2, minutosFraccion);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar las reglas de cobro", e);
        }
    }
}
