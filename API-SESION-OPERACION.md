# Usuario de operación en desarrollo

El perfil `dev` admite una sesión de prueba centralizada si se configura
`SESION_PRUEBA_HABILITADA=true`. El inicio de sesión con contraseña y permisos
está activo por defecto y se documenta en [API-SEGURIDAD.md](API-SEGURIDAD.md).
En el modo de prueba, `GET /api/v1/sesion/actual`
devuelve `idUsuario`, `usuario`, `modo`, `idTurno` e `idFormaPagoEfectivo`. El usuario
se resuelve en la base por el nombre configurado, no tomando el primer usuario
del catálogo. Por defecto es `operacion_local`; puede configurarse con
`SESION_PRUEBA_USUARIO`. Debe existir y estar ACTIVO.

El modo de prueba está previsto para validaciones locales de los módulos operativos.

## Escrituras y turno

Antes de POST, PUT, PATCH o DELETE en `/api/v1`, el backend exige un usuario de
operación activo. En los DTO de operaciones, el `idUsuario` omitido se completa
antes de validar la solicitud; un identificador diferente al actual se rechaza.
Los DTO de cobro/comprobante también reciben el turno actual si se omitió. Un
turno de otro usuario o la falta de un único turno abierto se rechazan con un
mensaje de negocio. La sesión de prueba usa un turno existente. La sesión con contraseña abre un turno con monto base cero para gerencia y recepción cuando corresponde.

El frontend comparte esta sesión mediante SesionService e incorpora el usuario
en las escrituras de los módulos. La cabecera muestra el usuario de prueba y los
formularios no permiten elegir otro operador. Los catálogos que usan los
formularios se limitan al usuario actual y al turno abierto de su sesión.

Las operaciones HTTP exitosas se registran en AUDITORIA_GENERAL con usuario,
tabla, identificador, acción, fecha, IP y método/ruta. Se registra la operación,
sin copiar documentos personales ni credenciales al historial genérico. Los
registros específicos de reservas, ventas, caja e inventario conservan además
su responsable habitual. Las pruebas de servicios con actores aislados desactivan
el contexto HTTP de prueba; sus transacciones se revierten al terminar.

## Venta hasta el cobro

La pantalla adapta su modelo al contrato vigente:

1. POST `/ventas/confirmadas`: claveRegistro, idUsuario, idClienteComprador o
   compradorNombreLibre, modalidadCobro SEPARADA/RESERVA, idDetalleReserva y
   detalles. Se confirma y descuenta stock una sola vez por clave.
2. Para SEPARADA, GET `/ventas/{id}/cuenta` y POST `/ventas/{id}/pagos` con clave
   estable, usuario, turno, forma, monto y referencia. El efectivo recibido
   calcula el vuelto; se registra únicamente el monto del pago.
3. Se consulta y muestra el resultado de la venta y su cuenta. RESERVA carga el
   consumo sin registrar un pago. Un consumo SEPARADA conserva la referencia a
   la reserva cuando corresponde.

`precioEsperado` es opcional por línea; si difiere del precio vigente, la venta
se rechaza antes de confirmar/descontar. Las confirmaciones y pagos pendientes
se conservan en sessionStorage de la pestaña y se recuperan con las mismas
claves y datos, también después de recargar. No se descarta una venta confirmada
para volver a crearla después de un error de red.

El pago genera una constancia interna. Emitir boleta/factura requiere los datos
del receptor y la operación específica de comprobantes; no se presenta una
cuenta BORRADOR como comprobante fiscal emitido.
