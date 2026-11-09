# Reservas asistidas: contrato de esta iteración

Registro por un operador para atención presencial, llamada o WhatsApp. No existe
autoservicio web. El titular puede ser persona natural adulta o empresa; los
ocupantes siempre son personas naturales. No se implementa autenticación.

## Proceso acordado

1. Crear cliente y reservar una o varias habitaciones. Los ocupantes son opcionales
   en ese momento. El anticipo también es opcional y se registra después de crear
   la reserva mediante el endpoint de pagos.
2. Completar o corregir ocupantes antes del ingreso. Todo menor requiere un adulto
   responsable en la misma habitación. Menores de siete años pueden no ocupar
   cama, dentro de los límites de personas y menores sin cama de la habitación.
   Para el inventario real se confirmó hasta **un menor adicional sin cama por
   habitación**: dos plazas admiten dos adultos y un menor de siete años que
   comparta cama con su responsable. `capacidadPersonas` incluye ese cupo;
   `plazasCama` conserva las plazas originales y `maxMenoresSinCama` admite 0 o 1.
   Al cumplir siete años se requiere plaza propia. La configuración se aplica
   mediante V12__menor_adicional_sin_cama.sql, conservando precios y camas.
   Una edad no obliga por sí sola a contratar una habitación adicional.
3. Registrar ingreso dentro del periodo reservado. Es obligatorio completar
   ocupantes; se permite saldo pendiente, según la decisión del usuario.
4. Registrar consumos confirmados durante una estancia EN_CURSO. RESERVA suma a
   la cuenta principal solo si tiene saldo y sigue sin emitir. SEPARADA permite
   pago o comprobante aparte, siempre conservando la referencia a la reserva.
5. Registrar uno o más pagos, parciales o completos. Una reserva puede tener una
   cuenta principal y varias cuentas de consumo separadas.
6. Emitir registro interno BOLETA o FACTURA de una cuenta. Los anticipos no emiten
   automáticamente boleta/factura: se guardan contra una cuenta interna BORRADOR.
   Emitir fija sus líneas y receptor; pagos posteriores siguen permitidos.
7. Antes de la salida se exige saldo cero de todas las cuentas de la reserva.
   La habitación queda en LIMPIEZA. Pagada y anulada son estados diferentes.

## Rutas

Prefijo /api/v1. Se mantienen POST/GET /reservas y las acciones de detalle.

| Método y ruta | Uso |
|---|---|
| PUT /reservas/detalles/{id}/ocupantes | Reemplazar lista antes del ingreso; idUsuario y ocupantes. |
| GET /reservas/{id}/cuenta | Totales, pagos, documentos y permiso para nuevos cargos consolidados. Solo lectura. |
| POST /reservas/{id}/pagos | claveOperacion, idUsuario, idTurno, idFormaPago, monto, numeroOperacion opcional, idVenta opcional. Sin idVenta paga la cuenta principal. |
| POST /reservas/{id}/consumos | Mismo DTO que ventas; idDetalleReserva obligatorio y perteneciente a esta reserva. Crea y confirma atómicamente. |
| GET /reservas/{id}/consumos | Consumos, tanto consolidados como separados. |
| POST /reservas/{id}/comprobantes | tipo BOLETA/FACTURA, idClienteReceptor, idUsuario, idTurno e idVenta opcional. Sin idVenta emite la cuenta principal. |
| POST /reservas/detalles/{id}/ingreso | Check-in de una habitación; exige ocupantes. |
| POST /reservas/detalles/{id}/salida | Check-out de una habitación; exige toda la cuenta de la reserva saldada. |

Una cuenta borrador sin pagos se materializa al registrar el primer anticipo,
consumo o comprobante. GET no crea cuentas. idCuenta es el idComprobante interno;
al emitir se conserva para mantener todos sus pagos y fuentes sin duplicarlos.

Pagos repetidos con la misma clave y contenido no duplican caja. La misma clave
con otro contenido produce 409. Se rechaza el sobrepago. Cada pago aceptado produce
un ingreso FLUJO_CAJA y requiere turno abierto del operador. El operador confirma
manualmente el dinero recibido; no se conecta una pasarela de pagos.

Los comprobantes son registros internos de desarrollo con series BINT/FINT.
No son CPE ni se presentan como aceptados por SUNAT. Los precios e IGV cero
conservan el criterio de esta fase; el tratamiento tributario queda pendiente.
FACTURA exige receptor con RUC registrado. Una emisión repetida con el mismo
tipo y receptor devuelve el mismo documento; cambiarlo después se rechaza.

## Límites deliberados y casos adicionales

- Sin devolución implementada, no se permite anular habitaciones con pagos ni
  documentos emitidos. La cancelación sin pagos sí libera la habitación y ajusta
  la cuenta borrador. No se borran pagos ni comprobantes para ocultar operaciones.
- Los consumos contabilizados no se anulan desde ventas: requieren el futuro
  flujo de devoluciones/rectificación. Ventas sin cuenta conservan su anulación.
- No-show, reprogramación, prórroga, cambio de habitación, ingreso anticipado,
  salida tardía, devolución de anticipos y sustitución de ocupantes durante una
  estancia requieren políticas adicionales. No se aplican penalidades inventadas.
- Una persona no puede quedar en dos habitaciones coincidentes. Titular,
  ocupante, comprador y receptor pueden ser personas diferentes.
- En reservas de varias habitaciones, esta etapa permite ingreso/salida por
  habitación; la salida exige saldar la reserva completa, incluso sus consumos
  separados. La llegada escalonada de personas dentro de una habitación y el
  reparto de alojamiento entre varios receptores no están implementados.
- Si la cuenta principal está pagada o emitida, los nuevos consumos deben usar
  SEPARADA; no se reabre el documento ni se vuelve a cobrar el alojamiento.

El DDL original se conserva como referencia. Para permitir que SEPARADA mantenga
idDetalleReserva se aplica únicamente V8__consumos_separados_con_reserva.sql;
no cambia las tablas ni columnas. La copia consolidada incorpora esa restricción.

## Prueba del recorrido

En la base local ya se aplicó V8 y se prepararon los catálogos mediante
`src/main/resources/db/schema/DATOS_RESERVAS.sql`. Para otra base se necesita
también la preparación original `DATOS_INICIALES_PROPUESTA.sql` y COMMIT.
La prueba de pagos concurrentes usa esos catálogos y limpia únicamente sus
usuarios, turnos, reserva y cobros creados para la prueba.

Abrir Swagger en `http://localhost:8080/swagger-ui.html`, o importar y ejecutar
en orden `api/postman/reservas-flujo.postman_collection.json`. La colección genera
sus propios clientes, habitación y producto y conserva los datos de demostración.
No hay que proporcionar tokens ni escribir a mano los identificadores.

```powershell
postman collection run api/postman/reservas-flujo.postman_collection.json --no-report-events
```

El recorrido HTTP valida S/80 de alojamiento, S/20 de anticipo, S/7 de consumo
consolidado y dos consumos separados de S/3.50. El resultado final es S/94 de
cargos, S/94 pagados y saldo cero. El comprobante principal solo incluye S/87;
los consumos separados conservan el número de reserva y no se cobran dos veces.

Se retiraron del código activo ocho archivos sin referencias: los DTO antiguos
de responsable de huésped y de pago (solicitud/respuesta), y las enumeraciones
EstadoCobro, EstadoCargo, TipoConsumo, FormaRegistro y ModoVenta. Se conservan
las entidades correspondientes al DDL y el historial documental anterior.

## Resultados de validación

- Reconstrucción limpia con Java 21 y Oracle real: 53 pruebas aprobadas, cero
  fallos. Incluye 34 pruebas funcionales y 3 de concurrencia (reservas, stock y
  cobro simultáneo del mismo saldo). Las 7 pruebas opcionales de APIs Perú se
  omitieron porque no forman parte de este flujo.
- Colección nueva de reservas: 30 solicitudes y 54 comprobaciones aprobadas.
- Regresión de las colecciones anteriores: 49 solicitudes y 116 comprobaciones
  aprobadas. Total HTTP: 79 solicitudes y 170 comprobaciones, con código de
  salida 0 y resultados locales, sin publicar en Postman.
- Swagger y api/openapi.json actualizados desde la API en ejecución; las nuevas
  operaciones se agrupan en Reservas.
- La reserva de demostración R110 quedó FINALIZADA con tres cuentas, S/94 de
  cargos y saldo cero. Los datos de demostración se conservan para revisión.
