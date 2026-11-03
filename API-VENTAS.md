# Venta de varios productos del hospedaje

Esta iteración trabaja en backend, sin autenticación ni integración SUNAT.
Una venta contiene entre 1 y 100 productos distintos, cada uno con cantidad
entera positiva. El servidor toma descripción y precio del catálogo; el cliente
no envía precios ni totales.

## Recorrido

1. Consultar GET /api/v1/productos/catalogo.
2. Crear borrador con POST /api/v1/ventas. No descuenta stock.
3. Cambiar cantidades o productos y comprador con PUT /api/v1/ventas/{id},
   solamente mientras esté BORRADOR.
4. Confirmar con POST /api/v1/ventas/{id}/confirmacion. Requiere operador activo
   y un turno abierto propio. Verifica todo el carrito antes de descontar stock.
   Si falla cualquier línea, no se confirma ni se descuenta ninguna.
5. Como alternativa, POST /api/v1/ventas/confirmadas crea y confirma en una
   transacción. Si falla, tampoco deja un borrador nuevo.
6. Una venta directa SEPARADA tiene su propia cuenta:
   GET /api/v1/ventas/{id}/cuenta, POST /api/v1/ventas/{id}/pagos y
   POST /api/v1/ventas/{id}/comprobantes.
7. Consultar GET /api/v1/ventas y GET /api/v1/ventas/{id}.

Una venta CONFIRMADA significa que entregó los productos y movió stock. Su saldo
y pagos están en la cuenta; confirmar no significa haber recibido dinero.
Se puede pagar completa o parcialmente, con una clave por pago. BOLETA o FACTURA
son registros internos; FACTURA requiere un receptor con RUC registrado.

Los cobros requieren un turno abierto del operador; cada pago genera exactamente
un ingreso de caja. Se rechazan sobrepagos y claves repetidas con otro contenido.
Los cobros directos se clasifican como COBRO_VENTA; los asociados a una reserva
conservan COBRO_RESERVA.

## Carrito y reintentos

POST conserva el contrato VentaRequestDTO: claveRegistro, idUsuario,
idClienteComprador o compradorNombreLibre, modalidadCobro, idDetalleReserva
opcional y detalles [{idProducto,cantidad}, ...].

PUT usa idUsuario, idClienteComprador o compradorNombreLibre y detalles. Conserva
el operador original, clave de creación, modalidad y vínculo con la reserva.
Reemplaza el carrito y toma los precios actuales; una vez confirmada, sus precios
se conservan aunque el catálogo cambie. No admite cambios después de confirmar.

Repetir la creación con la misma clave y solicitud devuelve la misma venta;
si el borrador se editó, devuelve su contenido actual. Otra solicitud con esa
clave produce 409. Confirmación y anulación repetidas no duplican movimientos.
Si se intenta registrar y confirmar usando la solicitud original de un borrador
que ya fue editado, se pide consultar y confirmar su contenido actual.
Productos repetidos deben agruparse en una sola línea. Se valida el total completo,
incluido el límite monetario del modelo.

## Relación con reservas

- RESERVA: estancia EN_CURSO y cuenta principal con saldo, sin emitir; los
  productos se agregan a esa cuenta y se pagan por las rutas de reserva.
- SEPARADA con idDetalleReserva: cobro independiente, conservando número de
  reserva. Puede cobrarse por la ruta de venta o de reserva; ambas usan la misma
  cuenta y las mismas claves de pago.
- SEPARADA sin idDetalleReserva: venta directa a comprador registrado o nombre
  libre, sin obligarlo a tener reserva.
- Una cuenta principal pagada o emitida exige consumos posteriores SEPARADOS.
- El comprobante de un consumo separado conserva idReserva y numeroReserva.
  Los de ventas directas tienen esos campos nulos.

No se anula automáticamente una venta con cuenta/comprobante o pagos asociados;
requiere un futuro reverso/devolución. Las ventas sin cuenta pueden anularse,
reponiendo stock una sola vez. GET de una cuenta directa confirmada es solo
lectura; la cuenta se materializa al primer pago o comprobante.
Los comprobantes consolidados de reserva nunca se cobran por la ruta de una
venta individual.

## Estructura y preparación

CuentaCobroService centraliza cobros, emisión interna y respuesta de documentos,
con interfaz e implementación @Service. CuentaReservaService y CuentaVentaService
resuelven su ámbito y delegan; no se duplican pagos ni numeraciones.
Se conservan las 46 tablas y 265 columnas. DATOS_VENTAS.sql prepara COBRO_VENTA
sin reemplazar registros; también se necesita la preparación de reservas para
CUENTA, series CTA/BINT/FINT y formas de pago.

## Probar el módulo

Abrir `http://localhost:8080/swagger-ui.html`, apartado **Ventas de productos**.
La base local ya tiene preparada la categoría de ingreso COBRO_VENTA.

Importar y ejecutar en orden
`api/postman/ventas-productos.postman_collection.json`. Obtiene operador, turno,
documentos y forma de pago desde catálogos, crea dos productos y compradores de
demostración y conserva sus registros.

```powershell
postman collection run api/postman/ventas-productos.postman_collection.json --no-report-events
```

Ejemplo de venta con varios productos; sustituir los identificadores por los
devueltos por el catálogo y la preparación local:

```json
{
  "claveRegistro": "venta-carrito-001",
  "idUsuario": 1,
  "compradorNombreLibre": "Comprador directo",
  "modalidadCobro": "SEPARADA",
  "detalles": [
    { "idProducto": 1, "cantidad": 2 },
    { "idProducto": 2, "cantidad": 3 }
  ]
}
```

El ejemplo de la colección comienza con agua a S/3.50 y galletas a S/2.00,
modifica el carrito a 3 aguas y 2 galletas (S/14.50), cobra S/5 y luego S/9.50,
y emite una boleta interna con ambas líneas. También prueba una factura interna
de S/5.50 a una empresa de demostración y una anulación sin cobros que repone
stock una sola vez. Las ventas de demostración pagadas son V151 y V154.

## Validación del 2 de noviembre de 2026

- 80 pruebas aprobadas con Java 21 y Oracle real. Incluyen varios productos,
  edición de borrador, precios guardados, rechazo de stock parcial, transacción
  atómica, cobro directo y consolidado, reintentos y dos operadores intentando
  cobrar el mismo saldo. Se omitieron 7 pruebas opcionales de APIs Perú.
- Colección de ventas: 37 solicitudes y 57 comprobaciones aprobadas.
- Regresión de reservas, gastos y verificación general: 71 solicitudes y 145
  comprobaciones aprobadas. Total HTTP: 108 solicitudes y 202 comprobaciones,
  todas con código de salida 0, sin publicación de resultados.
- OpenAPI actualizado desde la API en ejecución y rutas agrupadas en
  Ventas de productos. No hubo cambios en el frontend ni en el DDL.
