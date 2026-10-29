# Sistema de Parqueadero y Lavado (escritorio)

Hice este programa de escritorio en Java (Swing) para llevar un parqueadero con servicio de lavado en un solo computador.
Los datos quedan en una base SQLite local, así que no necesita internet ni servidor.

## Qué hace

- Registra entradas y salidas de vehículos. Desde la pantalla de inicio escribo o escaneo la placa y el sistema me
  muestra qué puedo hacer con ese vehículo (cobrar la salida, registrar la entrada, agregar un lavado, editar o cancelar).
- Calcula el cobro por tiempo según el tipo de vehículo, con minutos de gracia, cobro por fracciones y tope por día.
  Los tipos de vehículo y sus tarifas los defino yo en Tarifas.
- Maneja servicios de lavado, con o sin parqueo.
- Maneja clientes con mensualidad: mientras la mensualidad está vigente no se les cobra parqueo, pero ocupan su cupo.
- Tiene caja: movimientos de dinero aparte y cierre del día.
- Imprime tickets y recibos con código QR de la placa, para escanearlo a la salida.
- Tiene usuarios con rol de administrador o cajero. Para editar o cancelar algo, el cajero necesita que un administrador
  lo autorice, y todo queda en la auditoría.
- Hace respaldos automáticos de la base de datos y permite copiarlos a una USB.
- Tiene tema claro y oscuro.
- Funciona 30 días de prueba y después pide un código de activación.

## Cómo lo compilo y lo ejecuto

Necesito JDK 17 o superior y Maven 3.8+.

```bash
mvn clean package          # compila, corre las pruebas y genera target/parqueadero.jar
java -jar target/parqueadero.jar
```

Los datos se guardan en `parqueadero.db` y los respaldos en `parqueadero-backups`, ambos en mi carpeta de usuario.

## Instalador para Windows

`generar-instalador.ps1` arma el instalador `.exe` con `jpackage` (necesita WiX 3). Antes de generarlo subo el número
de versión que está dentro del script.

## Licencias

Las claves de activación son firmas digitales Ed25519. El programa solo lleva la clave pública (`util/ValidadorLicencia.java`);
la privada y la herramienta que firma las claves viven aparte, en una carpeta que no subo a este repositorio.

## Estructura

```
src/main/java/com/parqueadero/
  Main.java                  punto de entrada
  modelo/                    clases de dominio
  dao/                       acceso a datos (JDBC + SQLite)
  servicio/                  lógica de negocio (cobro, caja, mensualidades, auditoría, licencia, respaldos)
  ui/                        interfaz gráfica (Swing + FlatLaf)
  util/                      tickets, códigos QR, contraseñas, licencias
src/test/                    pruebas automáticas (cobro, flujo completo con base temporal, contraseñas)
```
