package com.parqueadero.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Aquí pido el monto que el cliente entrega en efectivo para un cobro y valido que
 * alcance para cubrir el total. Devuelvo {@code null} si el cajero cancela.
 */
public final class DialogoPago {

    private DialogoPago() {
    }

    public static Integer pedirMontoPagado(Component padre, String concepto, int total) {
        Object valorInicial = String.valueOf(total);
        while (true) {
            Object entrada = JOptionPane.showInputDialog(padre,
                    concepto + "\nTotal a pagar: " + Formato.dinero(total) + "\n\nMonto recibido en efectivo:",
                    "Registrar pago", JOptionPane.QUESTION_MESSAGE, null, null, valorInicial);
            if (entrada == null) {
                return null;
            }
            // Acepto lo que el cajero escriba de forma natural: "20.000", "$20000" o "20 000".
            String texto = entrada.toString().trim();
            try {
                int monto = Formato.entero(texto);
                if (monto < 0) {
                    JOptionPane.showMessageDialog(padre, "El monto no puede ser negativo", "Error", JOptionPane.ERROR_MESSAGE);
                    valorInicial = texto;
                    continue;
                }
                if (monto < total) {
                    JOptionPane.showMessageDialog(padre, "El monto recibido (" + Formato.dinero(monto)
                            + ") es menor al total a pagar (" + Formato.dinero(total) + ")",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    valorInicial = texto;
                    continue;
                }
                return monto;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(padre, "Escribe solo números, por ejemplo 20000", "Error", JOptionPane.ERROR_MESSAGE);
                valorInicial = texto;
            }
        }
    }
}
