# Caja del turno

## Configuración local

Aplica [DATOS_CAJA.sql](src/main/resources/db/schema/DATOS_CAJA.sql) al esquema de desarrollo para habilitar la categoría `INGRESO_MANUAL`. Conserva los tipos de gasto existentes. La API usa la columna `claveRegistro` de la migración V9. Configura `app.caja.directorio-evidencias` en un directorio privado y persistente cuando el backend se ejecute fuera del entorno local.

Gerencia tiene acceso al módulo. Asigna `CAJA:VER`, `CAJA:REGISTRAR_INGRESO`, `GASTOS:CREAR` y `CAJA:CERRAR` desde Seguridad según las funciones del rol. La sesión identifica al usuario. Las operaciones se limitan a su propio turno y las escrituras nuevas requieren un único turno abierto.

## Operaciones HTTP

| Método y ruta bajo `/api/v1/caja` | Uso |
| --- | --- |
| `GET /actual` | Devuelve el turno abierto o el último cierre propio. Sin turno disponible devuelve un cuerpo vacío. |
| `GET /turnos/{id}` | Consulta los importes esperados o el arqueo guardado. |
| `GET /turnos/{id}/movimientos` | Lista el historial con `pagina` y `tamanio` opcionales. |
| `POST /turnos/{id}/movimientos` | Recibe `datos` JSON con `idTipoMovimiento`, `concepto`, `monto` positivo y `claveRegistro` única (por ejemplo, un UUID). Acepta `archivo` opcional JPG o PNG de hasta 5 MB. |
| `GET /movimientos/{id}/evidencia` | Descarga el comprobante del movimiento propio. |
| `POST /turnos/{id}/cierre` | Recibe `revision` de la consulta y `arqueos` con `idFormaPago` y `montoDeclarado` para cada medio. |

El importe esperado suma los cobros verificados por medio. En efectivo suma también el fondo de apertura y los ingresos manuales, y resta los gastos manuales. La diferencia es el declarado menos el esperado. La revisión y la escritura se hacen bajo el bloqueo del turno. Si los importes cambiaron, la API devuelve `409` para solicitar un nuevo conteo. Si se pierde la respuesta al cerrar, consulta el turno por ID. Para recuperar el resultado de un ingreso o gasto, reenvía los mismos datos y adjunto con la misma `claveRegistro`.
