# REBOLSYS Backend

API REST para administrar las reservas y la tienda del hospedaje Real Bolognesi. Desarrollada con Java 21, Spring Boot, Spring Data JPA y Oracle.

## Ejecutar

Requiere Java 21 y un esquema Oracle preparado con los scripts de `src/main/resources/db`. El DDL inicial está en `migration/V1__esquema_inicial_rebolsys.sql`; los cambios posteriores están en `iteration/`, ordenados de V2 a V7. Ejecutarlos una sola vez, en ese orden, sobre un esquema nuevo. En un esquema existente se aplican únicamente los cambios pendientes, después de respaldarlo. Hibernate valida el esquema al arrancar; no crea tablas ni inserta datos.

Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en las variables de entorno de IntelliJ o de la terminal. `.env.example` muestra los nombres disponibles; Spring no carga ese archivo automáticamente.

En desarrollo también se puede crear `.local/application-local-private.properties`, excluido de Git, con estas propiedades:

```properties
DB_URL=jdbc:oracle:thin:@//localhost:1522/FREEPDB1
DB_USERNAME=
DB_PASSWORD=
APISPERU_TOKEN=
```

Ejecutar desde la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

`application.yaml` selecciona `dev`; `application-dev.yaml` configura el desarrollo local y `application-prod.yaml` la conexión mediante variables de entorno. Para seleccionar producción, definir `SPRING_PROFILES_ACTIVE=prod`.

- API local: `http://localhost:8080/api/v1`
- Swagger en desarrollo: `http://localhost:8080/swagger-ui.html`
- Frontend: [rebolsysFrontend](https://github.com/branpachman-lang/rebolsysFrontend)

## Funcionalidades

- Estado y calendario de habitaciones, con límites de ocupación configurables.
- Reservas con una cabecera y varias habitaciones en `detalle_reserva`.
- Cliente natural o empresa; huéspedes y menores con un adulto responsable en la misma habitación.
- Edición del cliente, huéspedes y menores a cargo; validaciones de documentos, edades y capacidad.
- Pagos en efectivo, abonos, saldo pendiente y comprobantes internos.
- Cancelación con devolución; los consumos de tienda requieren revisar previamente su liquidación.
- Venta a personas externas o del hospedaje, con cobro separado o cargo a una reserva.
- Edición de ventas pendientes sin cobros ni comprobante, con ajuste de inventario y saldo.
- Productos, categorías, precios, stock y rutas de imágenes.
- Consulta opcional de documentos mediante APIs Perú, configurando `APISPERU_TOKEN` en el backend.

La API conserva la organización `controller`, `service/service`, `service/impl`, `repository`, `entity`, `dto`, `enums` y `exception`. El mantenimiento básico utiliza `CrudService`; las operaciones de reservas, pagos y ventas se ejecutan dentro de transacciones.

## Estado del proyecto

Usuarios, roles, login, pasarela de pagos, envío de correo y emisión tributaria están pendientes. Los comprobantes actuales son internos. El registro requiere los catálogos y el usuario de operación existentes; los cobros requieren un turno de caja abierto. El arranque no crea estos registros automáticamente. Esta versión está destinada al desarrollo local, no a exponer una API operativa en Internet.

El repositorio contiene código y recursos del sistema. La documentación de trabajo, colecciones Postman, pruebas, respaldos y credenciales permanecen fuera de Git.