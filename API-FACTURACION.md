# Facturación electrónica con APIsPerú — beta

Contrato de esta iteración. Se mantiene el modelo actual de Perfil, Persona Natural
y Persona Jurídica. El precio registrado representa el importe final con impuesto
incluido, confirmado por el usuario. La integración usa exclusivamente beta; la
empresa configurada en APIsPerú debe declarar ese ambiente antes de cada operación.

La cuenta y sus pagos continúan en el modelo existente. Una emisión electrónica
conserva una copia fiscal separada del comprobante interno ya emitido: sus líneas,
receptor, emisor, impuestos, serie, número y fecha quedan fijados al prepararla.
Preparar, enviar y consultar nunca cobran dinero ni descuentan stock otra vez.

Rutas implementadas, prefijo /api/v1:

- POST /comprobantes/{id}/emision-electronica: prepara el documento sin enviarlo.
  Recibe claveOperacion, idUsuario, tasaIgv (porcentaje), direccionReceptor opcional,
  tratamientos por numeroLinea opcionales (afectacion 10 gravada, 20 exonerada,
  30 inafecta y tasaIgv), y cuotas opcionales si hay saldo pendiente.
- GET /comprobantes/{id}/emision-electronica: consulta estado local y totales.
- POST /comprobantes/{id}/emision-electronica/envio: envía una única vez, con idUsuario.
- POST /comprobantes/{id}/emision-electronica/consulta: consulta CDR con idUsuario.
- GET /comprobantes/{id}/emision-electronica/xml: XML firmado recibido.
- GET /comprobantes/{id}/emision-electronica/cdr: ZIP de la constancia recibida.
- GET /comprobantes/{id}/emision-electronica/pdf: solicita representación PDF de la copia fiscal.
- GET /facturacion/configuracion: indica beta, habilitación y configuración pendiente, sin secretos.

Primero se emite el registro interno BOLETA/FACTURA por la ruta de reservas o ventas
existente. Su idCuenta identifica el comprobante para estas nuevas rutas. La primera
versión conserva sus series BINT/FINT y correlativos exclusivamente para beta.

PREPARADO permite enviar. ENVIANDO impide una segunda solicitud concurrente.
ACEPTADO y RECHAZADO requieren una respuesta CDR coherente. INCIERTO exige consultar
antes de cualquier decisión: un timeout no demuestra que la emisión haya fallado.
ERROR_ENVIO identifica fallos previos al envío o validaciones HTTP explícitas, y
permite repetir manualmente la misma copia. No se reintentan automáticamente POST.
Un estado incierto no se reenvía automáticamente ni se libera por tiempo transcurrido.

Las tasas son explícitas y se guardan por línea en la copia fiscal; no se deduce
un beneficio tributario por el hecho de ser un hospedaje. Puede configurarse una
tasa distinta por línea para alojamiento y productos. La base neta se obtiene del
total final, y el impuesto es la diferencia, con redondeo a céntimos por línea.
Si existe saldo se exige un cronograma que sume ese saldo para declarar Crédito;
una cuenta saldada se declara Contado. El comprobante electrónico no sustituye
las reglas de caja, anticipos ni el pago total requerido al check-out.

Fuente primaria: https://facturacion.apisperu.com/doc/swagger (API 1.3),
documentación https://facturacion.apisperu.com/doc. Operaciones: auth/login,
companies/{id}, invoice/send, invoice/status e invoice/pdf. La autenticación de
facturación es independiente del token de consultas DNI/RUC. La creación de cuenta,
empresa beta, certificado de pruebas y configuración SUNAT se realiza en APIsPerú.

La emisión beta autenticada se validó el 3 de noviembre de 2026 con una boleta
y una factura aceptadas, sus CDR y representaciones PDF. No se implementan
todavía notas de crédito/débito, bajas ni producción. Las cuotas y condiciones
comerciales del proveedor dependen de la cuenta utilizada.
La copia preparada queda fijada; revisar tasas y receptor antes de prepararla.
Los métodos de envío y consulta son orquestadores: sus rutas los invocan fuera
de transacciones de otros servicios. Las llamadas HTTP ocurren entre transacciones
cortas, para confirmar ENVIANDO antes de contactar al proveedor.

## Configurar la cuenta real de pruebas

1. Activar el servicio de facturación en la [cuenta de APIsPerú](https://www.apisperu.com/servicios/facturacion).
   La API de consulta DNI/RUC y la de facturación tienen autenticaciones distintas.
2. Configurar una empresa con environment beta, certificado de pruebas y los datos
   que solicite el proveedor. Su documentación incluye la generación de certificado
   gratuito de pruebas en companies/certificate/free. No se crea ni cambia esa
   empresa automáticamente desde REBOLSYS.
3. Obtener el id de esa empresa y configurar únicamente el bloque FACTURACION_*
   de [.env.example](.env.example) en las variables del proceso o en el archivo
   privado .local/application-local-private.properties, conservando la conexión DB.
   No guardar contraseñas ni tokens en los archivos versionados ni en Postman.
   El .env no se carga automáticamente.
4. Usar FACTURACION_APISPERU_URL=https://facturacion.apisperu.com/api/v1,
   FACTURACION_HABILITADA=true, idEmpresa, RUC, razón social y dirección completa
   con ubigeo de seis dígitos. Para autenticación, configurar el token de facturación
   o usuario/password de esa cuenta. Con usuario/password el backend obtiene un
   token y lo guarda solo en memoria durante hasta 23 horas. Ante HTTP 401, invalida
   esa sesión para el siguiente intento manual; no repite automáticamente el envío.
5. Reiniciar el backend y consultar GET /api/v1/facturacion/configuracion. Debe
   indicar habilitada=true y configuracionPendiente vacío. Antes de cada operación
   el cliente verifica por GET companies/{id} el RUC y el ambiente beta. Si no puede
   comprobarlos, bloquea la llamada de emisión. La producción permanece bloqueada.

La forma concreta del objeto environment no está detallada por el OpenAPI del
proveedor. En la respuesta autenticada se verificó environment.nombre=beta y
environment.fe_url apuntando al receptor beta de SUNAT. El cliente admite nombre,
code, name, value o id textual beta; bloquea respuestas ambiguas o un fe_url que
apunte a otro receptor. El token de empresa se guarda en configuración privada y
es independiente del APISPERU_TOKEN de consultas DNI/RUC. El certificado y los
datos SOL se administran en APIsPerú; no se copian al repositorio del backend.

## Ejemplo del flujo

Para una venta, emitir primero la boleta interna con
POST /api/v1/ventas/{idVenta}/comprobantes. Para una reserva usar su ruta existente
/api/v1/reservas/{idReserva}/comprobantes. La respuesta incluye idCuenta; ese es
el idComprobante de las rutas nuevas. Las condiciones de reserva y consumo
separado se conservan. Al emitir productos por separado, se mantiene el número
de reserva en observacion cuando corresponde.

POST /api/v1/comprobantes/{idComprobante}/emision-electronica:

```json
{
  "claveOperacion":"beta-venta-001",
  "idUsuario":1,
  "tasaIgv":18,
  "direccionReceptor":"DIRECCION FISCAL DE PRUEBA",
  "tratamientos":[],
  "cuotas":[]
}
```

Identificadores y tasa son ilustrativos. El ejemplo supone una cuenta pagada
y una operación gravada con ese porcentaje. Si el precio final es 118 y la tasa
es 18%, la copia conserva total 118, subtotal 100 e impuesto 18; no suma otro 18%.
Para una factura, direccionReceptor es obligatoria y el receptor debe tener RUC,
incluida la persona natural con RUC. El modelo de identidades sigue sin cambios.

Un tratamiento por línea usa numeroLinea, afectacion (10,20,30) y tasaIgv. Para
exonerada o inafecta se exige tasa cero. Las cuotas usan fechaPago (YYYY-MM-DD)
y monto; deben sumar exactamente el saldo pendiente. No hay cálculo automático
de exoneraciones, descuentos, detracciones, anticipos tributarios o moneda extranjera.
Los anticipos registrados en caja permanecen como pagos; esta etapa no implementa
la emisión fiscal independiente de anticipos ni su posterior aplicación tributaria.

La tasa debe corresponder a la empresa y la operación. Por ejemplo, SUNAT publica
una tasa total especial de 10.50% para determinadas MYPE de hoteles/alojamientos
durante 2026, sujeta a requisitos; el sistema no la aplica por defecto.
[Referencia SUNAT](https://orientacion.sunat.gob.pe/reduccion-del-igv-para-restaurantes-hoteles-y-alojamiento-turisticos-1).

Después, POST .../emision-electronica/envio con {"idUsuario":1}. Una respuesta HTTP
200 solo indica que la solicitud se procesó: revisar estado, codigoRespuesta y
mensajeRespuesta. Una aceptación beta continúa siendo una prueba sin validez
tributaria de producción. Consultar .../consulta ante un resultado INCIERTO.

La [colección para APIsPerú beta real](api/postman/facturacion-apisperu-beta.postman_collection.json)
requiere completar baseUrl, idComprobante, idUsuario, tasaIgv y dirección del receptor
si es factura; tratamientos y cuotas son arrays JSON. La última consulta exige
ACEPTADO y luego descarga XML/CDR/PDF. Esta colección fue ejecutada contra el
backend conectado a APIsPerú beta para los dos tipos de documento. Permite usar
un entorno de Postman para aportar las variables, sin incluir el token del proveedor.

## Base de datos

Se añadió EMISION_ELECTRONICA, una fila por comprobante interno y una numeración
única por RUC/tipo/serie/correlativo. Conserva la copia JSON, XML, CDR, totales,
estado, fecha e intentos. No contiene credenciales del proveedor.

La [V10](src/main/resources/db/iteration/V10__emision_electronica_beta.sql) agrega
solo esa tabla. Ya está aplicada en Oracle local. Para otra base, ejecutarla antes
del arranque; el DDL consolidado también se actualizó. Son 47 tablas y 288 columnas.
Flyway sigue deshabilitado y Hibernate valida. No se alteraron las tablas de identidad.

## Pruebas del 2 de noviembre de 2026

- Maven/Java 21 y Oracle: 115 pruebas, 108 aprobadas, cero fallos y errores,
  7 omitidas de la consulta opcional DNI/RUC. Incluye 8 pruebas del cálculo fiscal,
  9 con transporte HTTP a la simulación de facturación y los módulos anteriores.
- Postman facturación: 67 solicitudes reales al backend (65 pasos y dos envíos
  concurrentes) y 97 comprobaciones aprobadas. La dependencia APIsPerú se simuló.
- Regresión HTTP sobre backend/Oracle: verificacion 21/57, ventas-productos 37/57
  y reservas-flujo 30/54 (solicitudes/comprobaciones). Total de esta verificación:
  155 solicitudes y 265 comprobaciones, todas con código de salida 0.
- La simulación registró cero documentos con más de un envío; el timeout después
  de guardar un documento se resolvió con invoice/status y sin otro invoice/send.
- Se verificaron rechazo CDR, HTTP 401, JSON inválido, CDR de otro documento,
  bloqueo de empresa en producción, redondeo, tasas mixtas, crédito y archivos.
  El XML/CDR/PDF de la simulación son fixtures ficticios.
- Se actualizó api/openapi.json desde el backend habitual. La especificación del
  proveedor quedó en api/dependencies/apisperu-facturacion.openapi.json; SHA-256
  9da95c9353d5a6480965bf29007c9a665e54dd0763dc105f7b09ec92c46fe218.

El descubrimiento Orbit no devolvió endpoints pertinentes; se descartaron y se
usó la especificación pública de APIsPerú. No se consultaron mocks publicados:
el CLI instalado no admite status y no se usó búsqueda autenticada. La simulación local se construyó
desde el contrato, con los límites registrados en api/simulations/facturacion-beta.context.yaml.

Para repetir la simulación:

```powershell
postman simulation run api/simulations/facturacion-beta.sim.yaml --port 4920 --no-history --output ndjson
# En otra terminal, con Oracle disponible:
$env:RUN_ORACLE_TESTS='true'
$env:RUN_FACTURACION_SIM_TESTS='true'
.\mvnw.cmd test
```

La colección [facturacion-beta](api/postman/facturacion-beta.postman_collection.json)
es exclusiva de la simulación: espera un backend en 8082 configurado con URL
http://127.0.0.1:4920/facturacion/api/v1, token ficticio token-facturacion-local,
empresa 1 y los datos ficticios descritos en el mock. No usar esa colección con
la API externa real. Sus ventas y comprobantes se conservan como demostración.

## Validación autenticada del 3 de noviembre de 2026

Se configuró la empresa beta facilitada por el usuario y se verificó por GET
companies/{id}: HTTP 200, RUC coincidente y environment.nombre=beta. El token
permanece en .local/application-local-private.properties, excluido de Git. La
ubicación de pruebas es Áncash / Huaraz / Huaraz, ubigeo 020101.
[Referencia territorial](https://www.mef.gob.pe/contenidos/presu_publ/migl/2026_Oficio_Multiple_002-2026-VIVIENDA-C4-I41.pdf).

La API real exigió fechas con segundos y zona horaria, sin fracciones:
yyyy-MM-dd'T'HH:mm:ss-05:00. El generador aplica ese formato tanto a fechaEmision
como a las fechas de cuotas. La validación XML del proveedor confirmó la corrección.

| Documento beta | Comprobante interno | Resultado | Total de prueba |
|---|---:|---|---:|
| Factura FINT-6 | 311 | ACEPTADO, código 0 | 118.00 |
| Boleta BINT-25 | 330 | ACEPTADO, código 0 | 118.00 |

Ambos conservaron subtotal fiscal 100 e impuesto 18, con precios finales incluidos
en las ventas ficticias. El 18% fue un dato explícito de los ejemplos y no una
asignación automática del régimen tributario del hospedaje.

Se descargaron XML, ZIP de CDR y PDF auténticos de beta. Se abrió cada CDR y se
verificaron ResponseCode=0 y DocumentReference/ID coincidente. Las copias locales
de los archivos y entornos de Postman están en .local, fuera de Git.

La colección facturacion-apisperu-beta pasó dos ejecuciones: 7 solicitudes y 13
comprobaciones cada una, código de salida 0. Los reintentos sobre los documentos
aceptados mantuvieron un solo intento de envío. La consulta posterior de estos
documentos utiliza el CDR ya guardado; no requirió volver a enviarlos.

El primer intento de boleta, previo al ajuste de fecha, quedó sin CDR confirmado
y no se reenvió. Una consulta remota de ese intento devolvió error HTTP/Unauthorized
del proveedor. Se conserva su historial como INCIERTO; esa respuesta no se presenta
como rechazo o aceptación de SUNAT. La reconciliación remota de ese caso permanece
pendiente; los dos documentos de la tabla se confirmaron con el CDR recibido al enviar.

Las pruebas finales de Java/Oracle terminaron con 120 casos: 113 aprobados y 7
omitidos de la consulta opcional DNI/RUC. Se añadieron cinco pruebas de los formatos
de ambiente y del bloqueo de un receptor de producción. Las pruebas de simulación
usan credenciales ficticias explícitas y nunca heredan el token real configurado.
