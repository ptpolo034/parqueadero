# Tareas de Parqueadero (escritorio)

<!-- AVANCE:INICIO -->
**Avance general: ████████░░ 86%**

| Tarea | Avance |
|---|---|
| Entradas, salidas y cobro | ██████████ 100% |
| Lavado y mensualidades | ██████████ 100% |
| Caja, tickets y usuarios | ██████████ 100% |
| Instalación, respaldos y licencia | ██████████ 100% |
| Seguridad, depuración y revisión | ██████████ 100% |
| Pendientes | ██░░░░░░░░ 20% |
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

## Seguridad, depuración y revisión (100%)
- [x] Licencias con firma digital Ed25519: el programa solo lleva la clave pública y la privada queda en `herramienta-licencias/`
- [x] Contraseñas con PBKDF2; las que estaban en SHA-256 se pasan solas al formato nuevo cuando el usuario inicia sesión
- [x] Quité el código sin uso (`Tema.fondoPagina` y el ícono `alerta`)
- [x] Revisé la interfaz con capturas: dinero y tablas sin cortes, gráficos, teclado (Enter y Esc), ticket blanco en tema oscuro
- [x] Comentarios del código en primera persona
- [x] 36 pruebas automáticas pasando y el programa arranca sin errores con una base temporal

## Pendientes (20%)
Cada idea o error nuevo se agrega aquí como una casilla `- [ ]` y el porcentaje se ajusta solo.

- [ ] Guardar una copia de `herramienta-licencias/clave-privada.txt` fuera del computador (si se pierde no puedo firmar más
  licencias, y si la reemplazo invalido todas las entregadas)
- [ ] Activar mi propia instalación con la clave nueva: la clave del sistema anterior ya no vale
- [ ] Generar el instalador nuevo (subir la versión en `generar-instalador.ps1`) y no repartir los instaladores anteriores,
  porque todavía llevan el secreto viejo
- [ ] Mantener el repositorio de GitHub **privado** mientras haya instaladores viejos circulando
- [x] Agregué pruebas automáticas para `Formato` y `Tablas.repartir` (38 nuevas, 74 en total pasando); con ellas
  corregí el punto de miles cuando el computador tiene otra región y el reparto de columnas que se pasaba del ancho
