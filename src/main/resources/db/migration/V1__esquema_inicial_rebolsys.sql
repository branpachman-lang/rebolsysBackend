-- REBOLSYS: DDL con unificacion de cuenta_huesped y cargo_producto en consumo.

-- Base: DB rebolsys cambios.txt. Creacion inicial: 46 tablas; no es una migracion.

-- Unificacion en consumo y correcciones de creacion descritas a continuacion.

-- consumo.idCliente es opcional; registra al comprador/responsable cuando esta identificado.

-- PRODUCTO requiere idProducto y permite idReserva NULL para ventas externas.

-- HOSPEDAJE requiere idReserva y no lleva idProducto. OTRO no lleva idProducto.

-- estadoCargo conserva el estado de facturacion; estadoCobro conserva el de pago.

-- Los datos del comprobante siguen siendo la copia historica de lo emitido.

-- CORRECCION DE CREACION: se retiran cuatro restricciones que usan columnas ausentes:
-- item_stocktotal_chk, item_stockalmacen_chk, item_stock_chk y dh_codigoUnico.
-- Se corrige la coma faltante de di_informe_item_unq.
-- No se reintroducen stockTotal, stockAlmacen ni codigoUnico.
-- Las restantes columnas y relaciones se conservan.
-- VALIDADO en Oracle Database 21c Express Edition el 2026-10-19:
-- creacion de 46 tablas, 65 FK, carga de 164 filas y 13 pruebas de restricciones/calculo.
-- Para crear desde cero en un esquema vacio; para ejecucion parcial, usar RECUPERAR_TABLAS_FALTANTES_REBOLSYS.sql.

/* ================= SECCION 1: CATALOGOS ================= */

CREATE TABLE tipo_documento (
    idTipoDocumento NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreDocumento VARCHAR2(30) NOT NULL,
    codigoSunat VARCHAR2(2),
    CONSTRAINT tipo_documento_pk PRIMARY KEY (idTipoDocumento),
    CONSTRAINT tipo_documento_nombre_unq UNIQUE (nombreDocumento)
);

CREATE TABLE rol (
    idRol NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreRol VARCHAR2(30) NOT NULL,
    CONSTRAINT rol_pk PRIMARY KEY (idRol),
    CONSTRAINT rol_nombre_unq UNIQUE (nombreRol)
);

CREATE TABLE tipo_habitacion (
    idTipoHabitacion NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreTipo VARCHAR2(30) NOT NULL,
    capacidadMax NUMBER(2) NOT NULL,
    precioBase NUMBER(10,2) DEFAULT 0 NOT NULL,
    CONSTRAINT tipo_habitacion_pk PRIMARY KEY (idTipoHabitacion),
    CONSTRAINT tipo_habitacion_nombre_unq UNIQUE (nombreTipo),
    CONSTRAINT tipo_habitacion_precio_chk CHECK (precioBase >= 0),
    CONSTRAINT tipo_habitacion_capacidad_chk CHECK (capacidadMax > 0)
);

CREATE TABLE categoria (
    idCategoria NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreCategoria VARCHAR2(30) NOT NULL,
    CONSTRAINT categoria_pk PRIMARY KEY (idCategoria),
    CONSTRAINT categoria_nombre_unq UNIQUE (nombreCategoria)
);

CREATE TABLE tipo_ocupante (
    idTipoOcupante NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreTipo VARCHAR2(30) NOT NULL,
    CONSTRAINT tipo_ocupante_pk PRIMARY KEY (idTipoOcupante),
    CONSTRAINT tipo_ocupante_nombre_unq UNIQUE (nombreTipo)
);

CREATE TABLE tipo_comprobante (
    idTipoComprobante NUMBER GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR2(20) NOT NULL,
    nombreComprobante VARCHAR2(30) NOT NULL,
    codigoSunat VARCHAR2(2),
    CONSTRAINT tipo_comprobante_pk PRIMARY KEY (idTipoComprobante),
    CONSTRAINT tipo_comprobante_codigo_unq UNIQUE (codigo),
    CONSTRAINT tipo_comprobante_nombre_unq UNIQUE (nombreComprobante)
);

CREATE TABLE forma_pago (
    idFormaPago NUMBER GENERATED ALWAYS AS IDENTITY,
    nombrePago VARCHAR2(30) NOT NULL,
    CONSTRAINT forma_pago_pk PRIMARY KEY (idFormaPago),
    CONSTRAINT forma_pago_nombre_unq UNIQUE (nombrePago)
);

CREATE TABLE tipo_movimiento_caja (
    idTipoMovimiento NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreMovimiento VARCHAR2(30) NOT NULL,
    SIGNO            NUMBER(2) NOT NULL,
    CONSTRAINT tipo_movimiento_caja_pk PRIMARY KEY (idTipoMovimiento),
    CONSTRAINT tipo_movimiento_caja_unq UNIQUE (nombreMovimiento),
    CONSTRAINT tipo_movimiento_caja_signo_chk CHECK (signo IN (-1, 1))
);

CREATE TABLE tipo_movimiento_almacen (
    idTipoMovimientoAlmacen NUMBER GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR2(10) NOT NULL,
    nombreMovimiento VARCHAR2(50) NOT NULL,
    signo NUMBER(2) NOT NULL,
    CONSTRAINT tma_pk PRIMARY KEY (idTipoMovimientoAlmacen),
    CONSTRAINT tma_codigo_unq UNIQUE (codigo),
    CONSTRAINT tma_nombre_unq UNIQUE (nombreMovimiento),
    CONSTRAINT tma_signo_chk CHECK (signo IN (-1, 0, 1))
);

CREATE TABLE tipo_credencial (
    idTipoCredencial NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreCredencial VARCHAR2(50) NOT NULL,
    CONSTRAINT tp_pk PRIMARY KEY (idTipoCredencial),
    CONSTRAINT tp_nombre_unq UNIQUE (nombreCredencial)
);

CREATE TABLE tipo_item (
idTipoItem NUMBER GENERATED ALWAYS AS IDENTITY,
nombreItem VARCHAR2(30) NOT NULL,
CONSTRAINT tipo_item_pk PRIMARY KEY (idTipoItem),
CONSTRAINT tipo_item_nombre_unq UNIQUE(nombreItem)
);

/* ================= SECCION 2: CONFIGURACION DEL SISTEMA ================= */

CREATE TABLE hospedaje (
    idHospedaje NUMBER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR2(150) NOT NULL,
    ruc VARCHAR2(11) NOT NULL,
    direccion VARCHAR2(200),
    logoRuta VARCHAR2(300),
    CONSTRAINT hospedaje_pk PRIMARY KEY (idHospedaje),
    CONSTRAINT hospedaje_ruc_unq UNIQUE (ruc),
    CONSTRAINT hospedaje_ruc_formato_chk CHECK (REGEXP_LIKE(ruc, '^[0-9]{11}$'))
);

CREATE TABLE parametro_sistema (
    idParametro NUMBER GENERATED ALWAYS AS IDENTITY,
    clave VARCHAR2(50) NOT NULL,
    valor VARCHAR2(200) NOT NULL,
    descripcion VARCHAR2(200),
    CONSTRAINT parametro_sistema_pk PRIMARY KEY (idParametro),
    CONSTRAINT parametro_clave_unq UNIQUE (clave)
);

CREATE TABLE modulo (
    idModulo NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreModulo VARCHAR2(50) NOT NULL,
    ruta VARCHAR2(100),
    CONSTRAINT modulo_pk PRIMARY KEY (idModulo),
    CONSTRAINT modulo_nombre_unq UNIQUE (nombreModulo)
);

CREATE TABLE permiso (
    idPermiso NUMBER GENERATED ALWAYS AS IDENTITY,
    idModulo NUMBER NOT NULL,
    accion VARCHAR2(100) NOT NULL,
    CONSTRAINT permiso_pk PRIMARY KEY (idPermiso),
    CONSTRAINT permiso_modulo_fk FOREIGN KEY (idModulo) REFERENCES modulo(idModulo),
    CONSTRAINT permiso_mod_acc_unq UNIQUE (idModulo, accion)
);

/* ================= SECCION 3: PERSONAS Y ROLES DE NEGOCIO ================= */

CREATE TABLE perfil (
    idPerfil NUMBER GENERATED ALWAYS AS IDENTITY,
    idTipoDocumento NUMBER NOT NULL,
    numeroDocumento VARCHAR2(50) NOT NULL,
    correo VARCHAR2(100),
    telefono VARCHAR2(15),
    CONSTRAINT perfil_pk PRIMARY KEY (idPerfil),
    CONSTRAINT perfil_tipoDocumento_fk FOREIGN KEY (idTipoDocumento) REFERENCES tipo_documento(idTipoDocumento),
    CONSTRAINT perfil_unq UNIQUE (idTipoDocumento, numeroDocumento)
);

CREATE TABLE persona_natural (
    idPerfil NUMBER,
    nombres VARCHAR2(100) NOT NULL,
    apellidos VARCHAR2(100) NOT NULL,
    fechaNacimiento DATE,
    CONSTRAINT persona_pk PRIMARY KEY (idPerfil),
    CONSTRAINT persona_perfil_fk FOREIGN KEY (idPerfil) REFERENCES perfil(idPerfil)
);

CREATE TABLE persona_juridica (
    idPerfil NUMBER,
    razonSocial VARCHAR2(250) NOT NULL,
    direccionFiscal VARCHAR2(200),
    CONSTRAINT empresa_pk PRIMARY KEY (idPerfil),
    CONSTRAINT empresa_perfil_fk FOREIGN KEY (idPerfil) REFERENCES perfil(idPerfil)
);

CREATE TABLE cliente (
    idCliente NUMBER GENERATED ALWAYS AS IDENTITY,
    idPerfil NUMBER NOT NULL,
    CONSTRAINT cliente_pk PRIMARY KEY (idCliente),
    CONSTRAINT cliente_perfil_fk FOREIGN KEY (idPerfil) REFERENCES perfil(idPerfil),
    CONSTRAINT cliente_perfil_unq UNIQUE (idPerfil)
);

CREATE TABLE huesped (
    idHuesped NUMBER GENERATED ALWAYS AS IDENTITY,
    idPerfil NUMBER NOT NULL,
    restriccionesMedicas VARCHAR2(500),
    CONSTRAINT huesped_pk PRIMARY KEY (idHuesped),
    CONSTRAINT huesped_persona_fk FOREIGN KEY (idPerfil) REFERENCES persona_natural(idPerfil),
    CONSTRAINT huesped_persona_unq UNIQUE (idPerfil)
);

CREATE TABLE empleado (
    idEmpleado NUMBER GENERATED ALWAYS AS IDENTITY,
    idPerfil NUMBER NOT NULL,
    fechaContratacion DATE NOT NULL,
    fechaCese DATE,
    CONSTRAINT empleado_pk PRIMARY KEY (idEmpleado),
    CONSTRAINT empleado_persona_fk FOREIGN KEY (idPerfil) REFERENCES persona_natural(idPerfil),
    CONSTRAINT empleado_persona_unq UNIQUE (idPerfil),
    CONSTRAINT empleado_cese_chk CHECK (fechaCese IS NULL OR fechaCese >= fechaContratacion)
);

/* ================= SECCION 4: SEGURIDAD Y ACCESO ================= */

CREATE TABLE usuario (
    idUsuario NUMBER GENERATED ALWAYS AS IDENTITY,
    idEmpleado NUMBER NOT NULL,
    username VARCHAR2(50) NOT NULL,
    estadoUsuario VARCHAR2(30) NOT NULL,
    CONSTRAINT usuario_pk PRIMARY KEY (idUsuario),
    CONSTRAINT usuario_empleado_fk FOREIGN KEY (idEmpleado) REFERENCES empleado(idEmpleado),
    CONSTRAINT usuario_empleado_unq UNIQUE (idEmpleado),
    CONSTRAINT usuario_username_unq UNIQUE (username),
    CONSTRAINT usuario_estado_chk CHECK (estadoUsuario IN ('ACTIVO', 'BLOQUEADO', 'INACTIVO'))
);

CREATE TABLE usuario_credencial (
    idCredencial NUMBER GENERATED ALWAYS AS IDENTITY,
    idUsuario NUMBER NOT NULL,
    idTipoCredencial NUMBER NOT NULL,
    valorHash VARCHAR2(255),
    credencialTemporal NUMBER(1) DEFAULT 1,
    intentosFallidos NUMBER DEFAULT 0 NOT NULL,
    fechaBloqueo TIMESTAMP,
    CONSTRAINT usuario_credencial_pk PRIMARY KEY (idCredencial),
    CONSTRAINT uc_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT uc_tipo_credencial FOREIGN KEY (idTipoCredencial) REFERENCES tipo_credencial(idTipoCredencial),
    CONSTRAINT uc_usu_tipo_unq UNIQUE (idUsuario, idTipoCredencial),
    CONSTRAINT uc_credenical_temporal_chk CHECK (credencialTemporal IN (0, 1)),
    CONSTRAINT uc_intentos_fallidos_check CHECK (intentosFallidos BETWEEN 0 AND 5)
);

CREATE TABLE token_recuperacion (
    idToken NUMBER GENERATED ALWAYS AS IDENTITY,
    idUsuario NUMBER NOT NULL,
    tokenHash VARCHAR2(255) NOT NULL,
    fechaCreacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fechaExpiracion TIMESTAMP NOT NULL,
    usado NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT token_recuperacion_pk PRIMARY KEY (idToken),
    CONSTRAINT token_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario) ON DELETE CASCADE,
    CONSTRAINT token_unq UNIQUE (tokenHash),
    CONSTRAINT token_usado_chk CHECK (usado IN (0, 1)),
    CONSTRAINT token_expira_chk CHECK (fechaExpiracion > fechaCreacion),
    CONSTRAINT TOKEN_HASH_HEX_CHK CHECK (REGEXP_LIKE(tokenHash, '^[0-9A-Fa-f]{64}$'))
);

CREATE TABLE usuario_rol (
    idUsuario NUMBER NOT NULL,
    idRol NUMBER NOT NULL,
    CONSTRAINT usuario_rol_pk PRIMARY KEY (idUsuario, idRol),
    CONSTRAINT ur_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT ur_rol_fk FOREIGN KEY (idRol) REFERENCES rol(idRol)
);

CREATE TABLE rol_permiso (
    idRol NUMBER NOT NULL,
    idPermiso NUMBER NOT NULL,
    CONSTRAINT rol_permiso_pk PRIMARY KEY (idRol, idPermiso),
    CONSTRAINT rp_rol_fk FOREIGN KEY (idRol) REFERENCES rol(idRol),
    CONSTRAINT rp_permiso_fk FOREIGN KEY (idPermiso) REFERENCES permiso(idPermiso)
);

CREATE TABLE lista_negra (
    idListaNegra NUMBER GENERATED ALWAYS AS IDENTITY,
    idPerfil NUMBER NOT NULL,
    motivo VARCHAR2(500) NOT NULL,
    fechaRegistro TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    idUsuario NUMBER NOT NULL,
    CONSTRAINT lista_negra_pk PRIMARY KEY (idListaNegra),
    CONSTRAINT ln_perfil_fk FOREIGN KEY (idPerfil) REFERENCES perfil(idPerfil),
    CONSTRAINT ln_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario)
);

/* ================= SECCION 5: HABITACIONES, INVENTARIO Y PRODUCTOS ================= */

CREATE TABLE habitacion (
    idHabitacion NUMBER GENERATED ALWAYS AS IDENTITY,
    numeroCuarto VARCHAR2(10) NOT NULL,
    idTipo NUMBER NOT NULL,
    estadoHabitacion VARCHAR2(30) NOT NULL,
    CONSTRAINT habitacion_pk PRIMARY KEY (idHabitacion),
    CONSTRAINT habitacion_numero_unq UNIQUE (numeroCuarto),
    CONSTRAINT habitacion_tipo_fk FOREIGN KEY (idTipo) REFERENCES tipo_habitacion(idTipoHabitacion),
    CONSTRAINT habitacion_estado_chk CHECK (estadoHabitacion IN ('DISPONIBLE', 'OCUPADO', 'MANTENIMIENTO', 'LIMPIEZA'))
);

CREATE TABLE item (
    idItem NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreItem VARCHAR2(100) NOT NULL,
    estadoItem VARCHAR2(30) DEFAULT 'Activo' NOT NULL,
    idTipoItem NUMBER NOT NULL,
    CONSTRAINT item_pk PRIMARY KEY (idItem),
    CONSTRAINT item_tipo_fk FOREIGN KEY (idTipoItem) REFERENCES tipo_item(idTipoItem),
    CONSTRAINT item_estado_chk CHECK (estadoItem IN ('Activo','Inactivo','En habitacion'))
);

CREATE TABLE producto (
    idProducto NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreProducto VARCHAR2(100) NOT NULL,
    precioActual NUMBER(10,2) NOT NULL,
    stock NUMBER(9,0) DEFAULT 0 NOT NULL,
    stockMinimo NUMBER(9,0) DEFAULT 0 NOT NULL,
    idCategoria NUMBER NOT NULL,
    estadoProducto VARCHAR2(30) NOT NULL,
    CONSTRAINT producto_pk PRIMARY KEY (idProducto),
    CONSTRAINT producto_precio_chk CHECK (precioActual >= 0),
    CONSTRAINT producto_stock_chk CHECK (stock >= 0),
    CONSTRAINT producto_stockmin_chk CHECK (stockMinimo >= 0),
    CONSTRAINT producto_categoria_fk FOREIGN KEY (idCategoria) REFERENCES categoria(idCategoria),
    CONSTRAINT producto_estado_chk CHECK (estadoProducto IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE detalle_habitacion (
    idDetalleHabitacion NUMBER GENERATED ALWAYS AS IDENTITY,
    idHabitacion NUMBER NOT NULL,
    idItem NUMBER NOT NULL,
    estadoActualItem VARCHAR2(30) NOT NULL,
    CONSTRAINT detalle_habitacion_pk PRIMARY KEY (idDetalleHabitacion),
    CONSTRAINT dh_habitacion_fk FOREIGN KEY (idHabitacion) REFERENCES habitacion(idHabitacion),
    CONSTRAINT dh_item_fk FOREIGN KEY (idItem) REFERENCES item(idItem),
    CONSTRAINT dh_estado_actual_chk CHECK (estadoActualItem IN ('BUENO', 'DANADO', 'FALTANTE', 'EN_REPARACION'))
);

/* ================= SECCION 6: RESERVAS ================= */

CREATE TABLE reserva (
    idReserva NUMBER GENERATED ALWAYS AS IDENTITY,
    idCliente NUMBER NOT NULL,
    idHabitacion NUMBER NOT NULL,
    costoEstadia NUMBER(10,2) DEFAULT 0 NOT NULL,
    fechaIngreso DATE NOT NULL,
    fechaSalida DATE NOT NULL,
    estadoReserva VARCHAR2(30) NOT NULL,
    idUsuario NUMBER NOT NULL,
    CONSTRAINT reserva_pk PRIMARY KEY (idReserva),
    CONSTRAINT reserva_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT reserva_cliente_fk FOREIGN KEY (idCliente) REFERENCES cliente(idCliente),
    CONSTRAINT reserva_habitacion_fk FOREIGN KEY (idHabitacion) REFERENCES habitacion(idHabitacion),
    CONSTRAINT reserva_costo_chk CHECK (costoEstadia >= 0),
    CONSTRAINT reserva_fechas_chk CHECK (fechaSalida > fechaIngreso),
    CONSTRAINT reserva_estado_chk CHECK (estadoReserva IN ('CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA'))
);

CREATE TABLE reserva_huesped (
    idReserva NUMBER,
    idHuesped NUMBER,
    idTipoOcupante NUMBER NOT NULL,
    CONSTRAINT reserva_huesped_pk PRIMARY KEY (idReserva, idHuesped),
    CONSTRAINT rh_reserva_fk FOREIGN KEY (idReserva) REFERENCES reserva(idReserva),
    CONSTRAINT rh_huesped_fk FOREIGN KEY (idHuesped) REFERENCES huesped(idHuesped),
    CONSTRAINT rh_tipo_fk FOREIGN KEY (idTipoOcupante) REFERENCES tipo_ocupante(idTipoOcupante)
);

/* ================= SECCION 7: CAJA Y FACTURACION ================= */

CREATE TABLE turno_caja (
    idTurno NUMBER GENERATED ALWAYS AS IDENTITY,
    idUsuario NUMBER NOT NULL,
    fechaApertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fechaCierre TIMESTAMP,
    montoBase NUMBER(10,2) DEFAULT 0.00 NOT NULL,
    estadoSesion NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT turno_pk PRIMARY KEY (idTurno),
    CONSTRAINT turno_montobase_chk CHECK (montoBase >= 0),
    CONSTRAINT turno_fechas_chk CHECK (fechaCierre IS NULL OR fechaCierre >= fechaApertura),
    CONSTRAINT turno_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT turno_estado_chek CHECK(estadoSesion IN(0,1))
);

CREATE TABLE serie_comprobante (
    idTipoComprobante NUMBER NOT NULL,
    serie VARCHAR2(10) NOT NULL,
    siguienteNumero NUMBER(10) DEFAULT 1 NOT NULL,
    CONSTRAINT serie_comprobante_pk PRIMARY KEY (idTipoComprobante, serie),
    CONSTRAINT sc_tipo_fk FOREIGN KEY (idTipoComprobante) REFERENCES tipo_comprobante(idTipoComprobante),
    CONSTRAINT sc_siguiente_chk CHECK (siguienteNumero BETWEEN 1 AND 100000000)
);

CREATE TABLE comprobante (
    idComprobante NUMBER GENERATED ALWAYS AS IDENTITY,
    idCliente NUMBER,
    idTipoComprobante NUMBER NOT NULL,
    idTurno NUMBER NOT NULL,
    serie VARCHAR2(10) NOT NULL,
    numeroComprobante VARCHAR2(20),
    fechaEmision TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    subtotal NUMBER(12,2) NOT NULL,
    igv NUMBER(12,2) NOT NULL,
    totalPagar NUMBER(12,2) NOT NULL,
    receptorTipoDoc VARCHAR2(10),
    receptorNumeroDoc VARCHAR2(20),
    receptorNombre VARCHAR2(200) NOT NULL,
    idComprobanteOrigen NUMBER,
    estadoComprobante VARCHAR2(30) NOT NULL,
    hashCpe VARCHAR2(100),
    sunatCodigoRespuesta VARCHAR2(10),
    sunatMensaje VARCHAR2(500),
    claveIdempotencia VARCHAR2(64),
    CONSTRAINT comprobante_pk PRIMARY KEY (idComprobante),
    CONSTRAINT comprobante_cliente_fk FOREIGN KEY (idCliente) REFERENCES cliente(idCliente),
    CONSTRAINT comprobante_tipo_fk FOREIGN KEY (idTipoComprobante) REFERENCES tipo_comprobante(idTipoComprobante),
    CONSTRAINT comprobante_turno_fk FOREIGN KEY (idTurno) REFERENCES turno_caja(idTurno),
    CONSTRAINT comprobante_origen_fk FOREIGN KEY (idComprobanteOrigen) REFERENCES comprobante(idComprobante),
    CONSTRAINT comprobante_total_chk CHECK (totalPagar >= 0),
    CONSTRAINT comprobante_montos_chk CHECK (subtotal >= 0 AND igv >= 0 AND totalPagar = subtotal + igv),
    CONSTRAINT comprobante_numero_unq UNIQUE (idTipoComprobante, serie, numeroComprobante),
    CONSTRAINT comprobante_idem_unq UNIQUE (claveIdempotencia),
    CONSTRAINT comprobante_estado_chk CHECK (estadoComprobante IN ('BORRADOR', 'EMITIDO', 'ANULADO')),
    CONSTRAINT comprobante_serie_fk FOREIGN KEY (idTipoComprobante, serie) REFERENCES serie_comprobante(idTipoComprobante, serie)
);

CREATE TABLE consumo (
    idConsumo NUMBER GENERATED ALWAYS AS IDENTITY,
    idCliente NUMBER,
    idReserva NUMBER,
    idProducto NUMBER,
    idUsuario NUMBER NOT NULL,
    tipoConsumo VARCHAR2(20) NOT NULL,
    descripcion VARCHAR2(300) NOT NULL,
    cantidad NUMBER(12,3) DEFAULT 1 NOT NULL,
    valorUnitario NUMBER(12,4) NOT NULL,
    subtotal NUMBER(12,2) NOT NULL,
    igv NUMBER(12,2) NOT NULL,
    total NUMBER(12,2) NOT NULL,
    estadoCargo VARCHAR2(30) NOT NULL,
    estadoCobro VARCHAR2(30) NOT NULL,
    fechaConsumo TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT consumo_pk PRIMARY KEY (idConsumo),
    CONSTRAINT consumo_cliente_fk FOREIGN KEY (idCliente) REFERENCES cliente(idCliente),
    CONSTRAINT consumo_reserva_fk FOREIGN KEY (idReserva) REFERENCES reserva(idReserva),
    CONSTRAINT consumo_producto_fk FOREIGN KEY (idProducto) REFERENCES producto(idProducto),
    CONSTRAINT consumo_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT consumo_producto_uq UNIQUE (idConsumo, idProducto),
    CONSTRAINT consumo_tipo_chk CHECK (tipoConsumo IN ('HOSPEDAJE', 'PRODUCTO', 'OTRO')),
    CONSTRAINT consumo_cantidad_chk CHECK (cantidad > 0),
    CONSTRAINT consumo_unidades_prod_chk CHECK (tipoConsumo <> 'PRODUCTO' OR cantidad = TRUNC(cantidad)),
    CONSTRAINT consumo_valor_chk CHECK (valorUnitario >= 0),
    CONSTRAINT consumo_total_chk CHECK (subtotal >= 0 AND igv >= 0 AND total = subtotal + igv),
    CONSTRAINT consumo_estado_cargo_chk CHECK (estadoCargo IN ('PENDIENTE', 'FACTURADO', 'ANULADO')),
    CONSTRAINT consumo_estado_cobro_chk CHECK (estadoCobro IN ('PENDIENTE', 'PAGADO', 'ANULADO')),
    CONSTRAINT consumo_tipo_relacion_chk CHECK (
        (tipoConsumo = 'PRODUCTO' AND idProducto IS NOT NULL)
        OR (tipoConsumo = 'HOSPEDAJE' AND idReserva IS NOT NULL AND idProducto IS NULL)
        OR (tipoConsumo = 'OTRO' AND idProducto IS NULL)
    )
);

CREATE TABLE comprobante_detalle (
    idComprobanteDetalle NUMBER GENERATED ALWAYS AS IDENTITY,
    idComprobante NUMBER NOT NULL,
    idConsumo NUMBER NOT NULL,
    numeroLinea NUMBER(4) NOT NULL,
    descripcion VARCHAR2(300) NOT NULL,
    cantidad NUMBER(12,3) NOT NULL,
    valorUnitario NUMBER(12,4) NOT NULL,
    subtotal NUMBER(12,2) NOT NULL,
    igv NUMBER(12,2) NOT NULL,
    total NUMBER(12,2) NOT NULL,
    IDHABITACION NUMBER,
    CONSTRAINT comprobante_detalle_pk PRIMARY KEY (idComprobanteDetalle),
    CONSTRAINT cd_comprobante_fk FOREIGN KEY (idComprobante) REFERENCES comprobante(idComprobante),
    CONSTRAINT cd_consumo_fk FOREIGN KEY (idConsumo) REFERENCES consumo(idConsumo),
    CONSTRAINT cd_linea_unq UNIQUE (idComprobante, numeroLinea),
    CONSTRAINT cd_numero_linea_chk CHECK (numeroLinea > 0),
    CONSTRAINT cd_cantidad_chk CHECK (cantidad > 0),
    CONSTRAINT cd_valor_chk CHECK (valorUnitario >= 0),
    CONSTRAINT cd_total_chk CHECK (subtotal >= 0 AND igv >= 0 AND total = subtotal + igv),
    CONSTRAINT cd_habitacion_fk FOREIGN KEY (IDHABITACION) REFERENCES habitacion(idHabitacion)
);

CREATE TABLE pago (
    idPago NUMBER GENERATED ALWAYS AS IDENTITY,
    idComprobante NUMBER NOT NULL,
    idFormaPago NUMBER NOT NULL,
    idUsuario NUMBER NOT NULL,
    idTurno NUMBER NOT NULL,
    monto NUMBER(10,2) NOT NULL,
    numeroOperacion VARCHAR2(50),
    fechaPago TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    verificado NUMBER(1) DEFAULT 0 NOT NULL,
    claveIdempotencia VARCHAR2(64),
    CONSTRAINT pago_pk PRIMARY KEY (idPago),
    CONSTRAINT pago_comprobante_fk FOREIGN KEY (idComprobante) REFERENCES comprobante(idComprobante),
    CONSTRAINT pago_formapago_fk FOREIGN KEY (idFormaPago) REFERENCES forma_pago(idFormaPago),
    CONSTRAINT pago_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT pago_turno_fk FOREIGN KEY (idTurno) REFERENCES turno_caja(idTurno),
    CONSTRAINT pago_monto_chk CHECK (monto > 0),
    CONSTRAINT pago_verificado_chk CHECK (verificado IN (0,1)),
    CONSTRAINT pago_idem_unq UNIQUE (claveIdempotencia)
);

CREATE TABLE movimiento_inventario (
    idMovimiento NUMBER GENERATED ALWAYS AS IDENTITY,
    idProducto NUMBER,
    idItem NUMBER,
    idConsumo NUMBER,
    idDetalleHabitacion NUMBER,
    idTipoMovimientoAlmacen NUMBER NOT NULL,
    cantidad NUMBER(9,0) NOT NULL,
    stockResultante NUMBER(9,0) NOT NULL,
    idUsuario NUMBER NOT NULL,
    observacion VARCHAR2(300),
    fechaMovimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT movimiento_inventario_pk PRIMARY KEY (idMovimiento),
    CONSTRAINT mi_producto_fk FOREIGN KEY (idProducto) REFERENCES producto(idProducto),
    CONSTRAINT mi_item_fk FOREIGN KEY (idItem) REFERENCES item(idItem),
    CONSTRAINT mi_consumo_producto_fk FOREIGN KEY (idConsumo, idProducto) REFERENCES consumo(idConsumo, idProducto),
    CONSTRAINT mi_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT mi_detalle_hab_fk FOREIGN KEY (idDetalleHabitacion) REFERENCES detalle_habitacion(idDetalleHabitacion),
    CONSTRAINT mi_tipo_almacen_fk FOREIGN KEY (idTipoMovimientoAlmacen) REFERENCES tipo_movimiento_almacen(idTipoMovimientoAlmacen),
    CONSTRAINT mi_consumo_unico_uq UNIQUE (idConsumo),
    CONSTRAINT mi_arco_chk CHECK ((idProducto IS NOT NULL AND idItem IS NULL) OR (idProducto IS NULL AND idItem IS NOT NULL)),
    CONSTRAINT mi_stock_chk CHECK (stockResultante >= 0),
    CONSTRAINT mi_consumo_producto_chk CHECK (idConsumo IS NULL OR idProducto IS NOT NULL)
);

CREATE TABLE flujo_caja (
    idFlujo NUMBER GENERATED ALWAYS AS IDENTITY,
    idTurno NUMBER NOT NULL,
    idTipoMovimiento NUMBER NOT NULL,
    idPago NUMBER,
    concepto VARCHAR2(1000) NOT NULL,
    monto NUMBER(10,2) NOT NULL,
    fechaHora TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    documentoRespaldo VARCHAR2(100),
    evidenciaRuta VARCHAR2(300),
    evidenciaSha256 CHAR(64),
    CONSTRAINT flujo_caja_pk PRIMARY KEY (idFlujo),
    CONSTRAINT flujo_turno_fk FOREIGN KEY (idTurno) REFERENCES turno_caja(idTurno),
    CONSTRAINT flujo_tipo_fk FOREIGN KEY (idTipoMovimiento) REFERENCES tipo_movimiento_caja(idTipoMovimiento),
    CONSTRAINT flujo_pago_fk FOREIGN KEY (idPago) REFERENCES pago(idPago),
    CONSTRAINT flujo_monto_posotivo_chk CHECK (monto > 0)
);

CREATE TABLE arqueo_caja (
    idArqueo NUMBER GENERATED ALWAYS AS IDENTITY,
    idTurno NUMBER NOT NULL,
    idFormaPago NUMBER NOT NULL,
    montoCalculado NUMBER(10,2) NOT NULL,
    montoDeclarado NUMBER(10,2) NOT NULL,
    diferencia NUMBER(10,2) GENERATED ALWAYS AS (montoDeclarado - montoCalculado) VIRTUAL,
    CONSTRAINT arqueo_caja_pk PRIMARY KEY (idArqueo),
    CONSTRAINT arqueo_calculado_chk CHECK (montoCalculado >= 0),
    CONSTRAINT arqueo_declarado_chk CHECK (montoDeclarado >= 0),
    CONSTRAINT arqueo_turno_forma_unq UNIQUE (idTurno, idFormaPago),
    CONSTRAINT arqueo_turno_fk FOREIGN KEY (idTurno) REFERENCES turno_caja(idTurno),
    CONSTRAINT arqueo_pago_fk FOREIGN KEY (idFormaPago) REFERENCES forma_pago(idFormaPago)
);

/* ================= SECCION 8: REVISION Y MANTENIMIENTO ================= */

CREATE TABLE informe_revision (
    idInformeRevision NUMBER GENERATED ALWAYS AS IDENTITY,
    idReserva NUMBER,
    idHabitacion NUMBER NOT NULL,
    idUsuario NUMBER NOT NULL,
    fechaHora TIMESTAMP NOT NULL,
    observaciones VARCHAR2(1000) NOT NULL,
    CONSTRAINT informe_revision_pk PRIMARY KEY (idInformeRevision),
    CONSTRAINT informe_habitacion_fk FOREIGN KEY (idHabitacion) REFERENCES habitacion(idHabitacion),
    CONSTRAINT informe_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT informe_reserva_fk FOREIGN KEY (idReserva) REFERENCES reserva(idReserva)
);

CREATE TABLE detalle_informe (
    idDetalleInforme NUMBER GENERATED ALWAYS AS IDENTITY,
    idInforme NUMBER NOT NULL,
    idDetalleHabitacion NUMBER NOT NULL,
    estadoEncontrado VARCHAR2(30) NOT NULL,
    evidenciaRuta VARCHAR2(300),
    evidenciaSha256 CHAR(64),
    CONSTRAINT detalle_informe_pk PRIMARY KEY (idDetalleInforme),
    CONSTRAINT di_informe_fk FOREIGN KEY (idInforme) REFERENCES informe_revision(idInformeRevision) ON DELETE CASCADE,
    CONSTRAINT di_detalle_habitacion_fk FOREIGN KEY (idDetalleHabitacion) REFERENCES detalle_habitacion(idDetalleHabitacion),
    CONSTRAINT di_informe_item_unq UNIQUE (idInforme, idDetalleHabitacion),
    CONSTRAINT di_estado_encontrado_chk CHECK (estadoEncontrado IN ('BUENO', 'DANADO', 'FALTANTE', 'EN_REPARACION'))
);

CREATE TABLE ticket_mantenimiento (
    idTicket NUMBER GENERATED ALWAYS AS IDENTITY,
    idDetalleInforme NUMBER NOT NULL,
    idEmpleado NUMBER,
    tipoAccion VARCHAR2(30) NOT NULL,
    estadoTicket VARCHAR2(30) NOT NULL,
    fechaGeneracion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fechaResolucion TIMESTAMP,
    costoReparacion NUMBER(10,2),
    evidenciaRuta VARCHAR2(300),
    evidenciaSha256 CHAR(64),
    observacionTecnica VARCHAR2(1000),
    CONSTRAINT ticket_mantenimiento_pk PRIMARY KEY (idTicket),
    CONSTRAINT ticket_costo_chk CHECK (costoReparacion >= 0),
    CONSTRAINT ticket_fechas_chk CHECK (fechaResolucion IS NULL OR fechaResolucion >= fechaGeneracion),
    CONSTRAINT ticket_detalle_fk FOREIGN KEY (idDetalleInforme) REFERENCES detalle_informe(idDetalleInforme),
    CONSTRAINT ticket_empleado_fk FOREIGN KEY (idEmpleado) REFERENCES empleado(idEmpleado),
    CONSTRAINT ticket_detalle_unq UNIQUE (idDetalleInforme),
    CONSTRAINT ticket_estado_chk CHECK (estadoTicket IN ('PENDIENTE', 'ASIGNADO', 'EN_PROCESO', 'RESUELTO', 'CANCELADO')),
    CONSTRAINT ticket_tipo_accion_chk CHECK (tipoAccion IN ('REPARACION', 'REPOSICION'))
);

/* ================= SECCION 9: AUDITORIA CENTRALIZADA ================= */

CREATE TABLE auditoria_general (
    idAuditoria NUMBER GENERATED ALWAYS AS IDENTITY,
    nombreTabla VARCHAR2(50) NOT NULL,
    idRegistro NUMBER NOT NULL,
    accion VARCHAR2(10) NOT NULL,
    idUsuario NUMBER,
    datosAnteriores CLOB,
    datosNuevos CLOB,
    ipOrigen VARCHAR2(45),
    fechaHora TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT auditoria_pk PRIMARY KEY (idAuditoria),
    CONSTRAINT auditoria_accion_chk CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT auditoria_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario)
);
