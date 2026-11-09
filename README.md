# REBOLSYS Backend — propuesta de siete casos de uso

Backend con Java 21, Spring Boot, JPA y Oracle, adaptado al DDL propuesto con cliente
unificado. Mantiene controladores, servicios, implementaciones, repositorios,
entidades y DTO separados. El frontend no forma parte de esta adaptación.

El backend incluye el [flujo de reservas asistidas](API-RESERVAS.md):
ocupantes opcionales al reservar, anticipo, ingreso con saldo pendiente, consumos
consolidados o separados, pagos parciales, comprobantes internos y salida con
saldo cero. Presencial, llamada y WhatsApp se registran mediante un operador.
Para probar el recorrido completo se incluye
[reservas-flujo.postman_collection.json](api/postman/reservas-flujo.postman_collection.json).

La iteración de [gastos del turno](API-GASTOS.md) añade historial paginado,
filtros por fechas, turno, recepcionista, categoría, texto e importe, resumen
del total filtrado y registro sin duplicados mediante claveRegistro. Se prueba con
[gastos-historial.postman_collection.json](api/postman/gastos-historial.postman_collection.json).
La V9 y las categorías de desarrollo ya se prepararon en la base local.

El módulo de [ventas de productos](API-VENTAS.md) permite un carrito con varios
productos, editar borradores, confirmar todos sus stocks en una transacción y
cobrar una venta directa con comprobante interno. Los consumos de reservas
conservan sus reglas y cuenta. Los cobros y documentos usan un servicio común.
Se incluye [ventas-productos.postman_collection.json](api/postman/ventas-productos.postman_collection.json).
DATOS_VENTAS.sql prepara COBRO_VENTA; ya está aplicado en la base local.

[Caja del turno](API-CAJA.md) describe los ingresos manuales, los gastos y el cierre con arqueo por forma de pago.

Los mantenimientos siguen el patrón de Pharma:
`service/generic/CrudService<REQ, RES, ID>` → interfaz por módulo en
`service/service` → implementación en `service/impl` → controlador REST.
Habitaciones, mobiliario, productos, categorías, tipos de habitación y tipos de
mobiliario comparten las cinco operaciones de esa interfaz. Reservas, ventas y
gastos conservan acciones propias de negocio y sus transacciones.

## Casos implementados

1. Registrar reservas con una o varias habitaciones; completar ocupantes asociados a CLIENTE al reservar o antes del ingreso.
2. Mostrar estado y disponibilidad de habitaciones, sin alterar registros al consultar.
3. Registrar gastos del turno abierto del operador y consultar su historial filtrable y totales.
4. Registrar ventas de varios productos, editar borradores, confirmar stock, cobrar y emitir comprobantes internos; anular sin cobros con reposición.
5. Crear y editar habitaciones, su tipo, estado y límites de ocupación.
6. Crear y editar mobiliario, asignarlo, trasladarlo, retirarlo y consultar movimientos.
7. Crear y editar productos, desactivarlos y ajustar existencias con historial.

Los bloqueos y transacciones impiden reservas superpuestas y ventas por encima del
stock. El registro de reservas y ventas y los ajustes de inventario admiten claves
de operación para evitar duplicados. Los menores requieren un adulto de la misma
habitación; solo menores de siete años pueden alojarse sin cama dentro del límite.

## Base de datos y arranque

Las tarifas de las 20 habitaciones reales están registradas según la imagen del
usuario, con impuesto incluido. La V11 añade `precioPorNoche` opcional a HABITACION
para permitir precios distintos dentro del mismo tipo; las habitaciones anteriores
sin tarifa propia siguen usando la del tipo. Ejecutar
[V11](src/main/resources/db/iteration/V11__tarifa_por_habitacion.sql) antes de arrancar
esta versión sobre una base existente. No modifica el costo de reservas guardadas.
El [inventario y sus tarifas](api/fixtures/README.md) documenta los 26 muebles de cama
y las correcciones de la 303, 401 y 504.

El esquema Oracle de esta versión es **PropuestaHospedajeRB**. El DDL propuesto ya
fue ejecutado. Hibernate usa `validate`; Flyway está deshabilitado y no se insertan
datos al iniciar. La referencia es
[DDL_REBOLSYS_PROPUESTO.sql](src/main/resources/db/schema/DDL_REBOLSYS_PROPUESTO.sql).
Las migraciones y el DDL consolidado anteriores se conservan como historial y no
corresponden a esta versión; no ejecutarlos en el esquema de la propuesta.

La conexión local está en `.local/application-local-private.properties`, excluido
de Git. También se admiten `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del entorno.
La contraseña proporcionada está configurada solo en el archivo privado.

```powershell
.\mvnw.cmd spring-boot:run
```

API: http://localhost:8080/api/v1 · Swagger: http://localhost:8080/swagger-ui.html

El 1 de noviembre de 2026 se preparó el esquema local con catálogos, el operador
`operacion_local`, un turno abierto y registros de demostración de los siete
casos. Para preparar otro esquema de desarrollo, el archivo opcional
[DATOS_INICIALES_PROPUESTA.sql](src/main/resources/db/schema/DATOS_INICIALES_PROPUESTA.sql)
carga catálogos, un operador de desarrollo y un turno abierto. No se ejecuta
automáticamente: revisar los registros y confirmar con COMMIT si se desea usarlos.
Es reutilizable y no reemplaza datos existentes. Habitaciones, clientes, productos
y mobiliario se registran mediante la API. La cuenta Oracle de conexión y el
operador del negocio son registros diferentes.

## Contrato y ejemplos

- [Rutas y reglas de los siete casos](API-PROPUESTA.md).
- [Configuraciones de habitaciones, mobiliario y productos](API-CONFIGURACIONES.md).
- [Flujo de informes de limpieza y tickets de mantenimiento](API-LIMPIEZA.md).
- [Inicio de sesión y gestión de permisos](API-SEGURIDAD.md).
- [Facturación electrónica beta con APIsPerú](API-FACTURACION.md).
- [Prueba Postman de las configuraciones](api/postman/configuraciones.postman_collection.json).
- [Guía de prueba y resultados de esta reconstrucción](PRUEBAS-BACKEND.md).
- OpenAPI en `/v3/api-docs` y copia en [api/openapi.json](api/openapi.json).
- [Verificación Postman sin modificar datos](api/postman/verificacion.postman_collection.json).
- [Ejemplo Postman completo de los siete casos](api/postman/siete-casos.postman_collection.json).

La colección completa crea registros de demostración y requiere la preparación
inicial y COMMIT. Descubre el operador, turno y documentos desde catálogos;
crea tipos y categoría propios y conserva los registros que crea.
La colección de verificación usa consultas y peticiones
inválidas; funciona incluso con el esquema vacío. Variable `baseUrl` para cambiar
el puerto. Las colecciones permanecen locales; no contienen credenciales.

Los productos permiten registrar existencias iniciales en el alta, con responsable
y una entrada de inventario automática. También admiten descripción opcional de
hasta 500 caracteres (migración V13). Para reponer o corregir existencias se usa
`POST /productos/{id}/ajustes-stock`, indicando operador, saldo observado, saldo
nuevo, motivo y clave. Editar los datos del producto conserva las existencias.

El perfil dev centraliza el usuario de prueba en `/api/v1/sesion/actual`, por
defecto `operacion_local`, y comprueba que esté activo para las escrituras.
Completa el actor/turno de los DTO de operación y rechaza valores de otra sesión.
Las operaciones HTTP exitosas se registran en AUDITORIA_GENERAL. Véase
[API-SESION-OPERACION.md](API-SESION-OPERACION.md), incluido el flujo de confirmación
y pago de ventas. La autenticación real permanece pendiente de la etapa de seguridad.
Los precios guardados en las líneas de una venta se mantienen al confirmarla y
representan el importe final con impuesto incluido. Las cuentas internas conservan
sus importes anteriores; la copia fiscal nueva desglosa base e impuesto por línea
sin aumentar el total cobrado. Los registros internos de boleta/factura se envían
electrónicamente mediante el módulo beta separado descrito en API-FACTURACION.md.

Para una base ya creada con el DDL original, aplicar
`src/main/resources/db/iteration/V8__consumos_separados_con_reserva.sql` y preparar
`src/main/resources/db/schema/DATOS_RESERVAS.sql` con COMMIT. La V8 permite que un
consumo separado conserve su estancia de origen; no agrega ni elimina columnas.
Ya se aplicó en la base local de desarrollo. Flyway sigue deshabilitado.

Las rutas operativas requieren inicio de sesión y permisos por funcionalidad.
`idUsuario` identifica al responsable que exige el DDL. APIs Perú conserva su consulta opcional
con `APISPERU_TOKEN`; los siete casos funcionan sin ese servicio.

## Pruebas

```powershell
# Pruebas de modelo y errores sin conexión a Oracle
.\mvnw.cmd test

# Integración y concurrencia contra el esquema propuesto
$env:RUN_ORACLE_TESTS='true'
.\mvnw.cmd test

# Verificación HTTP local con Postman
postman collection run api/postman/verificacion.postman_collection.json --no-report-events
```

Las pruebas funcionales se revierten al terminar cada transacción. Las seis pruebas
de concurrencia necesitan transacciones independientes: crean registros propios y
los eliminan por sus claves en `finally`. No usan usuarios ni datos operativos
preexistentes. La prueba de modelo comprueba exactamente 47 tablas y 289 columnas con V11,
y que Java no intente escribir columnas calculadas.

Las pruebas del modelo anterior se conservaron localmente en
`docs/historico/pruebas-modelo-anterior`. Se sustituyeron por pruebas del contrato
propuesto. Las pruebas nuevas y los contratos de esta versión están incluidos en
el repositorio; las credenciales, informes de ejecución y archivos privados siguen
excluidos.

La validación original de siete funcionalidades se registra en PRUEBAS-BACKEND.md.
La nueva validación de reservas y sus casos límite se registra en API-RESERVAS.md.
La validación de gastos e historial se registra en API-GASTOS.md.
La validación de ventas (80 pruebas y 202 comprobaciones HTTP)
se registra en API-VENTAS.md. La iteración de configuraciones pasa 91 pruebas
automatizadas y 271 comprobaciones HTTP, documentadas en API-CONFIGURACIONES.md.
La primera iteración de facturación pasó 108 pruebas y 265 comprobaciones HTTP.
La configuración autenticada posterior pasó 113 pruebas automatizadas y dos
ejecuciones de Postman contra el beta real (26 comprobaciones). API-FACTURACION.md
registra las boletas/facturas aceptadas y distingue pruebas reales y simuladas.
Las 7 pruebas opcionales de consulta DNI/RUC de APIs Perú permanecen omitidas.

La facturación electrónica viene deshabilitada por defecto. En la configuración
privada local ya está habilitada con la empresa beta, verificada el 3 de noviembre.
La V10 ya se aplicó en Oracle local y agrega
EMISION_ELECTRONICA sin alterar Perfil, Persona Natural ni Persona Jurídica.
El token de consulta DNI/RUC no se reutiliza para emitir comprobantes.
