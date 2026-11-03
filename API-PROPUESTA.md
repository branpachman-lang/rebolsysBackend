# Backend de la propuesta: siete casos de uso

Contrato de adaptación al DDL `DDL_REBOLSYS_PROPUESTO_CLIENTE_UNIFICADO.sql`.
Prefijo `/api/v1`. Los identificadores son números; fechas de estancia en ISO 8601
sin zona horaria, hora local del hospedaje (America/Lima). Importes en soles.

La [iteración de reservas asistidas](API-RESERVAS.md) amplía este contrato con
ocupantes diferidos, anticipos, consumos, pagos y comprobantes internos.

| Caso | Operaciones |
|---|---|
| Registrar reserva | `POST /reservas`, `GET /reservas`, `GET /reservas/{id}`; `POST /reservas/detalles/{id}/ingreso`, `/salida`, `/cancelacion` |
| Mostrar estado de habitaciones | `GET /habitaciones?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` (solo lectura) |
| Registrar gastos | `POST /gastos`, `GET /gastos?idTurno=...`, `GET /gastos/{id}` |
| Registrar venta de producto | `POST /ventas`, `GET /ventas`, `GET /ventas/{id}`, `POST /ventas/{id}/confirmacion`, `POST /ventas/{id}/anulacion` |
| Configuración de habitaciones | `GET /habitaciones/configuracion`, `GET /habitaciones/{id}`, `POST /habitaciones`, `PUT/DELETE /habitaciones/{id}` |
| Configuración de mobiliario | `GET /items`, `GET /items/{id}`, `POST /items`, `PUT/DELETE /items/{id}`, `PUT /items/{id}/asignacion`, `GET /habitaciones/{id}/items` |
| Configuración de productos | `GET/POST /productos`, `GET/PUT/DELETE /productos/{id}`, `POST /productos/{id}/ajustes-stock`, `GET /productos/{id}/movimientos` |

Catálogos: `GET /recepcion/catalogos`; clientes unificados:
`POST /clientes`, `GET /clientes/{id}`, `PUT /clientes/{id}`.
La especificación ejecutable se publica en `/v3/api-docs` y Swagger UI.

El [módulo de gastos del turno](API-GASTOS.md) añade `GET /gastos/historial`
con filtros, paginación y total gastado, `GET /gastos/tipos` y la clave opcional
contra duplicados en `POST /gastos`. El listado anterior por turno se conserva.

El [módulo de ventas](API-VENTAS.md) añade edición de carrito con `PUT /ventas/{id}`,
registro y confirmación atómicos en `POST /ventas/confirmadas` y cuenta, pagos y
comprobantes internos en `/ventas/{id}/cuenta`, `/pagos` y `/comprobantes`.

El [módulo de configuraciones](API-CONFIGURACIONES.md) completa habitaciones,
mobiliario y productos con búsqueda paginada, filtros, cambio de estado físico,
conservación e historial de mobiliario, y alertas de stock bajo. Los formularios
pueden obtener sus opciones en `GET /configuraciones/catalogos`.

La [facturación electrónica beta](API-FACTURACION.md) añade preparación, envío,
consulta CDR y archivos sobre los comprobantes internos. Usa el contrato de
facturación de APIsPerú, con autenticación independiente de DNI/RUC. La conexión
externa queda deshabilitada hasta configurar la empresa beta y sus credenciales.

## Estructura de servicios basada en Pharma

Los mantenimientos de habitaciones, mobiliario, productos, categorías, tipos de
habitación y tipos de mobiliario implementan `CrudService<REQ, RES, ID>` en
`service/generic`: `create`, `update`, `read`, `readAll` y `delete`. Cada interfaz
de módulo vive en `service/service` y su implementación transaccional en
`service/impl`. Los controladores validan DTO y delegan en esas interfaces.
Los métodos anteriores `guardar`, `obtener` y `listar` de habitaciones e ítems
se conservan como adaptadores para no romper los flujos existentes.

Los catálogos configurables ofrecen `GET/POST /tipos-habitacion`,
`GET/PUT/DELETE /tipos-habitacion/{id}`, las mismas rutas en `/tipos-item`
y `GET/POST /categorias`, `GET/PUT/DELETE /categorias/{id}`.
El tipo de habitación incluye `nombreTipo`, `capacidadMax` y `precioBase`;
el tipo de mobiliario incluye `nombreItem`.

Eliminar mobiliario o productos los desactiva, conservando el historial.
Un ítem asignado debe retirarse antes de desactivarlo. Una habitación solo
se elimina si no tiene reservas ni asignaciones históricas de mobiliario;
el DDL no tiene un estado INACTIVO para habitaciones. Los tipos y categorías
solo se eliminan si no tienen referencias. Reducir la capacidad de un tipo
no permite dejar habitaciones por encima del nuevo máximo.

Reservas, gastos y ventas conservan operaciones de negocio explícitas:
no se borran movimientos de caja ni estancias para simular un CRUD.
Confirmar/anular ventas y registrar ingreso/salida/cancelación siguen siendo
acciones transaccionales.

## Reglas y resultados

- Reserva: `claveRegistro` obligatoria (1–64), `idCliente`, `idUsuario` y detalles
  con habitación, fechas y costo; ocupantes opcionales hasta el ingreso.
  Cada ocupante usa `idCliente`;
  menores incluyen `idClienteResponsable` y parentesco PADRE/MADRE/TUTOR. El
  responsable adulto debe ocupar la misma habitación. Edad al ingreso; sin cama
  solo menores de siete años. Se bloquean habitaciones en orden para impedir
  superposiciones concurrentes y se valida capacidad, plazas y restricciones físicas.
- Venta: `claveRegistro`, `idUsuario`, `modalidadCobro` SEPARADA/RESERVA y líneas
  de producto/cantidad. Se guarda BORRADOR sin descontar stock. Confirmar bloquea
  productos en orden, toma precios guardados y registra un movimiento por línea,
  atómicamente. Consumos vinculados exigen una estancia EN_CURSO. RESERVA exige
  además una cuenta principal con saldo y sin emitir; SEPARADA puede conservar
  la estancia y siempre tiene su propia cuenta. Anular un borrador no mueve
  stock; anular una confirmada repone existencias una sola vez.
- Una clave de reserva o venta repetida con el mismo contenido devuelve el registro;
  con otro contenido se rechaza. Confirmación y anulación repetidas no duplican movimientos.
- Gastos: requieren usuario activo, turno abierto del mismo usuario, tipo de caja
  con signo -1 y monto positivo; se guardan en FLUJO_CAJA sin idPago.
- Mobiliario: ITEM representa una unidad, no un stock. Una asignación por ítem;
  cada asignación, traslado o retiro registra historial. Los catálogos y sus
  referencias deben existir; no se crean usuarios ni datos iniciales automáticamente.
- Productos: cambios de existencias requieren usuario, motivo y clave de operación;
  se validan signo, cantidad, saldo y concurrencia bajo bloqueo del producto.
- 201 al crear; 200 al consultar o ejecutar acciones; 204 al desactivar producto;
  400 por datos inválidos; 404 por referencia inexistente; 409 por reglas de negocio,
  conflictos de integridad o concurrencia. Errores con mensaje y campos.

En esta etapa las rutas funcionan sin autenticación ni restricciones por IP.
`idUsuario` identifica al operador registrado que exige el DDL; no es un login.
Se incluyen cobros y comprobantes internos de reservas; la integración tributaria
y los cambios de frontend quedan fuera de esta etapa.
El esquema existente se valida al iniciar; no se vuelve a ejecutar el DDL.
