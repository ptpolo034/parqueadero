package com.parqueadero.ui;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebo cómo reparto el ancho de la tabla entre las columnas. Recuerdo las medidas:
 * duro = ancho del título (nunca lo corto), blando = ancho completo si la columna es corta (fecha, placa, dinero)
 * o el del título si es de texto largo, preferido = ancho del contenido más ancho.
 */
class TablasRepartirTest {

    private static int suma(int[] anchos) {
        return Arrays.stream(anchos).sum();
    }

    /** Verifico que ninguna columna quede más angosta que su mínimo. */
    private static void assertNoBajaDe(int[] minimo, int[] ancho) {
        for (int i = 0; i < ancho.length; i++) {
            assertTrue(ancho[i] >= minimo[i], "la columna " + i + " quedó en " + ancho[i] + " y su mínimo es " + minimo[i]);
        }
    }

    // ---------- cuando todo cabe ----------

    @Test
    void siCabeExactoCadaColumnaQuedaEnSuPreferido() {
        int[] preferido = {100, 50, 150};
        int[] ancho = Tablas.repartir(300, new int[]{40, 30, 60}, new int[]{40, 50, 60}, preferido);
        assertArrayEquals(preferido, ancho);
    }

    @Test
    void loQueSobraLoRepartoEnProporcionAlPreferido() {
        int[] ancho = Tablas.repartir(600, new int[]{40, 30, 60}, new int[]{40, 50, 60}, new int[]{100, 50, 150});
        assertArrayEquals(new int[]{200, 100, 300}, ancho);
    }

    @Test
    void conSobranteNoDivisibleLosPixelesQueQuedanVanALaUltimaColumna() {
        int[] ancho = Tablas.repartir(101, new int[]{10, 10, 10}, new int[]{10, 10, 10}, new int[]{30, 30, 30});
        assertEquals(101, suma(ancho));
        assertArrayEquals(new int[]{33, 33, 35}, ancho);
    }

    @Test
    void unaSolaColumnaOcupaTodoElAncho() {
        assertArrayEquals(new int[]{500}, Tablas.repartir(500, new int[]{40}, new int[]{40}, new int[]{120}));
    }

    @Test
    void conAnchosEnormesNoSeDesbordaLaCuenta() {
        // Multiplico sobrante por preferido en long; con int esto daría negativo.
        int[] ancho = Tablas.repartir(2_000_000_000, new int[]{10, 10}, new int[]{10, 10}, new int[]{100_000, 100_000});
        assertEquals(2_000_000_000, suma(ancho));
        assertArrayEquals(new int[]{1_000_000_000, 1_000_000_000}, ancho);
    }

    // ---------- cuando no cabe el preferido pero sí el blando ----------

    @Test
    void lasColumnasCortasConservanSuAnchoYLasLargasSeAchican() {
        // Columna 0: placa (corta, 80). Columna 1: concepto (larga, título 60, contenido 400). Columna 2: dinero (corta, 90).
        int[] duro = {50, 60, 70};
        int[] blando = {80, 60, 90};
        int[] preferido = {80, 400, 90};
        int[] ancho = Tablas.repartir(300, duro, blando, preferido);
        assertArrayEquals(new int[]{80, 130, 90}, ancho);
    }

    @Test
    void unaColumnaLargaQueCabeEnSuParteSeQuedaEnSuPreferido() {
        // La columna 0 es larga pero solo pide 60, menos que su parte (150): la dejo en 60 y el resto va a la columna 1.
        int[] ancho = Tablas.repartir(300, new int[]{30, 40}, new int[]{30, 40}, new int[]{60, 500});
        assertArrayEquals(new int[]{60, 240}, ancho);
    }

    @Test
    void elRepartoEntreColumnasLargasEsEquitativo() {
        int[] ancho = Tablas.repartir(300, new int[]{40, 40, 40}, new int[]{40, 40, 40}, new int[]{500, 500, 500});
        assertArrayEquals(new int[]{100, 100, 100}, ancho);
    }

    @Test
    void siSoloCabeElBlandoExactoCadaColumnaQuedaEnSuBlando() {
        int[] blando = {80, 60, 90};
        int[] ancho = Tablas.repartir(230, new int[]{50, 60, 70}, blando, new int[]{80, 400, 90});
        assertArrayEquals(blando, ancho);
    }

    @Test
    void elRepartoEquitativoNoSePasaDelAnchoDisponible() {
        // Error que encontré: la columna 0 (larga, pide 140) cabía en su parte (150) y la fijé en 140, pero entonces
        // a la columna 1 solo le quedaban 160 y su título mide 170: le daba 170 y la tabla medía 310 de 300.
        // Lo correcto es achicar la columna 0 a 130 para que el título de la columna 1 quepa completo.
        int[] duro = {40, 170};
        int[] blando = {40, 170};
        int[] preferido = {140, 400};
        int[] ancho = Tablas.repartir(300, duro, blando, preferido);
        assertEquals(300, suma(ancho), "la suma de las columnas debe ser el ancho disponible");
        assertNoBajaDe(duro, ancho);
        assertArrayEquals(new int[]{130, 170}, ancho);
    }

    @Test
    void elRepartoEquitativoUsaTodosLosPixelesDisponibles() {
        // 301 píxeles entre tres columnas largas: no debe quedar una franja vacía a la derecha de la tabla.
        int[] ancho = Tablas.repartir(301, new int[]{40, 40, 40}, new int[]{40, 40, 40}, new int[]{500, 500, 500});
        assertEquals(301, suma(ancho));
        assertNoBajaDe(new int[]{40, 40, 40}, ancho);
    }

    @Test
    void enElRepartoEquitativoNingunaColumnaPasaDeSuPreferidoNiBajaDeSuBlando() {
        int[] duro = {30, 45, 20, 60, 35};
        int[] blando = {30, 90, 20, 60, 70};
        int[] preferido = {200, 90, 55, 300, 70};
        int sumaBlando = suma(blando);
        int sumaPreferido = suma(preferido);
        for (int disponible = sumaBlando; disponible < sumaPreferido; disponible++) {
            int[] ancho = Tablas.repartir(disponible, duro, blando, preferido);
            assertEquals(disponible, suma(ancho), "con " + disponible + " píxeles");
            for (int i = 0; i < ancho.length; i++) {
                assertTrue(ancho[i] >= blando[i] && ancho[i] <= preferido[i],
                        "con " + disponible + " píxeles la columna " + i + " quedó en " + ancho[i]);
            }
        }
    }

    // ---------- cuando ni el blando cabe ----------

    @Test
    void sinEspacioParaElBlandoAchicoProporcionalmenteHastaElTitulo() {
        // duro suma 100 y blando 200; con 150 me quedo justo en la mitad del camino.
        int[] ancho = Tablas.repartir(150, new int[]{50, 50}, new int[]{100, 100}, new int[]{100, 300});
        assertArrayEquals(new int[]{75, 75}, ancho);
    }

    @Test
    void sinEspacioNiParaLosTitulosNoBajoDelTitulo() {
        // Prefiero que la tabla se desborde a cortar un título: cada columna queda en su duro.
        int[] duro = {50, 60, 70};
        int[] ancho = Tablas.repartir(100, duro, new int[]{80, 60, 90}, new int[]{80, 400, 90});
        assertArrayEquals(duro, ancho);
    }

    @Test
    void sinEspacioYBlandoIgualAlDuroElSobranteVaALaUltima() {
        // Si blando y duro coinciden no hay nada que achicar en proporción; lo que sobre lo doy a la última columna.
        int[] duro = {50, 50};
        int[] ancho = Tablas.repartir(99, duro, new int[]{50, 50}, new int[]{100, 300});
        assertArrayEquals(duro, ancho);
    }

    @Test
    void sinEspacioParaElBlandoLaSumaEsElDisponibleSiAlcanzanLosTitulos() {
        int[] duro = {33, 41, 27};
        int[] blando = {70, 95, 61};
        int[] preferido = {70, 300, 61};
        for (int disponible = suma(duro); disponible < suma(blando); disponible++) {
            int[] ancho = Tablas.repartir(disponible, duro, blando, preferido);
            assertEquals(disponible, suma(ancho), "con " + disponible + " píxeles");
            assertNoBajaDe(duro, ancho);
        }
    }

    @Test
    void conCeroPixelesCadaColumnaQuedaEnSuTitulo() {
        int[] duro = {20, 30};
        assertArrayEquals(duro, Tablas.repartir(0, duro, new int[]{40, 30}, new int[]{40, 90}));
    }
}
