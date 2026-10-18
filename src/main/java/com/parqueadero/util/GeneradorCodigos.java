package com.parqueadero.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Genera imágenes de código QR que codifican la placa de un vehículo, para imprimirlas
 * en los tickets de entrada/lavado y poder escanearlas después con un lector de
 * código de barras/QR (que funciona como si "escribiera" el texto y presionara Enter).
 */
public final class GeneradorCodigos {

    private GeneradorCodigos() {
    }

    public static BufferedImage generarQR(String contenido, int tamanoPixeles) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1);
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE,
                    tamanoPixeles, tamanoPixeles, hints);
            return MatrixToImageWriter.toBufferedImage(matriz);
        } catch (WriterException e) {
            throw new RuntimeException("No se pudo generar el código QR", e);
        }
    }
}
