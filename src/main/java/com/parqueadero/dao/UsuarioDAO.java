package com.parqueadero.dao;

import com.parqueadero.modelo.RolUsuario;
import com.parqueadero.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public int contarUsuarios() {
        return contar("SELECT COUNT(*) AS c FROM usuarios");
    }

    public int contarAdmins() {
        return contar("SELECT COUNT(*) AS c FROM usuarios WHERE rol = 'ADMIN'");
    }

    private int contar(String sql) {
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt("c");
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los usuarios", e);
        }
    }

    public void crear(String usuario, String hashContrasena, String salt, RolUsuario rol) {
        String sql = "INSERT INTO usuarios (usuario, hash_contrasena, salt, rol) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, hashContrasena);
            ps.setString(3, salt);
            ps.setString(4, rol.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ya existe un usuario con ese nombre", e);
        }
    }

    public Usuario buscarPorUsuario(String usuario) {
        String sql = "SELECT * FROM usuarios WHERE usuario = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el usuario", e);
        }
    }

    public List<Usuario> listarTodos() {
        String sql = "SELECT * FROM usuarios ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los usuarios", e);
        }
        return usuarios;
    }

    public void actualizarRol(int id, RolUsuario rol) {
        String sql = "UPDATE usuarios SET rol = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rol.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el rol del usuario", e);
        }
    }

    public void actualizarUsuario(int id, String nuevoUsuario) {
        String sql = "UPDATE usuarios SET usuario = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoUsuario);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ya existe un usuario con ese nombre", e);
        }
    }

    public void actualizarContrasena(int id, String hashContrasena, String salt) {
        String sql = "UPDATE usuarios SET hash_contrasena = ?, salt = ? WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashContrasena);
            ps.setString(2, salt);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la contraseña", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection con = ConexionBD.obtenerConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el usuario", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id"), rs.getString("usuario"),
                rs.getString("hash_contrasena"), rs.getString("salt"),
                RolUsuario.valueOf(rs.getString("rol")));
    }
}
