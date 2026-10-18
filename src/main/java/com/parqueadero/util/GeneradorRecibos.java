package com.parqueadero.util;

import com.parqueadero.modelo.ConfiguracionNegocio;
import com.parqueadero.modelo.Mensualidad;
import com.parqueadero.modelo.RegistroLavado;
import com.parqueadero.modelo.Vehiculo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GeneradorRecibos {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final String LINEA = "------------------------------";
    private static final String FIRMA_DESARROLLADOR = "Software desarrollado por Henry Polo";

    private GeneradorRecibos() {
    }

    public static String reciboParqueo(ConfiguracionNegocio negocio, int numeroTiquete, String placa, String tipo,
                                        LocalDateTime entrada, LocalDateTime salida, int total,
                                        String cajero, int montoPagado, int vuelto) {
        StringBuilder sb = new StringBuilder();
        encabezado(sb, negocio, numeroTiquete);
        sb.append("Placa    : ").append(placa).append("\n");
        sb.append("Entrada  : ").append(entrada.format(FMT)).append("\n");
        sb.append("Salida   : ").append(salida.format(FMT)).append("\n");
        sb.append("Duración : ").append(formatearDuracion(entrada, salida)).append("\n");
        sb.append("Tarifa   : Horas - ").append(tipo).append("\n");
        sb.append(LINEA).append("\n");
        sb.append("Total a pagar: $").append(total).append("\n");
        sb.append("Paga con     : $").append(montoPagado).append("\n");
        sb.append("Devolución   : $").append(vuelto).append("\n");
        pie(sb, negocio, cajero);
        return sb.toString();
    }

    public static String reciboLavado(ConfiguracionNegocio negocio, int numeroTiquete, String placa, String servicio,
                                       int precio, LocalDateTime inicio, LocalDateTime fin,
                                       String nombreDueno, String telefonoDueno,
                                       String cajero, int montoPagado, int vuelto) {
        StringBuilder sb = new StringBuilder();
        encabezado(sb, negocio, numeroTiquete);
        sb.append("Placa    : ").append(placa).append("\n");
        sb.append("Servicio : ").append(servicio).append("\n");
        if (nombreDueno != null && !nombreDueno.isBlank()) {
            sb.append("Cliente  : ").append(nombreDueno).append("\n");
        }
        if (telefonoDueno != null && !telefonoDueno.isBlank()) {
            sb.append("Teléfono : ").append(telefonoDueno).append("\n");
        }
        sb.append("Inicio   : ").append(inicio.format(FMT)).append("\n");
        sb.append("Fin      : ").append(fin.format(FMT)).append("\n");
        sb.append("Duración : ").append(formatearDuracion(inicio, fin)).append("\n");
        sb.append(LINEA).append("\n");
        sb.append("Total a pagar: $").append(precio).append("\n");
        sb.append("Paga con     : $").append(montoPagado).append("\n");
        sb.append("Devolución   : $").append(vuelto).append("\n");
        pie(sb, negocio, cajero);
        return sb.toString();
    }

    /** Ticket que se entrega al cliente mientras el servicio está en curso (aún no hay cobro que mostrar). */
    public static String ticketPendiente(ConfiguracionNegocio negocio, int numeroTiquete, String placa, String detalle, String cajero) {
        StringBuilder sb = new StringBuilder();
        encabezado(sb, negocio, numeroTiquete);
        sb.append("Placa: ").append(placa).append("\n");
        sb.append(detalle);
        sb.append(LINEA).append("\n");
        sb.append("Conserva este tiquete para reclamar\n");
        sb.append(LINEA).append("\n");
        sb.append("Cajero   : ").append(cajero != null ? cajero : "-").append("\n");
        sb.append("Fecha    : ").append(LocalDateTime.now().format(FMT)).append("\n");
        agregarFirma(sb);
        return sb.toString();
    }

    /** Ticket de entrada (parqueo y/o lavado y/o mensualidad nueva); los tres últimos parámetros son opcionales. */
    public static String ticketEntrada(ConfiguracionNegocio negocio, String placa, Vehiculo vehiculo,
                                       RegistroLavado lavado, Mensualidad mensualidad) {
        int numero = vehiculo != null ? vehiculo.getId() : lavado != null ? lavado.getId()
                : mensualidad != null ? mensualidad.getId() : 0;
        String cajero = vehiculo != null ? vehiculo.getUsuarioEntrada() : lavado != null ? lavado.getUsuarioInicio()
                : mensualidad != null ? mensualidad.getUsuarioRegistro() : null;
        StringBuilder sb = new StringBuilder();
        encabezado(sb, negocio, numero);
        sb.append("Placa    : ").append(placa).append("\n");
        if (mensualidad != null) {
            sb.append("Mensualidad: $").append(mensualidad.getMontoMensual())
                    .append(" - vence ").append(mensualidad.getFechaVencimiento().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (vehiculo != null) {
            sb.append("Parqueo  : ").append(vehiculo.getTipo())
                    .append(" - Entrada: ").append(vehiculo.getHoraEntrada().format(FMT)).append("\n");
            if (vehiculo.isEsMensual()) {
                sb.append("(Cubierto por mensualidad activa - sin cobro)\n");
            }
        }
        if (lavado != null) {
            sb.append("Lavado   : ").append(lavado.getServicio().getNombre()).append("\n");
        }
        sb.append(LINEA).append("\n");
        sb.append("Conserva este tiquete para reclamar\n");
        sb.append(LINEA).append("\n");
        sb.append("Cajero   : ").append(cajero != null ? cajero : "-").append("\n");
        agregarFirma(sb);
        return sb.toString();
    }

    /** Encabezado (nombre, NIT, dirección/teléfono, número de tiquete) reutilizable también en los tickets de entrada. */
    public static void encabezado(StringBuilder sb, ConfiguracionNegocio negocio, int numeroTiquete) {
        sb.append(negocio.getNombre()).append("\n");
        if (esValido(negocio.getNit())) {
            sb.append("NIT: ").append(negocio.getNit()).append("\n");
        }
        if (esValido(negocio.getRegimenTributario())) {
            sb.append(negocio.getRegimenTributario()).append("\n");
        }
        if (esValido(negocio.getDireccion()) || esValido(negocio.getTelefono())) {
            if (esValido(negocio.getDireccion())) {
                sb.append(negocio.getDireccion());
            }
            if (esValido(negocio.getTelefono())) {
                sb.append(esValido(negocio.getDireccion()) ? " - Tel: " : "Tel: ").append(negocio.getTelefono());
            }
            sb.append("\n");
        }
        sb.append("Número Tiquete: ").append(formatearNumero(numeroTiquete)).append("\n");
        sb.append(LINEA).append("\n");
    }

    public static void pie(StringBuilder sb, ConfiguracionNegocio negocio, String cajero) {
        sb.append(LINEA).append("\n");
        sb.append("Cajero   : ").append(cajero != null ? cajero : "-").append("\n");
        sb.append("Equipo   : ").append(NombreEquipo.obtener()).append("\n");
        if (esValido(negocio.getAvisoLegal())) {
            sb.append("\n").append(negocio.getAvisoLegal()).append("\n");
        }
        sb.append("\nFecha Impresión: ").append(LocalDateTime.now().format(FMT)).append("\n");
        agregarFirma(sb);
    }

    /** Línea de crédito del desarrollador del software, para tickets que no usan {@link #pie}. */
    public static void agregarFirma(StringBuilder sb) {
        sb.append(LINEA).append("\n");
        sb.append(FIRMA_DESARROLLADOR).append("\n");
    }

    public static String formatearNumero(int numero) {
        return String.format("%06d", numero);
    }

    private static String formatearDuracion(LocalDateTime desde, LocalDateTime hasta) {
        long segundosTotales = Duration.between(desde, hasta).getSeconds();
        long horas = segundosTotales / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;
        return String.format("=> HH:MM:SS %02d:%02d:%02d", horas, minutos, segundos);
    }

    private static boolean esValido(String texto) {
        return texto != null && !texto.isBlank();
    }
}
