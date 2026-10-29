package com.parqueadero.servicio;

import com.parqueadero.BaseTemporalPruebas;
import com.parqueadero.util.ValidadorLicencia;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebo la activación completa contra una base temporal y un par de claves efímero (nunca toco la base real). */
class LicenciaServiceTest {

    @BeforeAll
    static void prepararBaseTemporal() {
        BaseTemporalPruebas.preparar();
    }

    private static String firmar(KeyPair par, String id) throws Exception {
        Signature firma = Signature.getInstance("Ed25519");
        firma.initSign(par.getPrivate());
        firma.update(("PARQUEADERO|" + id).getBytes(StandardCharsets.UTF_8));
        return ValidadorLicencia.codificar(firma.sign());
    }

    @Test
    void activaSoloConLaClaveDeEsteEquipoYAceptaLaClavePegadaConRuido() throws Exception {
        KeyPair par = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        LicenciaService servicio = new LicenciaService(new ValidadorLicencia(par.getPublic()));
        String id = servicio.idInstalacion();

        assertFalse(servicio.estaActivada());
        assertThrows(IllegalArgumentException.class, () -> servicio.activar(""));
        assertThrows(IllegalArgumentException.class, () -> servicio.activar("XXXX-XXXX-XXXX-XXXX"));
        assertThrows(IllegalArgumentException.class, () -> servicio.activar(firmar(par, "OTRO1-OTRO2")));
        assertFalse(servicio.estaActivada());

        // La pego en minúsculas y partida en varias líneas, como la copiaría el cliente desde un chat.
        String pegada = firmar(par, id).toLowerCase().replaceAll("(.{30})", "$1\n  ");
        servicio.activar(pegada);
        assertTrue(servicio.estaActivada());

        // Con mi clave pública de producción esa firma efímera no vale: la licencia no cuenta como activada.
        assertFalse(new LicenciaService().estaActivada());
    }
}
