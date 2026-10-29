package com.parqueadero.ui;

import com.parqueadero.modelo.ServicioLavado;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebo los formatos de dinero y fechas que uso en todas las pantallas, y cómo leo los montos que escribe el cajero. */
class FormatoTest {

    // ---------- dinero ----------

    @Test
    void elDineroLlevaSignoYPuntoDeMiles() {
        assertEquals("$25.000", Formato.dinero(25000));
        assertEquals("$1.234.567", Formato.dinero(1234567));
    }

    @Test
    void elDineroSinMilesNoLlevaPunto() {
        assertEquals("$0", Formato.dinero(0));
        assertEquals("$999", Formato.dinero(999));
        assertEquals("$1.000", Formato.dinero(1000));
    }

    @Test
    void elDineroNegativoConservaElSignoMenos() {
        // Hoy un negativo se ve como "$-5.000"; lo dejo fijado para notar si alguien cambia ese formato.
        assertEquals("$-5.000", Formato.dinero(-5000));
    }

    @Test
    void elDineroAguantaLosExtremosDelEntero() {
        assertEquals("$2.147.483.647", Formato.dinero(Integer.MAX_VALUE));
        assertEquals("$-2.147.483.648", Formato.dinero(Integer.MIN_VALUE));
    }

    @Test
    void elDineroUsaPuntoDeMilesSinImportarLaConfiguracionRegionalDelComputador() {
        // En un Windows con región Francia o Suiza, "%,d" separa los miles con espacio o apóstrofo, no con coma.
        Locale original = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (Locale region : new Locale[]{Locale.FRANCE, new Locale("de", "CH"), Locale.US, new Locale("es", "CO")}) {
                Locale.setDefault(Locale.Category.FORMAT, region);
                assertEquals("$25.000", Formato.dinero(25000), "con la región " + region);
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    void dineroONadaDevuelveVacioSiNoHayValor() {
        assertEquals("", Formato.dineroONada(null));
    }

    @Test
    void dineroONadaFormateaSiHayValor() {
        assertEquals("$0", Formato.dineroONada(0));
        assertEquals("$15.500", Formato.dineroONada(15500));
    }

    // ---------- entero ----------

    @Test
    void enteroAceptaLasFormasNaturalesDeEscribirUnMonto() {
        assertEquals(20000, Formato.entero("20000"));
        assertEquals(20000, Formato.entero("20.000"));
        assertEquals(20000, Formato.entero("$20000"));
        assertEquals(20000, Formato.entero("$20.000"));
        assertEquals(20000, Formato.entero("20,000"));
        assertEquals(20000, Formato.entero("20 000"));
        assertEquals(20000, Formato.entero("  $ 20.000  "));
    }

    @Test
    void enteroLeeElCeroYLosNegativos() {
        assertEquals(0, Formato.entero("0"));
        // El negativo lo devuelvo tal cual: quien llama decide si lo rechaza (DialogoPago lo rechaza).
        assertEquals(-5000, Formato.entero("-5.000"));
    }

    @Test
    void enteroEsInversoDeDinero() {
        for (int valor : new int[]{0, 7, 1000, 25000, 1234567, Integer.MAX_VALUE}) {
            assertEquals(valor, Formato.entero(Formato.dinero(valor)));
        }
    }

    @Test
    void enteroRechazaElTextoVacioOSoloSimbolos() {
        assertThrows(NumberFormatException.class, () -> Formato.entero(""));
        assertThrows(NumberFormatException.class, () -> Formato.entero("   "));
        assertThrows(NumberFormatException.class, () -> Formato.entero("$"));
        assertThrows(NumberFormatException.class, () -> Formato.entero("..."));
    }

    @Test
    void enteroRechazaLetras() {
        assertThrows(NumberFormatException.class, () -> Formato.entero("veinte mil"));
        assertThrows(NumberFormatException.class, () -> Formato.entero("20k"));
        assertThrows(NumberFormatException.class, () -> Formato.entero("20.000 pesos"));
    }

    @Test
    void enteroRechazaMontosQueNoCabenEnUnEntero() {
        assertThrows(NumberFormatException.class, () -> Formato.entero("3.000.000.000"));
    }

    @Test
    void enteroTrataLaComaDecimalComoSeparadorDeMiles() {
        // Dejo fijado lo que hago hoy: en pesos no manejo centavos, así que "20,50" lo leo como 2050.
        assertEquals(2050, Formato.entero("20,50"));
    }

    // ---------- fechas ----------

    @Test
    void fechaVaConDiaMesYAnioCompleto() {
        assertEquals("05/01/2026", Formato.fecha(LocalDate.of(2026, 1, 5)));
        assertEquals("29/02/2024", Formato.fecha(LocalDate.of(2024, 2, 29)));
    }

    @Test
    void fechaHoraDeEsteAnioNoMuestraElAnio() {
        int anioActual = LocalDate.now().getYear();
        assertEquals("27/10 19:03", Formato.fechaHora(LocalDateTime.of(anioActual, 10, 27, 19, 3)));
        assertEquals("01/01 00:00", Formato.fechaHora(LocalDateTime.of(anioActual, 1, 1, 0, 0)));
        assertEquals("31/12 23:59", Formato.fechaHora(LocalDateTime.of(anioActual, 12, 31, 23, 59, 59)));
    }

    @Test
    void fechaHoraDeOtroAnioMuestraElAnioCorto() {
        int anioActual = LocalDate.now().getYear();
        String anioPasado = String.format("%02d", (anioActual - 1) % 100);
        String anioSiguiente = String.format("%02d", (anioActual + 1) % 100);
        assertEquals("31/12/" + anioPasado + " 23:59", Formato.fechaHora(LocalDateTime.of(anioActual - 1, 12, 31, 23, 59)));
        assertEquals("01/01/" + anioSiguiente + " 00:00", Formato.fechaHora(LocalDateTime.of(anioActual + 1, 1, 1, 0, 0)));
    }

    @Test
    void fechaHoraConSegundosSiempreLlevaElAnio() {
        assertEquals("27/10/26 19:03:07", Formato.fechaHoraConSegundos(LocalDateTime.of(2026, 10, 27, 19, 3, 7)));
        assertEquals("01/01/00 00:00:00", Formato.fechaHoraConSegundos(LocalDateTime.of(2000, 1, 1, 0, 0)));
    }

    @Test
    void lasFechasNulasLanzanError() {
        // Quien llama debe evitar pasar nulos (por ejemplo, un vehículo que no ha salido); aquí no los tapo.
        assertThrows(NullPointerException.class, () -> Formato.fecha(null));
        assertThrows(NullPointerException.class, () -> Formato.fechaHora(null));
        assertThrows(NullPointerException.class, () -> Formato.fechaHoraConSegundos(null));
    }

    // ---------- lista de servicios ----------

    @Test
    void laListaDeServiciosMuestraNombreYPrecioFormateado() {
        JComboBox<ServicioLavado> combo = new JComboBox<>();
        Formato.conPreciosFormateados(combo);
        ServicioLavado servicio = new ServicioLavado(1, "Lavado básico", 15000);
        JLabel celda = (JLabel) combo.getRenderer()
                .getListCellRendererComponent(new JList<>(), servicio, 0, false, false);
        assertEquals("Lavado básico - $15.000", celda.getText());
    }

    @Test
    void laListaDeServiciosMuestraOtrosValoresSinCambios() {
        JComboBox<ServicioLavado> combo = new JComboBox<>();
        Formato.conPreciosFormateados(combo);
        JLabel celda = (JLabel) combo.getRenderer().getListCellRendererComponent(new JList<>(), null, -1, false, false);
        assertEquals("", celda.getText());
    }
}
