-- Descripción opcional: preserva todos los productos y existencias actuales.
DECLARE
  cantidad NUMBER;
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
    RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
  END IF;
  SELECT COUNT(*) INTO cantidad FROM user_tab_columns
    WHERE table_name='PRODUCTO' AND column_name='DESCRIPCION';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE producto ADD (descripcion VARCHAR2(500 CHAR))';
  END IF;
END;
/
