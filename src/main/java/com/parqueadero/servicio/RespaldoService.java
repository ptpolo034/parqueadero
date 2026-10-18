package com.parqueadero.servicio;

import com.parqueadero.dao.ConexionBD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Genera copias de seguridad de la base de datos SQLite. Usa VACUUM INTO, que produce una copia consistente
 * aunque haya movimientos recientes en el archivo WAL; si eso falla, copia el archivo directamente.
 */
public class RespaldoService {
    private static final Path CARPETA_RESPALDOS = Path.of(System.getProperty("user.home"), "parqueadero-backups");
    private static final int MAX_RESPALDOS = 15;
    private static final DateTimeFormatter FMT_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path respaldarAhora() {
        return respaldarEn(CARPETA_RESPALDOS, "parqueadero_");
    }

    /** Copia la base de datos en la carpeta indicada (por ejemplo, una memoria USB o una carpeta sincronizada). */
    public Path respaldarEnCarpeta(Path carpetaDestino) {
        return respaldarEn(carpetaDestino, "parqueadero_");
    }

    private Path respaldarEn(Path carpeta, String prefijo) {
        try {
            Files.createDirectories(carpeta);
            String nombre = prefijo + LocalDateTime.now().format(FMT_ARCHIVO) + ".db";
            Path destino = carpeta.resolve(nombre);
            copiarBaseDeDatos(destino);
            if (carpeta.equals(CARPETA_RESPALDOS)) {
                limpiarRespaldosAntiguos();
            }
            return destino;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el respaldo de la base de datos", e);
        }
    }

    private void copiarBaseDeDatos(Path destino) throws IOException {
        try (Connection con = ConexionBD.obtenerConexion(); Statement stmt = con.createStatement()) {
            stmt.execute("VACUUM INTO '" + destino.toString().replace("'", "''") + "'");
        } catch (SQLException e) {
            Files.deleteIfExists(destino);
            Files.copy(ConexionBD.rutaArchivoBD(), destino, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /** Genera un respaldo automático solo si hoy todavía no se ha hecho ninguno. */
    public void respaldarSiNoExisteHoy() {
        String prefijoHoy = "parqueadero_" + LocalDate.now().toString().replace("-", "");
        boolean existeHoy;
        try {
            existeHoy = listarRespaldos().stream()
                    .anyMatch(p -> p.getFileName().toString().startsWith(prefijoHoy));
        } catch (RuntimeException e) {
            existeHoy = false;
        }
        if (!existeHoy) {
            respaldarAhora();
        }
    }

    public List<Path> listarRespaldos() {
        if (!Files.exists(CARPETA_RESPALDOS)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(CARPETA_RESPALDOS)) {
            return archivos.sorted(Comparator.comparing(Path::getFileName).reversed()).toList();
        } catch (IOException e) {
            throw new RuntimeException("Error al listar los respaldos", e);
        }
    }

    private void limpiarRespaldosAntiguos() throws IOException {
        List<Path> respaldos = listarRespaldos();
        if (respaldos.size() > MAX_RESPALDOS) {
            for (Path p : respaldos.subList(MAX_RESPALDOS, respaldos.size())) {
                Files.deleteIfExists(p);
            }
        }
    }
}
