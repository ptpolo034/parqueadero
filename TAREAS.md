# Tareas de Parqueadero (escritorio)

<!-- AVANCE:INICIO -->
**Avance general: ████████░░ 80%**

| Tarea | Avance |
|---|---|
| Entradas, salidas y cobro | ██████████ 100% |
| Lavado y mensualidades | ██████████ 100% |
| Caja, tickets y usuarios | ██████████ 100% |
| Instalación, respaldos y licencia | ██████████ 100% |
| Pendientes | ░░░░░░░░░░ 0% |
<!-- AVANCE:FIN -->

Aquí llevo el paso a paso del programa de escritorio: lo que ya hice y lo que me falta. Cada vez que trabajo en el
proyecto empiezo por esta lista y marco lo terminado con `[x]`.

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso

El porcentaje de cada sección se calcula solo con las casillas marcadas; las que están en curso todavía no suman.

---

## Entradas, salidas y cobro (100%)
- [x] Registrar entradas y salidas desde la pantalla de inicio, escribiendo o escaneando la placa
- [x] Cobro por tiempo según el tipo de vehículo, con minutos de gracia, fracciones y tope por día
- [x] Tipos de vehículo y tarifas definidos desde Tarifas
- [x] Editar o cancelar con autorización de un administrador, con todo en la auditoría

## Lavado y mensualidades (100%)
- [x] Servicios de lavado, con o sin parqueo
- [x] Clientes con mensualidad: sin cobro de parqueo mientras esté vigente, pero ocupan su cupo

## Caja, tickets y usuarios (100%)
- [x] Caja: movimientos de dinero aparte y cierre del día
- [x] Tickets y recibos con código QR de la placa
- [x] Usuarios con rol de administrador o cajero
- [x] Tema claro y oscuro

## Instalación, respaldos y licencia (100%)
- [x] Respaldos automáticos de la base de datos y copia a una USB
- [x] Instalador para Windows con `jpackage` (`generar-instalador.ps1`)
- [x] 30 días de prueba y código de activación, con herramienta de licencias aparte

## Pendientes (0%)
Cada idea o error nuevo se agrega aquí como una casilla `- [ ]` y el porcentaje se ajusta solo.

- [ ] Cambiar la clave secreta de las licencias: hoy `SECRETO` está escrita en `GeneradorClaves.java` y va dentro de cada
  instalador. Pasar a firma con clave pública y privada, para que el programa solo lleve la pública, y volver a generar
  las claves ya entregadas. Mientras tanto, el repositorio de GitHub se queda **privado**.
