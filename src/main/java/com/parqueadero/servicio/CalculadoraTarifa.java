package com.parqueadero.servicio;

/**
 * Aquí reúno las reglas de cobro del parqueo. No toco la base de datos, así las pruebo fácilmente:
 * <ul>
 *   <li><b>Gracia:</b> si el vehículo estuvo hasta {@code minutosGracia} minutos, no le cobro nada.</li>
 *   <li><b>Fracción:</b> cobro por bloques de {@code minutosFraccion} minutos (60 = hora o fracción;
 *       30 = media hora o fracción). Siempre cobro al menos un bloque.</li>
 *   <li><b>Tope por día:</b> si {@code precioDia > 0}, cada período de 24 horas lo cobro como máximo a ese valor,
 *       y el tiempo restante nunca lo cobro por encima de él.</li>
 * </ul>
 */
public final class CalculadoraTarifa {
    private static final int MINUTOS_POR_DIA = 24 * 60;

    private CalculadoraTarifa() {
    }

    public static int calcular(long minutos, int precioHora, int precioDia, int minutosGracia, int minutosFraccion) {
        if (minutos < 0) {
            minutos = 0;
        }
        if (minutos <= minutosGracia) {
            return 0;
        }
        int fraccion = minutosFraccion <= 0 ? 60 : minutosFraccion;
        if (precioDia <= 0) {
            return costoPorFracciones(minutos, precioHora, fraccion);
        }
        long dias = minutos / MINUTOS_POR_DIA;
        long resto = minutos % MINUTOS_POR_DIA;
        long total = dias * precioDia;
        if (dias == 0 || resto > 0) {
            total += Math.min(costoPorFracciones(resto, precioHora, fraccion), precioDia);
        }
        return (int) total;
    }

    private static int costoPorFracciones(long minutos, int precioHora, int fraccion) {
        long bloques = Math.max(1, (minutos + fraccion - 1) / fraccion);
        return (int) Math.round(bloques * precioHora * (fraccion / 60.0));
    }
}
