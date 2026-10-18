package com.parqueadero.dao;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class ConexionBD {
    private static final Path RUTA_BD_PATH = Path.of(System.getProperty("user.home"), "parqueadero.db");
    private static final String URL = "jdbc:sqlite:" + RUTA_BD_PATH;

    private ConexionBD() {
    }

    public static Path rutaArchivoBD() {
        return RUTA_BD_PATH;
    }

    public static Connection obtenerConexion() throws SQLException {
        Connection conexion = DriverManager.getConnection(URL);
        try (Statement stmt = conexion.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            // Si otra operación tiene la base ocupada, se espera un momento en vez de fallar de inmediato.
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA synchronous = NORMAL");
        }
        return conexion;
    }

    public static void inicializar() {
        String sqlVehiculos = """
                CREATE TABLE IF NOT EXISTS vehiculos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    placa TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    hora_entrada TEXT NOT NULL,
                    hora_salida TEXT,
                    total_parqueo INTEGER,
                    estado TEXT NOT NULL DEFAULT 'ACTIVO',
                    es_mensual INTEGER NOT NULL DEFAULT 0
                )
                """;
        String sqlServicios = """
                CREATE TABLE IF NOT EXISTS servicios_lavado (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    precio INTEGER NOT NULL
                )
                """;
        String sqlLavados = """
                CREATE TABLE IF NOT EXISTS lavados (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    placa TEXT NOT NULL,
                    servicio_id INTEGER NOT NULL REFERENCES servicios_lavado(id),
                    vehiculo_id INTEGER REFERENCES vehiculos(id),
                    precio INTEGER NOT NULL,
                    fecha_inicio TEXT NOT NULL,
                    fecha_fin TEXT,
                    estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                    nombre_dueno TEXT,
                    telefono_dueno TEXT
                )
                """;
        String sqlTarifas = """
                CREATE TABLE IF NOT EXISTS tarifas_parqueo (
                    tipo TEXT PRIMARY KEY,
                    precio_hora INTEGER NOT NULL
                )
                """;
        String sqlUsuarios = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT NOT NULL UNIQUE,
                    hash_contrasena TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    rol TEXT NOT NULL DEFAULT 'CAJERO'
                )
                """;
        String sqlConfiguracion = """
                CREATE TABLE IF NOT EXISTS configuracion_negocio (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    nombre TEXT NOT NULL,
                    nit TEXT,
                    direccion TEXT,
                    telefono TEXT,
                    regimen_tributario TEXT,
                    aviso_legal TEXT,
                    cupos_totales INTEGER NOT NULL DEFAULT 20
                )
                """;
        String sqlLicencia = """
                CREATE TABLE IF NOT EXISTS licencia (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    id_instalacion TEXT NOT NULL,
                    fecha_instalacion TEXT NOT NULL,
                    ultima_fecha_vista TEXT NOT NULL,
                    clave_activacion TEXT
                )
                """;
        String sqlMovimientosCaja = """
                CREATE TABLE IF NOT EXISTS movimientos_caja (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo TEXT NOT NULL,
                    concepto TEXT NOT NULL,
                    monto INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    usuario TEXT
                )
                """;
        String sqlCierresCaja = """
                CREATE TABLE IF NOT EXISTS cierres_caja (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha TEXT NOT NULL,
                    usuario TEXT,
                    base_inicial INTEGER NOT NULL,
                    ingresos_parqueo INTEGER NOT NULL,
                    ingresos_lavado INTEGER NOT NULL,
                    ingresos_manuales INTEGER NOT NULL,
                    egresos_manuales INTEGER NOT NULL,
                    total_esperado INTEGER NOT NULL,
                    monto_contado INTEGER NOT NULL,
                    diferencia INTEGER NOT NULL,
                    fecha_hora_cierre TEXT NOT NULL,
                    ingresos_mensualidades INTEGER NOT NULL DEFAULT 0
                )
                """;
        String sqlMensualidades = """
                CREATE TABLE IF NOT EXISTS mensualidades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    placa TEXT NOT NULL,
                    nombre_cliente TEXT,
                    telefono TEXT,
                    tipo_vehiculo TEXT NOT NULL,
                    monto_mensual INTEGER NOT NULL,
                    fecha_inicio TEXT NOT NULL,
                    fecha_vencimiento TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'ACTIVA',
                    usuario_registro TEXT
                )
                """;
        String sqlAuditoria = """
                CREATE TABLE IF NOT EXISTS auditoria (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha_hora TEXT NOT NULL,
                    usuario TEXT,
                    autorizado_por TEXT,
                    accion TEXT NOT NULL,
                    detalle TEXT
                )
                """;

        String sqlPagosMensualidad = """
                CREATE TABLE IF NOT EXISTS pagos_mensualidad (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mensualidad_id INTEGER NOT NULL REFERENCES mensualidades(id),
                    placa TEXT NOT NULL,
                    monto INTEGER NOT NULL,
                    fecha_pago TEXT NOT NULL,
                    fecha_vencimiento_nueva TEXT NOT NULL,
                    usuario TEXT
                )
                """;

        try (Connection conexion = obtenerConexion(); Statement stmt = conexion.createStatement()) {
            stmt.execute(sqlVehiculos);
            stmt.execute(sqlServicios);
            stmt.execute(sqlLavados);
            stmt.execute(sqlTarifas);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlConfiguracion);
            stmt.execute(sqlLicencia);
            stmt.execute(sqlMovimientosCaja);
            stmt.execute(sqlCierresCaja);
            stmt.execute(sqlMensualidades);
            stmt.execute(sqlPagosMensualidad);


            // Migración: mensualidades (versiones anteriores no tenían la columna en vehiculos).
            if (!tieneColumna(stmt, "vehiculos", "es_mensual")) {
                stmt.execute("ALTER TABLE vehiculos ADD COLUMN es_mensual INTEGER NOT NULL DEFAULT 0");
            }
            if (!tieneColumna(stmt, "cierres_caja", "ingresos_mensualidades")) {
                stmt.execute("ALTER TABLE cierres_caja ADD COLUMN ingresos_mensualidades INTEGER NOT NULL DEFAULT 0");
            }

            // Migración: si la tabla usuarios ya existía de una versión anterior sin la columna "rol", se agrega.
            if (!tieneColumna(stmt, "usuarios", "rol")) {
                stmt.execute("ALTER TABLE usuarios ADD COLUMN rol TEXT NOT NULL DEFAULT 'CAJERO'");
            }
            // Migración: datos del dueño del vehículo en los lavados (versiones anteriores no la tenían).
            if (!tieneColumna(stmt, "lavados", "nombre_dueno")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN nombre_dueno TEXT");
            }
            if (!tieneColumna(stmt, "lavados", "telefono_dueno")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN telefono_dueno TEXT");
            }
            // Migración: quién atendió cada operación y el pago recibido (para el formato de tiquete completo).
            if (!tieneColumna(stmt, "vehiculos", "usuario_entrada")) {
                stmt.execute("ALTER TABLE vehiculos ADD COLUMN usuario_entrada TEXT");
            }
            if (!tieneColumna(stmt, "vehiculos", "usuario_salida")) {
                stmt.execute("ALTER TABLE vehiculos ADD COLUMN usuario_salida TEXT");
            }
            if (!tieneColumna(stmt, "vehiculos", "monto_pagado")) {
                stmt.execute("ALTER TABLE vehiculos ADD COLUMN monto_pagado INTEGER");
            }
            if (!tieneColumna(stmt, "vehiculos", "vuelto")) {
                stmt.execute("ALTER TABLE vehiculos ADD COLUMN vuelto INTEGER");
            }
            if (!tieneColumna(stmt, "lavados", "usuario_inicio")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN usuario_inicio TEXT");
            }
            if (!tieneColumna(stmt, "lavados", "usuario_fin")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN usuario_fin TEXT");
            }
            if (!tieneColumna(stmt, "lavados", "monto_pagado")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN monto_pagado INTEGER");
            }
            if (!tieneColumna(stmt, "lavados", "vuelto")) {
                stmt.execute("ALTER TABLE lavados ADD COLUMN vuelto INTEGER");
            }
            // Migración: régimen tributario / nota fiscal para los tiquetes (versiones anteriores no la tenían).
            if (!tieneColumna(stmt, "configuracion_negocio", "regimen_tributario")) {
                stmt.execute("ALTER TABLE configuracion_negocio ADD COLUMN regimen_tributario TEXT");
            }
            // Migración: cupos totales de parqueo configurables (antes era un valor fijo en el código).
            if (!tieneColumna(stmt, "configuracion_negocio", "cupos_totales")) {
                stmt.execute("ALTER TABLE configuracion_negocio ADD COLUMN cupos_totales INTEGER NOT NULL DEFAULT 20");
            }
            stmt.execute(sqlAuditoria);

            // Reglas de cobro configurables: tope por día, minutos de gracia y fracción de cobro.
            if (!tieneColumna(stmt, "tarifas_parqueo", "precio_dia")) {
                stmt.execute("ALTER TABLE tarifas_parqueo ADD COLUMN precio_dia INTEGER NOT NULL DEFAULT 0");
            }
            if (!tieneColumna(stmt, "configuracion_negocio", "minutos_gracia")) {
                stmt.execute("ALTER TABLE configuracion_negocio ADD COLUMN minutos_gracia INTEGER NOT NULL DEFAULT 0");
            }
            if (!tieneColumna(stmt, "configuracion_negocio", "minutos_fraccion")) {
                stmt.execute("ALTER TABLE configuracion_negocio ADD COLUMN minutos_fraccion INTEGER NOT NULL DEFAULT 60");
            }

            // WAL: más resistente a cortes de luz y permite leer mientras se escribe (el modo se guarda en el archivo).
            stmt.execute("PRAGMA journal_mode = WAL");
            // Índices para que las búsquedas por placa/estado/fecha sigan siendo rápidas con años de historia.
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_vehiculos_placa_estado ON vehiculos(placa, estado)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_vehiculos_estado_salida ON vehiculos(estado, hora_salida)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lavados_estado_placa ON lavados(estado, placa)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lavados_estado_fin ON lavados(estado, fecha_fin)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mensualidades_placa ON mensualidades(placa, estado)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_mensualidad_fecha ON pagos_mensualidad(fecha_pago)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_caja_fecha ON movimientos_caja(fecha)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha_hora)");

            // Si ningún usuario tiene rol ADMIN (por ejemplo, tras la migración anterior), se asciende
            // al usuario más antiguo para que la cuenta original siga teniendo control total.
            stmt.execute("""
                    UPDATE usuarios SET rol = 'ADMIN'
                    WHERE id = (SELECT MIN(id) FROM usuarios)
                    AND NOT EXISTS (SELECT 1 FROM usuarios WHERE rol = 'ADMIN')
                    """);

            var rsServicios = stmt.executeQuery("SELECT COUNT(*) AS c FROM servicios_lavado");
            rsServicios.next();
            if (rsServicios.getInt("c") == 0) {
                stmt.execute("""
                        INSERT INTO servicios_lavado (nombre, precio) VALUES
                        ('Lavado básico - carro', 15000),
                        ('Lavado completo - carro', 25000),
                        ('Lavado básico - moto', 8000),
                        ('Lavado completo - moto', 12000)
                        """);
            }

            var rsTarifas = stmt.executeQuery("SELECT COUNT(*) AS c FROM tarifas_parqueo");
            rsTarifas.next();
            if (rsTarifas.getInt("c") == 0) {
                stmt.execute("""
                        INSERT INTO tarifas_parqueo (tipo, precio_hora) VALUES
                        ('CARRO', 3000),
                        ('MOTO', 1500),
                        ('BICICLETA', 1000)
                        """);
            }

            var rsConfig = stmt.executeQuery("SELECT COUNT(*) AS c FROM configuracion_negocio");
            rsConfig.next();
            if (rsConfig.getInt("c") == 0) {
                PreparedStatement ps = conexion.prepareStatement(
                        "INSERT INTO configuracion_negocio (id, nombre, nit, direccion, telefono, regimen_tributario, aviso_legal) " +
                                "VALUES (1, ?, '', '', '', '', ?)");
                ps.setString(1, "Sistema de Parqueadero y Lavado");
                ps.setString(2, "Después de retirado el vehículo no se responde por daños ocasionados.");
                ps.executeUpdate();
                ps.close();
            }

            var rsLicencia = stmt.executeQuery("SELECT COUNT(*) AS c FROM licencia");
            rsLicencia.next();
            if (rsLicencia.getInt("c") == 0) {
                String hoy = LocalDate.now().toString();
                PreparedStatement ps = conexion.prepareStatement(
                        "INSERT INTO licencia (id, id_instalacion, fecha_instalacion, ultima_fecha_vista, clave_activacion) " +
                                "VALUES (1, ?, ?, ?, NULL)");
                ps.setString(1, generarIdInstalacion());
                ps.setString(2, hoy);
                ps.setString(3, hoy);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    private static String generarIdInstalacion() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O ni 1/I, para evitar confusiones al dictarlo
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                sb.append('-');
            }
            sb.append(alfabeto.charAt(random.nextInt(alfabeto.length())));
        }
        return sb.toString();
    }

    private static boolean tieneColumna(Statement stmt, String tabla, String columna) throws SQLException {
        try (var rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
