package com.parqueadero.util;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compruebo la firma digital de las licencias con un par de claves efímero (así no dependo de la
 * clave real) y, si tengo mi herramienta-licencias en esta máquina, que la privada real corresponda
 * a la pública que lleva la aplicación.
 */
class ValidadorLicenciaTest {

    private static final String ID = "ABCDE-FGHJK";

    private static KeyPair parEfimero() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    /** Firmo como lo haría mi herramienta: "PARQUEADERO|" + ID en mayúsculas, y lo escribo en Base32. */
    private static String firmar(PrivateKey privada, String id) throws Exception {
        Signature firma = Signature.getInstance("Ed25519");
        firma.initSign(privada);
        firma.update(("PARQUEADERO|" + id.toUpperCase()).getBytes(StandardCharsets.UTF_8));
        return ValidadorLicencia.codificar(firma.sign());
    }

    @Test
    void aceptaLaClaveFirmadaParaSuId() throws Exception {
        KeyPair par = parEfimero();
        ValidadorLicencia validador = new ValidadorLicencia(par.getPublic());
        String clave = firmar(par.getPrivate(), ID);

        assertTrue(validador.esValida(ID, clave));
        // Acepto el ID en minúsculas o con espacios alrededor.
        assertTrue(validador.esValida("  abcde-fghjk ", clave));
    }

    @Test
    void laClaveTieneElFormatoEsperado() throws Exception {
        KeyPair par = parEfimero();
        String clave = firmar(par.getPrivate(), ID);
        // 103 caracteres Base32 en grupos de 5 dan 21 grupos y 20 guiones.
        assertTrue(clave.matches("([A-HJ-NP-Z2-9]{5}-){20}[A-HJ-NP-Z2-9]{3}"), clave);
        assertEquals(123, clave.length());
    }

    @Test
    void toleraEspaciosSaltosDeLineaYMinusculasAlPegar() throws Exception {
        KeyPair par = parEfimero();
        ValidadorLicencia validador = new ValidadorLicencia(par.getPublic());
        String clave = firmar(par.getPrivate(), ID);

        String conSaltos = clave.replace("-", " ").replaceAll("(.{20})", "$1\r\n");
        assertTrue(validador.esValida(ID, "  " + conSaltos + "\n "));
        assertTrue(validador.esValida(ID, clave.toLowerCase()));
        assertTrue(validador.esValida(ID, clave.replace("-", "")));
        assertEquals(clave, ValidadorLicencia.normalizarClave(clave.toLowerCase().replace("-", "\n")));
    }

    @Test
    void rechazaLaClaveDeOtroId() throws Exception {
        KeyPair par = parEfimero();
        ValidadorLicencia validador = new ValidadorLicencia(par.getPublic());
        String claveDeOtro = firmar(par.getPrivate(), "ZZZZZ-99999");

        assertFalse(validador.esValida(ID, claveDeOtro));
    }

    @Test
    void rechazaUnaClaveAlteradaEnCualquierCaracter() throws Exception {
        KeyPair par = parEfimero();
        ValidadorLicencia validador = new ValidadorLicencia(par.getPublic());
        String clave = firmar(par.getPrivate(), ID);

        for (int i = 0; i < clave.length(); i++) {
            if (clave.charAt(i) == '-') {
                continue;
            }
            char cambiado = clave.charAt(i) == 'A' ? 'B' : 'A';
            String alterada = clave.substring(0, i) + cambiado + clave.substring(i + 1);
            assertFalse(validador.esValida(ID, alterada), "Aceptó una clave alterada en la posición " + i);
        }
    }

    @Test
    void rechazaVaciaNulaCortaLargaYConCaracteresConfusos() throws Exception {
        KeyPair par = parEfimero();
        ValidadorLicencia validador = new ValidadorLicencia(par.getPublic());
        String clave = firmar(par.getPrivate(), ID);

        assertFalse(validador.esValida(ID, ""));
        assertFalse(validador.esValida(ID, "   \n "));
        assertFalse(validador.esValida(ID, null));
        assertFalse(validador.esValida(null, clave));
        assertFalse(validador.esValida(ID, clave.substring(0, clave.length() - 5)));
        assertFalse(validador.esValida(ID, clave + "AAAAA"));
        assertFalse(validador.esValida(ID, clave.replaceFirst("[A-Z]", "0")));
        assertFalse(validador.esValida(ID, "XXXX-XXXX-XXXX-XXXX"));
        assertNull(ValidadorLicencia.normalizarClave("hola"));
    }

    @Test
    void rechazaUnaFirmaHechaConOtraClavePrivada() throws Exception {
        ValidadorLicencia validador = new ValidadorLicencia(parEfimero().getPublic());
        String claveAjena = firmar(parEfimero().getPrivate(), ID);

        assertFalse(validador.esValida(ID, claveAjena));
    }

    @Test
    void laClavePublicaRealCorrespondeAMiPrivadaSiLaTengoEnEstaMaquina() throws Exception {
        Path privada = Path.of("herramienta-licencias", "clave-privada.txt");
        Assumptions.assumeTrue(Files.exists(privada), "No tengo la clave privada aquí; omito esta prueba");

        byte[] der = Base64.getDecoder().decode(Files.readString(privada, StandardCharsets.UTF_8).trim());
        PrivateKey clavePrivada = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(der));

        ValidadorLicencia real = ValidadorLicencia.porDefecto();
        assertTrue(real.esValida(ID, firmar(clavePrivada, ID)));
        assertFalse(real.esValida("OTRO1-OTRO2", firmar(clavePrivada, ID)));
    }
}
