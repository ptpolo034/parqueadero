package com.parqueadero.modelo;

import java.time.LocalDateTime;

/** Aquí guardo una línea del registro de auditoría: quién hizo qué, cuándo y (si aplica) quién lo autorizó. */
public record RegistroAuditoria(int id, LocalDateTime fechaHora, String usuario, String autorizadoPor,
                                String accion, String detalle) {
}
