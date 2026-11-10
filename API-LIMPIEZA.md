# Informes de limpieza y tickets de mantenimiento

El módulo toma de la sesión al responsable del informe y al empleado del ticket. El gerente consulta un resumen general de todo el personal sin requerir un turno de caja. El personal con el rol `RECEPCIONISTA` consulta su turno abierto y el resto del personal consulta su propia actividad del día en hora de Lima. Las funciones de limpieza se asignan por separado en el módulo Seguridad. En el modo local de pruebas se puede configurar el usuario con `SESION_PRUEBA_USUARIO`.

## Preparación

En una base existente de la propuesta, ejecutar [V14](src/main/resources/db/iteration/V14__flujo_limpieza.sql) como `PropuestaHospedajeRB` antes de arrancar. La migración agrega el estado del informe, las observaciones por ítem y una restricción que admite un solo informe activo por habitación. Los informes existentes quedan como historial `APROBADO`. El DDL de instalaciones nuevas ya incluye estos cambios.

El flujo usa el movimiento de inventario `EST_ITEM`. Si falta en el catálogo, ejecutar [DATOS_CONFIGURACIONES.sql](src/main/resources/db/schema/DATOS_CONFIGURACIONES.sql) y confirmar con `COMMIT`.

Las fotos se guardan en `.local/evidencias-limpieza` por defecto. Para elegir un directorio persistente, configurar `APP_LIMPIEZA_DIRECTORIO_EVIDENCIAS`. Oracle guarda la ruta y el SHA-256 mediante los campos existentes.

## Rutas

Todas las rutas de la tabla empiezan con `/api/v1/limpieza`.

| Método y ruta | Uso |
| --- | --- |
| `GET /inicio` | Gerencia recibe totales generales con `alcance=GENERAL`, recepción los de su turno con `alcance=TURNO` y el resto su actividad de hoy con `alcance=DIA`. Solo recepción sin turno recibe contadores en cero. La consulta y gestión de informes y tickets no exige turno de caja. |
| `GET /habitaciones` | Filtra por `numeroHabitacion`, `idTipoHabitacion`, `estado` y `fechaSalida`. Muestra la salida prevista de la estancia en curso o la última salida de una estancia finalizada. Usa la hora real cuando está registrada. |
| `GET /habitaciones/{id}` | Muestra el inventario actual, el último informe enviado y sus tickets. Mientras el informe está en borrador devuelve `SIN_REGISTRAR` en los ítems, `SIN_INFORME` y `SIN_TICKET`. |
| `POST /habitaciones/{id}/iniciar` | Abre la limpieza de una habitación `POR_LIMPIAR` y devuelve el borrador. El gerente también puede iniciar una revisión de una habitación `DISPONIBLE` sin estancia en curso, que pasa a `EN_LIMPIEZA`. El responsable puede retomar su borrador. |
| `DELETE /borradores/{id}` | Cancela el borrador propio y deja la habitación `POR_LIMPIAR`. |
| `POST /borradores/{id}/enviar` | Exige un estado por cada ítem actual y deja el informe `POR_APROBAR`. |
| `GET /informes` | Filtra por `nombre`, `estado`, `fechaEmision` y `numeroHabitacion`. |
| `GET /informes/{id}` | Muestra el estado encontrado y el estado efectivo de cada ítem, sus evidencias y sus tickets. |
| `PATCH /informes/{id}` | Corrige los ítems indicados mientras el informe está `POR_APROBAR`. |
| `DELETE /informes/{id}` | Elimina un informe `POR_APROBAR` y sus tickets. Revierte los cambios de conservación de esos tickets y deja la habitación `POR_LIMPIAR`. |
| `POST /informes/{id}/aprobar` | Aprueba el informe si todas las incidencias se resolvieron y deja la habitación `DISPONIBLE`. |
| `POST /informes/{id}/items/{idDetalleInforme}/ticket` | Crea un ticket resuelto para el ítem y actualiza su conservación a `BUENO`. |
| `GET /tickets` | Filtra por `nombre`, `numeroHabitacion`, `tipoAccion` y `fechaEmision`. |
| `GET /tickets/{id}` | Muestra todos los campos del ticket y sus asociaciones. |
| `PUT /tickets/{id}` | Sustituye acción, observación técnica y costo mientras el informe asociado está `POR_APROBAR`. |
| `DELETE /tickets/{id}` | Elimina un ticket de un informe `POR_APROBAR`, reabre su incidencia y restaura la conservación anterior. |
| `POST /evidencias` | Carga un JPEG o PNG de hasta 5 MB y 20 megapíxeles. Recibe `multipart/form-data` con el campo `archivo`. |
| `GET /evidencias/{nombre}` | Descarga la foto. La ruta completa viene en la respuesta de carga. |

Los filtros de fecha usan `AAAA-MM-DD` y hora de Lima. Los informes y tickets admiten `pagina` desde cero y `tamanio` entre 1 y 100. Los nombres se generan como `INF-{id}` y `TKT-{id}`.

## Envío y resolución

El borrador entrega `idInformeRevision` e `idItem` para cada ítem. Para enviar, usar un cuerpo como este y sustituir `evidencia` por la respuesta de `POST /evidencias`.

```json
{
  "observaciones": "Revisión después de la salida",
  "items": [
    {
      "idItem": 12,
      "estadoEncontrado": "DANADO",
      "observaciones": "La pata de la mesa está suelta",
      "evidencia": {
        "evidenciaRuta": "/api/v1/limpieza/evidencias/<sha256>.jpg",
        "evidenciaSha256": "<sha256>"
      }
    },
    {"idItem": 13, "estadoEncontrado": "BUENO"}
  ]
}
```

`BUENO` corresponde a Óptimo. `DANADO`, `FALTANTE` y `EN_REPARACION` requieren foto. `PATCH /informes/{id}` acepta el mismo arreglo `items` con las filas a corregir. Para cambiar el estado encontrado de una fila con ticket, primero se elimina ese ticket.

El detalle del informe entrega el `idDetalleInforme` para la ruta de creación de tickets. El cuerpo de creación contiene `accion` y la foto cargada como `evidencia`.

```json
{
  "accion": {
    "tipoAccion": "REPARACION",
    "observacionTecnica": "Se ajustó el soporte de la mesa",
    "costoReparacion": 15.50
  },
  "evidencia": {
    "evidenciaRuta": "/api/v1/limpieza/evidencias/<sha256>.jpg",
    "evidenciaSha256": "<sha256>"
  }
}
```

Para editar un ticket se envían directamente los campos de `accion`. El costo es opcional para `REPARACION` y debe omitirse para `REPOSICION`. La evidencia queda registrada al crear el ticket.

Los campos `puedeAprobar`, `puedeEditar`, `puedeEliminar` y `puedeCrearTicket` permiten activar los botones correspondientes. La API vuelve a comprobar cada regla al recibir la acción. Los conflictos de estado devuelven HTTP 409. Las operaciones de escritura se serializan por habitación y las actualizaciones de conservación usan el historial de inventario.

`tieneIncidencias` indica si el informe registró algún hallazgo. `incidenciasPendientes` cuenta los hallazgos que todavía requieren un ticket. Ambos valores se mantienen separados para que el historial conserve lo observado.
