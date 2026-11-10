-- Complemento para el esquema nuevo basado en ITEM y RESERVA_HUESPED.
DECLARE
  v_tablas NUMBER;

  PROCEDURE agregar_columna(p_tabla VARCHAR2, p_columna VARCHAR2, p_ddl VARCHAR2) IS
    v_existe NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_tab_columns WHERE table_name = p_tabla AND column_name = p_columna;
    IF v_existe = 0 THEN EXECUTE IMMEDIATE p_ddl; END IF;
  END;

  PROCEDURE agregar_restriccion(p_nombre VARCHAR2, p_ddl VARCHAR2) IS
    v_existe NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_constraints WHERE constraint_name = p_nombre;
    IF v_existe = 0 THEN EXECUTE IMMEDIATE p_ddl; END IF;
  END;

  PROCEDURE retirar_restriccion(p_tabla VARCHAR2, p_nombre VARCHAR2) IS
    v_existe NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_constraints WHERE table_name = p_tabla AND constraint_name = p_nombre;
    IF v_existe = 1 THEN EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabla || ' DROP CONSTRAINT ' || p_nombre; END IF;
  END;
BEGIN
  SELECT COUNT(*) INTO v_tablas FROM user_tables WHERE table_name IN ('HUESPED', 'RESERVA_HUESPED', 'DETALLE_HABITACION');
  IF v_tablas <> 2 OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'REBOLSYSDBV2' THEN
    RAISE_APPLICATION_ERROR(-20040, 'Aplica el complemento en el esquema nuevo REBOLSYSDBV2');
  END IF;
  SELECT COUNT(*) INTO v_tablas FROM user_tables WHERE table_name = 'DETALLE_HABITACION';
  IF v_tablas > 0 THEN
    RAISE_APPLICATION_ERROR(-20041, 'El esquema todavía usa DETALLE_HABITACION');
  END IF;

  SELECT COUNT(*) INTO v_tablas FROM user_tables WHERE table_name = 'EMISION_ELECTRONICA';
  IF v_tablas = 0 THEN
    EXECUTE IMMEDIATE q'[CREATE TABLE "EMISION_ELECTRONICA" (
    idEmision NUMBER GENERATED ALWAYS AS IDENTITY,
    idComprobante NUMBER NOT NULL,
    idUsuario NUMBER NOT NULL,
    claveOperacion VARCHAR2(64) NOT NULL,
    firmaSolicitud VARCHAR2(64) NOT NULL,
    estado VARCHAR2(20) NOT NULL,
    ambiente VARCHAR2(10) DEFAULT 'BETA' NOT NULL,
    rucEmisor VARCHAR2(11) NOT NULL,
    tipoDocumento VARCHAR2(2) NOT NULL,
    serie VARCHAR2(4) NOT NULL,
    correlativo NUMBER(8) NOT NULL,
    fechaEmision TIMESTAMP NOT NULL,
    fechaActualizacion TIMESTAMP NOT NULL,
    intentos NUMBER(6) DEFAULT 0 NOT NULL,
    solicitud CLOB NOT NULL,
    xmlFirmado CLOB,
    cdrZip CLOB,
    hashCpe VARCHAR2(100),
    codigoRespuesta VARCHAR2(20),
    mensajeRespuesta VARCHAR2(500),
    subtotal NUMBER(12,2) NOT NULL,
    igv NUMBER(12,2) NOT NULL,
    total NUMBER(12,2) NOT NULL,
    CONSTRAINT ee_pk PRIMARY KEY (idEmision),
    CONSTRAINT ee_comprobante_uq UNIQUE (idComprobante),
    CONSTRAINT ee_clave_uq UNIQUE (claveOperacion),
    CONSTRAINT ee_numero_uq UNIQUE (rucEmisor,tipoDocumento,serie,correlativo),
    CONSTRAINT ee_comprobante_fk FOREIGN KEY (idComprobante) REFERENCES comprobante(idComprobante),
    CONSTRAINT ee_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT ee_ambiente_ck CHECK (ambiente='BETA'),
    CONSTRAINT ee_tipo_ck CHECK (tipoDocumento IN ('01','03')),
    CONSTRAINT ee_estado_ck CHECK (estado IN ('PREPARADO','ENVIANDO','ACEPTADO','RECHAZADO','INCIERTO','ERROR_ENVIO')),
    CONSTRAINT ee_montos_ck CHECK (subtotal>=0 AND igv>=0 AND total>0 AND total=subtotal+igv),
    CONSTRAINT ee_numero_ck CHECK (correlativo BETWEEN 1 AND 99999999 AND intentos>=0)
)]';
  END IF;

  agregar_columna('DETALLE_INFORME', 'CONDICIONANTERIOR',
    'ALTER TABLE detalle_informe ADD (condicionAnterior VARCHAR2(30))');
  agregar_columna('MOVIMIENTO_INVENTARIO', 'CONDICIONITEM',
    'ALTER TABLE movimiento_inventario ADD (condicionItem VARCHAR2(30))');

  agregar_restriccion('DI_ITEM_FK',
    'ALTER TABLE detalle_informe ADD CONSTRAINT di_item_fk FOREIGN KEY (idItem) REFERENCES item(idItem)');
  agregar_restriccion('PN_MI_ITEM_FK',
    'ALTER TABLE movimiento_inventario ADD CONSTRAINT pn_mi_item_fk FOREIGN KEY (idItem) REFERENCES item(idItem)');
  agregar_restriccion('PN_MI_HABITACION_FK',
    'ALTER TABLE movimiento_inventario ADD CONSTRAINT pn_mi_habitacion_fk FOREIGN KEY (idHabitacion) REFERENCES habitacion(idHabitacion)');
  retirar_restriccion('DETALLE_INFORME', 'DI_ITEM_HABITACION_FK');
  retirar_restriccion('MOVIMIENTO_INVENTARIO', 'PN_MI_ITEM_HABITACION_FK');

  agregar_restriccion('ITEM_CONDICION_V2_CHK',
    q'[ALTER TABLE item ADD CONSTRAINT item_condicion_v2_chk CHECK (condicionItem IN ('OPTIMO','DANADO','FALTANTE','EN_REPARACION'))]');
  retirar_restriccion('ITEM', 'ITEM_CONDICION_CHK');
  agregar_restriccion('DI_ESTADO_V2_CHK',
    q'[ALTER TABLE detalle_informe ADD CONSTRAINT di_estado_v2_chk CHECK (estadoEncontrado IN ('OPTIMO','DANADO','FALTANTE','EN_REPARACION'))]');
  retirar_restriccion('DETALLE_INFORME', 'DI_ESTADO_ENCONTRADO_CHK');
  agregar_restriccion('INFORME_ESTADO_V2_CHK',
    q'[ALTER TABLE informe_revision ADD CONSTRAINT informe_estado_v2_chk CHECK (estadoInforme IN ('BORRADOR','POR_APROBAR','APROBADO','INVALIDADO'))]');
  retirar_restriccion('INFORME_REVISION', 'INFORME_ESTADO_CHK');

  -- Columnas que conserva el proyecto para tarifas, tienda, caja y credenciales temporales.
  agregar_columna('HABITACION', 'PRECIOPORNOCHE', 'ALTER TABLE habitacion ADD (precioPorNoche NUMBER(10,2))');
  agregar_columna('PRODUCTO', 'DESCRIPCION', 'ALTER TABLE producto ADD (descripcion VARCHAR2(500 CHAR))');
  agregar_columna('FLUJO_CAJA', 'CLAVEREGISTRO', 'ALTER TABLE flujo_caja ADD (claveRegistro VARCHAR2(64))');
  agregar_restriccion('PN_FLUJO_CLAVE_UQ', 'ALTER TABLE flujo_caja ADD CONSTRAINT pn_flujo_clave_uq UNIQUE (claveRegistro)');
  agregar_columna('USUARIO_CREDENCIAL', 'FECHAVENCIMIENTO', 'ALTER TABLE usuario_credencial ADD (fechaVencimiento TIMESTAMP)');
END;
/
