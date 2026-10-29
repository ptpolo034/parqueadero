package com.parqueadero.servicio;

import com.parqueadero.dao.AuditoriaDAO;
import com.parqueadero.modelo.RegistroAuditoria;

import java.time.LocalDate;
import java.util.List;

/**
 * Aquí llevo la bitácora de las acciones sensibles (cancelaciones, ediciones, cambios de tarifas y usuarios...).
 * No quiero que impida la operación principal: si falla al guardar el registro, ignoro el error.
 */
public class AuditoriaService {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public void registrar(String usuario, String autorizadoPor, String accion, String detalle) {
        try {
            auditoriaDAO.registrar(usuario, autorizadoPor, accion, detalle);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void registrar(String usuario, String accion, String detalle) {
        registrar(usuario, null, accion, detalle);
    }

    public List<RegistroAuditoria> listar(LocalDate desde, String texto, int limite) {
        return auditoriaDAO.listar(desde, texto, limite);
    }
}
