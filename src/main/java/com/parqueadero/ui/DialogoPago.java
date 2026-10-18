package com.parqueadero.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Pide el monto que el cliente entrega en efectivo para un cobro, validando que
 * alcance para cubrir el total. Devuelve {@code null} si el cajero cancela.
 */
public final class DialogoPago {

    private DialogoPago() {
    }

    public static Integer pedirMontoPagado(Component padre, String concepto, int total) {
        Object valorInicial = String.valueOf(total);
        while (true) {
            Object entrada = JOptionPane.showInputDialog(padre,
                    concepto + "\nTotal a pagar: $" + total + "\n\nMonto recibido en efectivo:",
                    "Registrar pago", JOptionPane.QUESTION_MESSAGE, null, null, valorInicial);
            if (entrada == null) {
                return null;
            }
            String texto = entrada.toString().trim();
            try {
                int monto = Integer.parseInt(texto);
                if (monto < 0) {
                    JOptionPane.showMessageDialog(padre, "El monto no puede ser negativo", "Error", JOptionPane.ERROR_MESSAGE);
                    valorInicial = texto;
                    continue;
                }
                if (monto < total) {
                    JOptionPane.showMessageDialog(padre, "El monto recibido es menor al total a pagar",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    valorInicial = texto;
                    continue;
                }
                return monto;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(padre, "Ingresa un número válido", "Error", JOptionPane.ERROR_MESSAGE);
                valorInicial = texto;
            }
        }
    }
}
