package com.parqueadero.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class LicenciaDAO {

    public record RegistroLicencia(String idInstalacion, LocalDate fechaInstalacion,
                                    LocalDate ultimaFechaVista, String claveActivacion) {
    }

    public RegistroLicencia obtener() {
        String sql = "SELECT * FROM licencia WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalStateException("No se encontró el registro de licencia");
            }
            return new RegistroLicencia(
                    rs.getString("id_instalacion"),
                    LocalDate.parse(rs.getString("fecha_instalacion")),
                    LocalDate.parse(rs.getString("ultima_fecha_vista")),
                    rs.getString("clave_activacion"));
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar la licencia", e);
        }
    }

    public void actualizarUltimaFechaVista(LocalDate fecha) {
        String sql = "UPDATE licencia SET ultima_fecha_vista = ? WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fecha.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la licencia", e);
        }
    }

    public void guardarClaveActivacion(String clave) {
        String sql = "UPDATE licencia SET clave_activacion = ? WHERE id = 1";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la activación", e);
        }
    }
}
