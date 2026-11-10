# Inventario de habitaciones de Real Bolognesi

`habitaciones-real-bolognesi.json` transcribe el inventario y las tarifas proporcionadas por el usuario el 3 de noviembre de 2026. Todos los precios son por noche y ya incluyen impuesto.

| Piso | Habitaciones | Cantidad |
| --- | --- | --- |
| 2 | 201–203 | 3 |
| 3 | 301–304 | 4 |
| 4 | 401–406 | 6 |
| 5 | 501–507 | 7 |

El inventario actualizado contiene 26 camas físicas. Las 202 y 203 tienen dos camas de dos plazas; la 303 tiene tres camas de dos plazas; las 302 y 304 tienen dos camas de plaza y media. Las restantes tienen una cama de la medida indicada. Una cama de plaza y media se configura inicialmente con una plaza de ocupante; la matrimonial, con dos. La política confirmada añade hasta un menor de siete años sin cama propia por habitación: la capacidad total inicial es plazas de cama más uno y el máximo sin cama es uno. Este cupo no aumenta las plazas para adultos ni modifica precios.

El baño privado se distingue mediante seis variantes de tipo: Simple con/sin baño, Doble sin baño, Matrimonial con/sin baño y Matrimonial doble con baño para 202–203. La última imagen corrige la 401 a con baño y la 504 a sin baño. Los tipos previos de prueba no se sobrescriben.

La V11 añade `HABITACION.precioPorNoche` opcional para tarifas independientes. Si es null, se usa el precio del tipo. La configuración expone el precio efectivo en `precioBase` y la tarifa propia en `precioPorNoche`; el calendario conserva `precio` con el precio efectivo. El DDL consolidado tiene 47 tablas y 289 columnas. Una tarifa cero sigue indicándose como pendiente en el frontend. Las 20 tarifas reales ya son positivas y habilitan la preparación de reservas. No se crean reservas, pagos ni comprobantes durante la carga.

| Piso 2 | Precio S/ | Piso 3 | Precio S/ | Piso 4 | Precio S/ | Piso 5 | Precio S/ |
| --- | ---: | --- | ---: | --- | ---: | --- | ---: |
| 201 | 100 | 301 | 100 | 401 | 100 | 501 | 30 |
| 202 | 120 | 302 | 80 | 402 | 50 | 502 | 80 |
| 203 | 120 | 303 | 150 | 403 | 100 | 503 | 80 |
| | | 304 | 80 | 404 | 30 | 504 | 80 |
| | | | | 405 | 50 | 505 | 30 |
| | | | | 406 | 30 | 506 | 40 |
| | | | | | | 507 | 30 |

Cada cama se registra como ITEM del tipo Cama y se asigna con una clave de operación estable. La condición inicial es BUENO y el responsable es el operador activo local, debido a que todavía no se implementa autenticación. Las unidades quedan disponibles para corregir su conservación desde el módulo de mobiliario. Las nueve habitaciones de pruebas se retiraron posteriormente mediante la limpieza respaldada solicitada por el usuario.

## Ejecución

Desde la raíz del backend, con el backend local iniciado:

```powershell
node api/scripts/importar-habitaciones.mjs
node api/scripts/importar-habitaciones.mjs --aplicar
# Para incorporar una revisión autorizada del inventario/precios:
node api/scripts/importar-habitaciones.mjs --actualizar
node api/scripts/importar-habitaciones.mjs --actualizar --aplicar
postman collection run api/postman/inventario-habitaciones.postman_collection.json --no-report-events
```

El primer comando consulta y presenta el plan. Sin `--actualizar` la carga detiene diferencias de configuración o tarifas existentes. `--actualizar --aplicar` incorpora explícitamente los datos de la revisión autorizada; conserva estado físico y permisos de menores, no reduce capacidad adicional y no borra historial. Usa exclusivamente la API local en localhost:8080. Repetir una revisión aplicada no crea duplicados ni nuevos movimientos. El reporte local se guarda en `.local/importacion-habitaciones-real-bolognesi.json`. Aplicar primero V11 en una base existente y arrancar el backend actualizado.

Para el inventario existente, V12 actualiza explícitamente los límites de menores
y las capacidades de sus tipos. El importador conserva cambios manuales en las
habitaciones existentes; sus nuevas altas usan el cupo de un menor de la fixture.
No se debe volver a ejecutar V12 después de desactivar deliberadamente ese cupo
en una habitación, porque aplica la política inicial de las 20 habitaciones.

Verificado el 2026-11-03 en Oracle local: primera carga 6 tipos, 20 habitaciones, 24 camas y 24 asignaciones; segunda ejecución, cero altas o asignaciones nuevas. La colección de solo lectura pasó sus 3 solicitudes y 14 aserciones de cantidades, tipos, capacidades, medidas de camas, vínculos y tarifas pendientes.

Revisión de tarifas del mismo día: 20 tarifas cargadas, dos camas adicionales en la 303 y correcciones de baño en 401/504. Repetir la revisión generó cero cambios o movimientos. Las nueve habitaciones anteriores de prueba quedaron idénticas al respaldo anterior. La colección actual pasó sus 3 solicitudes y 33 aserciones, incluidos los 20 precios exactos y la correspondencia del calendario con cada tarifa.
