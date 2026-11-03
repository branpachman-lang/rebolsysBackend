# Gastos del turno: registro e historial

El recepcionista registra salidas de caja durante su turno abierto, por ejemplo
taxi para conseguir un repuesto, mantenimiento o suministros. FLUJO_CAJA conserva
la relación con TURNO_CAJA y de allí con USUARIO. No se modifica la relación de
reservas con su operador ni se incorpora seguridad en esta etapa.

## Operaciones

- POST /api/v1/gastos: registrar gasto; devuelve 201.
- GET /api/v1/gastos/historial: historial paginado y resumen de los mismos filtros.
- GET /api/v1/gastos/{id}: detalle.
- GET /api/v1/gastos/tipos: categorías disponibles (movimientos de caja de salida).
- GET /api/v1/gastos?idTurno=...: listado anterior, conservado para compatibilidad.

La pantalla futura puede mostrar el historial y abrir “Nuevo gasto” desde allí.
Después de guardar, consulta de nuevo el historial con sus filtros. Este trabajo
prepara esos datos y operaciones en el backend.

## Filtros del historial

| Parámetro | Comportamiento |
|---|---|
| idTurno | Turno específico, abierto o cerrado. |
| idUsuario | Recepcionista responsable del turno, incluso si está inactivo actualmente. |
| idTipoMovimiento | Categoría de gasto. |
| desde, hasta | Fechas YYYY-MM-DD inclusivas en America/Lima; se pueden usar por separado. |
| buscar | Texto literal, sin distinguir mayúsculas, en concepto, documento, categoría o usuario. |
| montoMinimo, montoMaximo | Importes inclusivos en soles. |
| pagina, tamanio | Página desde 0, tamaño predeterminado 20 y máximo 100. |

Todos los filtros se combinan. Sin filtros se consulta el historial completo.
Los movimientos se ordenan del más reciente al más antiguo, con idFlujo como
desempate. Cada movimiento incluye responsable, categoría, turno, concepto,
importe, fecha y referencias del documento/evidencia. El resumen contiene
cantidadGastos y totalGastado del resultado completo, no solamente de la página.
Un resultado vacío devuelve lista vacía, cantidad cero y total cero.

Solo se incluyen movimientos con signo -1 y sin idPago: los cobros de reservas,
ingresos y devoluciones vinculadas a pagos no se mezclan con gastos operativos.

## Registrar gasto

El cuerpo conserva idUsuario, idTurno, idTipoMovimiento, concepto y monto,
más documentoRespaldo, evidenciaRuta y evidenciaSha256 opcionales. La ruta y
huella de evidencia se informan juntas. El servidor fija la fecha al registrar;
no se permite escribir fechas retroactivas ni atribuirlo al turno de otro usuario.

Se añade claveRegistro opcional (1–64 caracteres, letras, números, punto,
guion, dos puntos o guion bajo). La pantalla nueva debe enviar una UUID por
registro: repetir la misma clave y contenido devuelve el mismo gasto, incluso
si el turno ya se cerró; otro contenido con esa clave produce 409. Las peticiones
anteriores sin clave siguen funcionando y cada una crea un gasto nuevo.

El importe se guarda positivo; la categoría indica que es una salida.
Se admite usuario activo y turno abierto propio, monto mayor que cero y tipo
de caja con signo -1. No se convierte una compra de repuestos en stock ni se
crean productos automáticamente. No hay edición ni borrado de movimientos;
la corrección mediante reverso queda para una iteración posterior.

## Preparación y sugerencias

V9__clave_registro_gastos.sql agrega una columna nullable y única a FLUJO_CAJA.
Los movimientos anteriores, incluidos cobros de reservas, conservan sus datos.
El DDL original se conserva y la copia consolidada incorpora V9 (46 tablas,
265 columnas). DATOS_GASTOS.sql prepara categorías de desarrollo sin reemplazar
registros. Flyway sigue deshabilitado; la preparación es explícita.

Categorías iniciales: taxis y movilidad, repuestos y mantenimiento, suministros
y otros gastos. Se conserva el tipo GASTO anterior. Como siguiente paso sugiero
la corrección con reverso y motivo, y registrar el medio de pago si se necesita
distinguir efectivo, Yape o transferencia. La referencia visual a Yape no supone
una integración con ese servicio.

La evidencia actual es una referencia y huella; este módulo no sube archivos.
No se valida saldo suficiente de efectivo porque aún no hay un cierre/arqueo
operativo completo ni un medio de pago asociado a cada gasto.

## Probar en desarrollo

Abrir `http://localhost:8080/swagger-ui.html` y seleccionar **Gastos**.
Para la pantalla futura recomiendo seleccionar inicialmente el turno actual y
ofrecer acceso al historial completo, con el total visible y la acción Nuevo gasto.

Ejemplo de consulta (usar un identificador real de turno):

```text
GET /api/v1/gastos/historial?idTurno=101&desde=2026-11-02&hasta=2026-11-02&tamanio=20
```

Ejemplo de estructura de respuesta:

```json
{
  "gastos": {
    "contenido": [],
    "pagina": 0,
    "tamanio": 20,
    "totalElementos": 0,
    "totalPaginas": 0,
    "ultima": true
  },
  "resumen": { "cantidadGastos": 0, "totalGastado": 0 }
}
```

Importar y ejecutar en orden
`api/postman/gastos-historial.postman_collection.json`. Obtiene el turno y las
categorías disponibles y genera las claves sin introducirlas a mano. Cada
ejecución crea dos gastos de demostración nuevos y los conserva: taxi S/12.50 y
repuesto S/35.90, por un total de S/48.40. El reintento del taxi no crea otro gasto.

```powershell
postman collection run api/postman/gastos-historial.postman_collection.json --no-report-events
```

En otra base de desarrollo, preparar primero el DDL y datos originales y después
aplicar `src/main/resources/db/iteration/V9__clave_registro_gastos.sql` y
`src/main/resources/db/schema/DATOS_GASTOS.sql`, con COMMIT de los datos iniciales.
En la base local esta preparación ya está aplicada.

## Validación del 2 de noviembre de 2026

- 67 pruebas aprobadas con Java 21 y Oracle real, incluidos filtros combinados,
  fechas inclusivas, búsqueda literal, paginación, resumen, turnos cerrados y
  dos solicitudes simultáneas con la misma clave creando un solo gasto.
- Se verificó que cobros y devoluciones vinculados a pagos de reservas no se
  mezclen con el historial de gastos operativos.
- Se omitieron las 7 pruebas opcionales de APIs Perú, fuera del alcance.
- Colección nueva: 20 solicitudes y 34 comprobaciones aprobadas.
- Regresión HTTP de reservas y verificación general: 51 solicitudes y 111
  comprobaciones aprobadas. Total: 71 solicitudes y 145 comprobaciones, todas
  con resultado satisfactorio y código de salida 0, sin publicar en Postman.
- OpenAPI actualizado desde el servidor y operaciones agrupadas en Gastos.
