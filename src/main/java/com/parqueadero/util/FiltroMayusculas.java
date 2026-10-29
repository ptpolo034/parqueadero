package com.parqueadero.util;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Esta clase me sirve para pasar a mayúsculas el texto de un campo mientras se escribe (por ejemplo, la placa),
 * en vez de dejar que el usuario la vea en minúscula hasta que la normalice al guardar.
 */
public final class FiltroMayusculas extends DocumentFilter {
    private static final FiltroMayusculas INSTANCIA = new FiltroMayusculas();

    private FiltroMayusculas() {
    }

    public static void aplicar(JTextField campo) {
        if (campo.getDocument() instanceof AbstractDocument documento) {
            documento.setDocumentFilter(INSTANCIA);
        }
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, text != null ? text.toUpperCase() : null, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text != null ? text.toUpperCase() : null, attrs);
    }
}
