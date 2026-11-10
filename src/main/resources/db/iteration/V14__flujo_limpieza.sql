-- Amplía el esquema de la propuesta. Los informes anteriores se conservan como historial aprobado.
DECLARE
  cantidad NUMBER;
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
    RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
  END IF;

  SELECT COUNT(*) INTO cantidad FROM user_tab_columns
    WHERE table_name='INFORME_REVISION' AND column_name='ESTADOINFORME';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE informe_revision ADD (estadoInforme VARCHAR2(20) DEFAULT 'APROBADO' NOT NULL)]';
  END IF;

  SELECT COUNT(*) INTO cantidad FROM user_constraints WHERE constraint_name='INFORME_ESTADO_CHK';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE informe_revision ADD CONSTRAINT informe_estado_chk
      CHECK (estadoInforme IN ('BORRADOR', 'POR_APROBAR', 'APROBADO'))]';
  END IF;

  SELECT COUNT(*) INTO cantidad FROM user_tab_columns
    WHERE table_name='DETALLE_INFORME' AND column_name='OBSERVACIONES';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE detalle_informe ADD (observaciones VARCHAR2(1000))';
  END IF;

  SELECT COUNT(*) INTO cantidad FROM user_indexes WHERE index_name='INFORME_ACTIVO_HAB_UNQ';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE q'[CREATE UNIQUE INDEX informe_activo_hab_unq ON informe_revision
      (CASE WHEN estadoInforme IN ('BORRADOR', 'POR_APROBAR') THEN idHabitacion END)]';
  END IF;
END;
/
