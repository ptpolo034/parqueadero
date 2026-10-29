package com.parqueadero.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public final class ImpresionUtil {

    private ImpresionUtil() {
    }

    /** Imprimo el contenido visual de un componente Swing tal como se ve en pantalla. */
    public static void imprimirComponente(Component componente) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((Graphics graphics, PageFormat pageFormat, int pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            graphics.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
            componente.print(graphics);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            job.print();
        }
    }
}
