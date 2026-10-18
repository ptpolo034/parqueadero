package com.parqueadero.servicio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraTarifaTest {
    // calcular(minutos, precioHora, precioDia, minutosGracia, minutosFraccion)

    @Test
    void siempreCobraAlMenosUnaHora() {
        assertEquals(3000, CalculadoraTarifa.calcular(1, 3000, 0, 0, 60));
        assertEquals(3000, CalculadoraTarifa.calcular(60, 3000, 0, 0, 60));
    }

    @Test
    void horaOFraccion() {
        assertEquals(6000, CalculadoraTarifa.calcular(61, 3000, 0, 0, 60));
        assertEquals(9000, CalculadoraTarifa.calcular(180, 3000, 0, 0, 60));
        assertEquals(12000, CalculadoraTarifa.calcular(181, 3000, 0, 0, 60));
    }

    @Test
    void minutosDeGraciaNoCobran() {
        assertEquals(0, CalculadoraTarifa.calcular(5, 3000, 0, 10, 60));
        assertEquals(0, CalculadoraTarifa.calcular(10, 3000, 0, 10, 60));
        assertEquals(3000, CalculadoraTarifa.calcular(11, 3000, 0, 10, 60));
    }

    @Test
    void cobroPorMediaHora() {
        assertEquals(1500, CalculadoraTarifa.calcular(20, 3000, 0, 0, 30));
        assertEquals(3000, CalculadoraTarifa.calcular(31, 3000, 0, 0, 30));
        assertEquals(4500, CalculadoraTarifa.calcular(70, 3000, 0, 0, 30));
    }

    @Test
    void topePorDiaLimitaElCobro() {
        // 10 horas a 3000 serían 30000, pero el tope diario es 18000
        assertEquals(18000, CalculadoraTarifa.calcular(10 * 60, 3000, 18000, 0, 60));
        // 5 horas = 15000, por debajo del tope
        assertEquals(15000, CalculadoraTarifa.calcular(5 * 60, 3000, 18000, 0, 60));
    }

    @Test
    void cadaDiaCompletoCobraElTope() {
        assertEquals(18000, CalculadoraTarifa.calcular(24 * 60, 3000, 18000, 0, 60));
        assertEquals(36000, CalculadoraTarifa.calcular(48 * 60, 3000, 18000, 0, 60));
        // 2 días + 1 hora
        assertEquals(36000 + 3000, CalculadoraTarifa.calcular(49 * 60, 3000, 18000, 0, 60));
        // fin de semana (72 h) con tope 18000 = 54000, no 216000
        assertEquals(54000, CalculadoraTarifa.calcular(72 * 60, 3000, 18000, 0, 60));
    }

    @Test
    void sinTopeSeSigueCobrandoPorHora() {
        assertEquals(72 * 3000, CalculadoraTarifa.calcular(72 * 60, 3000, 0, 0, 60));
    }

    @Test
    void fraccionInvalidaUsaHoraCompleta() {
        assertEquals(3000, CalculadoraTarifa.calcular(30, 3000, 0, 0, 0));
    }
}
