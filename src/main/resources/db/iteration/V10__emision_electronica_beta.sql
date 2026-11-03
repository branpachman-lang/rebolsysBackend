-- Ejecutar solo en PropuestaHospedajeRB. DDL Oracle confirma implícitamente.
DECLARE
    cantidad NUMBER;
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    SELECT COUNT(*) INTO cantidad FROM user_tables WHERE table_name='EMISION_ELECTRONICA';
    IF cantidad=0 THEN
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
END;
/
