# Configuración de habitaciones, mobiliario y productos

Se conserva la estructura de Pharma: CrudService<REQ, RES, ID>, interfaz por
módulo, implementación @Service, repositorio y DTO. Esta etapa sigue en backend,
sin autenticación. Los mantenimientos existentes conservan sus rutas.

## Preparación de formularios

GET /api/v1/configuraciones/catalogos devuelve tipos de habitación con precio y
capacidad, tipos de mobiliario, categorías de productos y los estados admitidos.

## Habitaciones

CRUD en /habitaciones y /habitaciones/{id}; GET /habitaciones/configuracion
conserva el listado completo anterior. GET /habitaciones/configuracion/pagina
añade pagina (0), tamanio (20, máximo 100), buscar (número o tipo),
idTipoHabitacion, estado y capacidadMinima. Ordena por número e identificador.

PATCH /habitaciones/{id}/estado recibe {estadoHabitacion}. No sobrescribe número,
tipo ni capacidad. OCUPADO corresponde exclusivamente a una estancia en curso:
no se puede simular un ingreso o salida cambiando la configuración.
DISPONIBLE, LIMPIEZA y MANTENIMIENTO se gestionan sin ocupar la habitación.

Tipos de habitación: CRUD en /tipos-habitacion. No se reduce capacidad de un tipo
por debajo de sus habitaciones ni la de una habitación por debajo de ocupantes
de reservas activas. Editar precio base no cambia el costo de reservas guardadas.
Las plazas de cama y capacidad se configuran aparte del número de muebles; una
cama matrimonial puede representar varias plazas.

La política actual permite hasta **un menor de 7 años** adicional sin plaza
propia. `maxMenoresSinCama` solo acepta 0 o 1. El máximo total de
personas debe incluir ese cupo; no se aumenta el número de plazas de cama. V12
configura las 20 habitaciones reales con `capacidadPersonas >= plazasCama + 1`
y un menor sin cama, sin modificar tarifas ni reservas existentes.

Solo se elimina una habitación sin reservas ni asignaciones históricas. El DDL
no tiene estado INACTIVO para habitaciones. MANTENIMIENTO impide nuevos ingresos
y reservas mientras permanezca ese estado; no anula reservas existentes.

### Tarifa propia por habitación (V11)

POST y PUT de habitaciones aceptan `precioPorNoche` opcional, con impuesto incluido,
desde cero hasta 99999999.99 y hasta dos decimales. Al crear sin tarifa propia se
usa el precio base del tipo. Al editar omitiendo el campo o enviando null se conserva
la tarifa propia existente, por lo que los formularios anteriores de ocupación
no la borran. Cero explícito significa tarifa pendiente en la interfaz actual.

La configuración devuelve `precioPorNoche` y conserva `precioBase` como precio
efectivo, por compatibilidad. El calendario conserva `precio` con ese mismo valor.
Modificar la tarifa del tipo no sustituye una tarifa propia; cambiar tarifas
no altera el costo de reservas previamente guardadas.

Ejemplo para la habitación 506: `precioPorNoche: 40.00`; las 402 y 405 conservan
50.00 aunque comparten el mismo tipo Simple con baño. La V11 agrega una columna
nullable y un CHECK no negativo. Ejecutarla antes de iniciar la nueva versión en
una base existente. El DDL consolidado ya la incluye.

## Mobiliario

Un ITEM es una unidad física. CRUD en /items y /items/{id}; tipos en /tipos-item.
GET /items/configuracion agrega pagina, tamanio, buscar, idTipoItem, estado,
idHabitacion, sinAsignar y estadoConservacion. Los filtros se combinan.

PUT /items/{id}/asignacion asigna, traslada o retira. idHabitacion nulo significa
retiro. Incluye claveOperacion, idUsuario, estadoActualItem y observacion.
PUT /items/{id}/conservacion registra un cambio de estado de un ítem asignado
con los mismos datos, sin idHabitacion. Cada cambio conserva una ficha anterior
y genera un movimiento. El retiro guarda su condición final sin borrar historial.

GET /habitaciones/{id}/items muestra únicamente mobiliario actualmente asignado.
GET /items/{id}/historial agrega origen, destino, condición y responsable a los
movimientos; /movimientos conserva su formato anterior. Las respuestas no muestran
las firmas internas usadas para reconocer reintentos.

El estado ACTIVO no significa que el mueble esté en buen estado; indica que está
registrado y sin asignación. estadoActualItem muestra la última condición conocida,
incluso después de retirarlo. Un ítem nuevo sin asignación previa no tiene condición
registrada. La conservación se cambia mientras está asignado o al retirarlo.

Estados JSON: Activo, Inactivo y En habitacion; también se aceptan ACTIVO,
INACTIVO y EN_HABITACION. La baja desactiva, conserva historial y requiere retirar
antes el ítem de su habitación. Tipos referenciados no se eliminan.

## Productos del hospedaje

CRUD en /productos y /productos/{id}; categorías en /categorias. El listado
paginado permite buscar por nombre/categoría, idCategoria, estado y stockBajo.
stockBajo=true selecciona stock <= stockMinimo; false selecciona stock superior.
Si se omite, no filtra existencias. El indicador se devuelve también en cada
producto. El filtro no excluye inactivos por sí solo; usar estado=ACTIVO si se desea.

Crear producto permite existencias iniciales mayores o iguales a cero. Si `stock`
es mayor a cero, `idUsuario` es obligatorio: se guardan el producto y una entrada
de inventario AJ_ENT en la misma transacción, con el motivo automático
«Existencias iniciales al registrar el producto». No requiere otro ajuste manual.
Stock cero sigue siendo compatible con las solicitudes anteriores sin operador.

`descripcion` es opcional, admite hasta 500 caracteres y se devuelve en los
listados, catálogo y detalle. V13__descripcion_producto.sql agrega la columna
nullable PRODUCTO.descripcion conservando los registros existentes. El modelo
consolidado contiene 47 tablas y 290 columnas.

PUT edita nombre, descripción, precio, categoría, mínimo,
imagen y estado, conservando existencias. POST /productos/{id}/ajustes-stock
carga o corrige cantidades con operador, motivo, stock observado y clave.
Una clave repetida no duplica movimientos. GET /productos/{id}/movimientos
conserva el historial. DELETE desactiva; no elimina ventas ni movimientos.
Las categorías con productos asociados no se eliminan.

El motivo de un ajuste posterior explica una reposición, merma o corrección de
conteo; se guarda en MOVIMIENTO_INVENTARIO.observacion junto con el usuario,
fecha, cantidad y stock resultante. GET /productos/{id}/movimientos lo expone.
El frontend incorpora el stock inicial en el alta y un modal superior de
categorías reutilizable desde el listado y el formulario.

Ejemplo de alta con existencias:

```json
{"nombreProducto":"Agua 500 ml","descripcion":"Agua sin gas","precioActual":3.50,"stock":50,"stockMinimo":5,"idCategoria":1,"estadoProducto":"ACTIVO","idUsuario":130}
```

POST de alta crea un producto por solicitud y no ofrece idempotencia. Si se pierde
su confirmación, el frontend pide verificar el listado antes de crear otro.
Los ajustes posteriores conservan su clave de operación para reintentos seguros.

## Validación y compatibilidad

Buscar trata %, _ y barra invertida como texto literal. Los listados tienen
orden estable; pagina y tamanio se validan, y las referencias inexistentes
devuelven 404. Las bajas o cambios incompatibles con historial devuelven 409.

La ampliación V11 agrega precioPorNoche a HABITACION. Con las ampliaciones
anteriores, el modelo actual contiene 47 tablas y 289 columnas.
DATOS_CONFIGURACIONES.sql prepara EST_ITEM, movimiento de signo cero para
cambios de conservación, sin reemplazar registros existentes.

## Ejemplos para probar

Todas las rutas usan el prefijo `/api/v1`. En Swagger aparecen los grupos
Configuración, Configuración de habitaciones, Configuración de mobiliario
y Configuración de productos. Primero crear tipos/categorías y luego sus
habitaciones, unidades de mobiliario o productos.

Cambiar solo el estado físico: `PATCH /habitaciones/{id}/estado`:

```json
{"estadoHabitacion":"MANTENIMIENTO"}
```

Asignar o trasladar una unidad: `PUT /items/{id}/asignacion`:

```json
{
  "claveOperacion":"instalar-tv-001",
  "idUsuario":1,
  "idHabitacion":1,
  "estadoActualItem":"BUENO",
  "observacion":"Instalación de televisor"
}
```

Usar identificadores reales obtenidos de los catálogos y listados. Para retirarlo,
enviar una clave nueva e `idHabitacion:null`, junto con su condición final.
Para registrar conservación sin trasladarlo, usar `PUT /items/{id}/conservacion`
con clave, usuario, condición y observación, omitiendo idHabitacion. Reintentar
la misma clave con los mismos datos no duplica movimientos; cambiar sus datos
devuelve 409. Una nueva operación necesita una clave nueva.

Filtros habituales:

```text
GET /habitaciones/configuracion/pagina?estado=DISPONIBLE&capacidadMinima=2
GET /items/configuracion?idHabitacion=1&estadoConservacion=DANADO
GET /items/configuracion?sinAsignar=true&estado=ACTIVO
GET /productos?estado=ACTIVO&stockBajo=true
```

La paginación devuelve contenido, pagina, tamanio, totalElementos, totalPaginas
y ultima. Una página fuera del total devuelve contenido vacío; no es un error.
Las búsquedas no distinguen mayúsculas y consideran literalmente %, _ y !.

La colección [configuraciones.postman_collection.json](api/postman/configuraciones.postman_collection.json)
prepara datos propios y prueba el ciclo completo. Requiere un operador activo
con turno abierto y los catálogos iniciales. Conserva dos habitaciones, un
televisor sin asignar con conservación EN_REPARACION y un producto con cinco
unidades. El historial de demostración se conserva para inspeccionarlo.

```powershell
postman collection run api/postman/configuraciones.postman_collection.json --no-report-events
```

En otra base de desarrollo, ejecutar primero el archivo opcional
[DATOS_CONFIGURACIONES.sql](src/main/resources/db/schema/DATOS_CONFIGURACIONES.sql)
y confirmar con COMMIT. Ya está preparado en Oracle local. El arranque sigue
validando el esquema, sin insertar catálogos ni ejecutar cambios de estructura.

## Verificación del 2 de noviembre de 2026

- Java 21, Maven y Oracle local real: 98 pruebas, 91 aprobadas, 7 omitidas del
  proveedor opcional APIs Perú, sin fallos ni errores. Incluye 11 pruebas nuevas
  de configuraciones, 6 de concurrencia y la comparación de 46 tablas/265 columnas.
- Se verificaron filtros combinados, referencias, paginación, búsqueda literal,
  estados físicos, conservación de fichas históricas, reintentos sin duplicados,
  retiro con condición final y mínimo de stock incluido en la alerta.
- Postman sobre `http://localhost:8080`: las cinco colecciones finalizaron
  con código 0 y cero comprobaciones fallidas. No se utilizaron mocks.

| Colección | Solicitudes | Comprobaciones |
|---|---:|---:|
| configuraciones | 48 | 69 |
| verificacion | 21 | 57 |
| gastos-historial | 20 | 34 |
| ventas-productos | 37 | 57 |
| reservas-flujo | 30 | 54 |
| Total | 156 | 271 |

Las pruebas funcionales revierten sus transacciones y las de concurrencia limpian
sus datos propios. Las colecciones conservan demostraciones para inspección.
Se actualizó `api/openapi.json` desde la instancia ejecutada. No se probó frontend,
autenticación ni emisión electrónica, que siguen fuera de esta etapa.

Las ejecuciones y sus informes son locales. La versión instalada del CLI de
Postman permite ejecutar las colecciones, pero no admite el comando `status`.
