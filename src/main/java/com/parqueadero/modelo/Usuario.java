package com.parqueadero.modelo;

public class Usuario {
    private final int id;
    private final String usuario;
    private final String hashContrasena;
    private final String salt;
    private final RolUsuario rol;

    public Usuario(int id, String usuario, String hashContrasena, String salt, RolUsuario rol) {
        this.id = id;
        this.usuario = usuario;
        this.hashContrasena = hashContrasena;
        this.salt = salt;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public String getSalt() {
        return salt;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean esAdmin() {
        return rol == RolUsuario.ADMIN;
    }
}
