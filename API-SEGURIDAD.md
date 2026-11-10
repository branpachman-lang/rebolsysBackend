# Inicio de sesión y permisos

El servidor autentica usuarios de REBOLSYS mediante las credenciales de Oracle y una sesión con cookie `HttpOnly`. La contraseña se guarda con BCrypt. La cookie se renueva al iniciar sesión y al cambiar la contraseña. La sesión vence después de 30 minutos de inactividad. Cinco intentos fallidos bloquean nuevos intentos durante 15 minutos. Las operaciones de escritura requieren el token CSRF de la sesión.

## Preparar el gerente inicial

1. En una instalación existente, ejecutar [V15](src/main/resources/db/iteration/V15__credencial_temporal.sql) como `PropuestaHospedajeRB` después de V14. El DDL para una instalación nueva ya incluye la columna de vencimiento.
2. Elegir una cuenta activa de un empleado existente que deba administrar el sistema. Para una prueba local se puede usar el operador de [DATOS_INICIALES_PROPUESTA.sql](src/main/resources/db/schema/DATOS_INICIALES_PROPUESTA.sql).
3. Arrancar una instancia con `REBOLSYS_GERENTE_USUARIO` y `REBOLSYS_GERENTE_PASSWORD_TEMPORAL` en su entorno privado. La contraseña debe tener al menos 15 caracteres y ocupar como máximo 72 bytes. La preparación inicial asigna el rol protegido `GERENTE`. Los arranques posteriores conservan su contraseña.
4. Retirar las variables de preparación después del primer arranque. Iniciar sesión en `/login` y elegir una contraseña nueva. Después aparece el menú **Seguridad**.

El inicio de sesión real está activo por defecto. Los flujos locales antiguos pueden usar `SESION_PRUEBA_HABILITADA=true` en `dev`; en ese modo se omite la autenticación de los módulos operativos y se bloquea el módulo de seguridad. El perfil de producción rechaza esa configuración y usa una cookie segura. La aplicación Angular y la API se sirven con el mismo origen mediante el proxy.

## Probar en el equipo local

En SQL Developer, conéctate como `PropuestaHospedajeRB` y ejecuta como script (F5) [V14](src/main/resources/db/iteration/V14__flujo_limpieza.sql) y luego [V15](src/main/resources/db/iteration/V15__credencial_temporal.sql). En una instalación actualizada, ambos scripts se pueden volver a ejecutar.

En PowerShell 7, abre una terminal en `rebolsysBackend`. Para el primer arranque con la cuenta de prueba `operacion_local` ya activa, ejecuta lo siguiente. Elige una contraseña temporal de al menos 15 caracteres y hasta 72 bytes.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.12.1"
$env:REBOLSYS_GERENTE_USUARIO = 'operacion_local'
$env:REBOLSYS_GERENTE_PASSWORD_TEMPORAL = Read-Host 'Contraseña temporal del gerente' -MaskInput
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Remove-Item Env:REBOLSYS_GERENTE_USUARIO, Env:REBOLSYS_GERENTE_PASSWORD_TEMPORAL
}
```

En otra terminal, entra a `rebolsysFrontend` y ejecuta `npm.cmd start`. Abre [el inicio de sesión](http://localhost:4200/login). Una vez creado el gerente, para los siguientes arranques basta con ejecutar `.\mvnw.cmd spring-boot:run` con Java 21 configurado. La API responde en [el endpoint de salud](http://localhost:8080/api/health).

## Sesión

En Seguridad, el alta de empleados muestra los datos obligatorios pendientes y propone la fecha de contratación actual. Al guardar, el empleado aparece entre los disponibles para crear usuario. El botón junto a su nombre abre el formulario con el empleado seleccionado. El identificador de acceso puede ser un nombre de usuario o un correo de hasta 50 caracteres. El gerente administra empleados y usuarios sin necesitar un turno de caja.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/sesion/csrf` | Obtiene `token` y `cabecera`. Enviar esa cabecera y la cookie de la sesión en las escrituras. |
| `POST /api/v1/sesion/iniciar` | Recibe `usuario` y `contrasena`. Devuelve `requiereCambio`, `gerente`, `roles`, `permisos` e `idTurno`. |
| `GET /api/v1/sesion/actual` | Consulta el usuario, permisos vigentes y turno. |
| `POST /api/v1/sesion/contrasena` | Recibe `actual` y `nueva`. Las demás sesiones quedan invalidadas. Obtener un nuevo token CSRF tras el cambio. |
| `POST /api/v1/sesion/cerrar` | Cierra la sesión actual. |

Con `requiereCambio=true` solo se puede consultar o cerrar la sesión y cambiar la contraseña. Las rutas operativas requieren un permiso explícito. Se devuelve HTTP 401 cuando la sesión falta o perdió vigencia, y HTTP 403 cuando la operación está fuera de sus permisos.

Gerencia y recepción recuperan su turno abierto al iniciar sesión. Si el usuario carece de turno abierto, se abre uno con monto base S/ 0.00. En el primer acceso con clave temporal, la apertura ocurre al establecer la contraseña definitiva. Cerrar sesión conserva el turno de caja.

## Empleados, usuarios y roles

Las rutas de esta tabla empiezan con `/api/v1/seguridad`.

| Método y ruta | Uso |
| --- | --- |
| `GET /empleados`, `POST /empleados` | Consultar o registrar un empleado. El registro solicita documento, nombres, apellidos, correo y fecha de contratación. |
| `PUT /empleados/{id}` | Actualiza nombres, apellidos, contacto y fechas del empleado. Conserva su documento y vínculo con la cuenta. |
| `PUT /empleados/{id}/estado` | Recibe `activo`. La baja fija la fecha de cese y desactiva la cuenta vinculada. La reactivación del empleado permite rehabilitar su cuenta por separado. |
| `GET /usuarios`, `POST /usuarios` | Consultar usuarios o crear una cuenta para un empleado. La contraseña temporal aparece una vez y vence en 24 horas. |
| `PUT /usuarios/{id}` | Actualiza el nombre de acceso y la selección de roles y permisos. Conserva el empleado y la credencial de la cuenta. |
| `PUT /usuarios/{id}/accesos` | Sustituye la selección de roles y permisos individuales. Aplica a la siguiente solicitud de una sesión abierta. |
| `PUT /usuarios/{id}/estado` | Cambia el estado de una cuenta. La desactivación revoca la contraseña y las sesiones. Después de reactivar, generar una nueva contraseña temporal. |
| `POST /usuarios/{id}/credencial-temporal` | Emite otra contraseña temporal para una cuenta activa y revoca la anterior. |
| `GET /funcionalidades` | Catálogo de acciones asignables por módulo. |
| `GET /roles`, `POST /roles`, `PUT /roles/{id}` | Consultar, crear y editar roles reutilizables. |
| `DELETE /roles/{id}` | Elimina un rol reutilizable sin usuarios asignados. El rol `GERENTE` está protegido. |

El rol `GERENTE` reserva la creación de cuentas, la emisión de credenciales y la asignación de accesos. La lectura de usuarios y roles y el alta de empleados se pueden delegar. Los permisos efectivos son la unión de roles y permisos individuales. Oracle guarda estos últimos en un rol interno por usuario. Para una cuenta de solo lectura, seleccionar `RESERVAS:VER`, `HABITACIONES:VER`, `PRODUCTOS:VER` o `MOBILIARIO:VER` y evitar roles que incluyan escrituras. Las solicitudes administrativas exitosas quedan en `AUDITORIA_GENERAL`.

## Recuperación por correo

Mientras se habilita el envío, el gerente puede entrar a **Seguridad → Usuarios → Nueva contraseña temporal** y entregar la clave por un canal privado. El empleado debe cambiarla al iniciar sesión.

Para preparar la recuperación automática, hay que disponer de lo siguiente.

1. Una dirección de envío del hospedaje y el nombre de su proveedor. Un solo remitente puede enviar a destinatarios de Gmail, Outlook y Hotmail.
2. La dirección HTTPS de REBOLSYS que usarán los empleados al abrir el enlace. Verifica también que cada empleado tenga su correo vigente registrado.
3. Autorización de envío en el proveedor elegido. Si se usa un dominio propio, publica los registros de autenticación que indique el proveedor. Guarda las credenciales de envío en el entorno privado del servidor. [Guía de Gmail](https://support.google.com/mail/answer/81126) · [Guía de Outlook](https://support.microsoft.com/en-us/outlook/fix-ndr-error-550-5-7-515-in-outlook-com)

Con esos datos se podrá implementar y probar el flujo de correo. La solicitud mostrará el mismo mensaje exista o no la cuenta. El enlace será de un solo uso y tendrá un vencimiento corto. REBOLSYS guardará su huella en `TOKEN_RECUPERACION` e invalidará las sesiones anteriores al cambiar la contraseña. [Guía de OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
