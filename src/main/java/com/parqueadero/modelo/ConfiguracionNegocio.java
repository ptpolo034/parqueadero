package com.parqueadero.modelo;

public class ConfiguracionNegocio {
    private String nombre;
    private String nit;
    private String direccion;
    private String telefono;
    private String regimenTributario;
    private String avisoLegal;
    private int cuposTotales;

    public ConfiguracionNegocio(String nombre, String nit, String direccion, String telefono,
                                String regimenTributario, String avisoLegal, int cuposTotales) {
        this.nombre = nombre;
        this.nit = nit;
        this.direccion = direccion;
        this.telefono = telefono;
        this.regimenTributario = regimenTributario;
        this.avisoLegal = avisoLegal;
        this.cuposTotales = cuposTotales;
    }

    private int minutosGracia;
    private int minutosFraccion = 60;

    public ConfiguracionNegocio(String nombre, String nit, String direccion, String telefono,
                                String regimenTributario, String avisoLegal, int cuposTotales,
                                int minutosGracia, int minutosFraccion) {
        this(nombre, nit, direccion, telefono, regimenTributario, avisoLegal, cuposTotales);
        this.minutosGracia = minutosGracia;
        this.minutosFraccion = minutosFraccion;
    }

    /** Minutos iniciales sin costo (0 = no hay tiempo de gracia). */
    public int getMinutosGracia() {
        return minutosGracia;
    }

    /** Cada cuántos minutos se cobra: 60 = hora completa o fracción; 30 = media hora o fracción, etc. */
    public int getMinutosFraccion() {
        return minutosFraccion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNit() {
        return nit;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getRegimenTributario() {
        return regimenTributario;
    }

    public String getAvisoLegal() {
        return avisoLegal;
    }

    public int getCuposTotales() {
        return cuposTotales;
    }
}
