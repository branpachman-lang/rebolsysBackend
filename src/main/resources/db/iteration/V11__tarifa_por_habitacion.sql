-- Tarifa opcional por habitación; las existentes conservan la tarifa del tipo.
-- Ejecutar en PropuestaHospedajeRB antes de iniciar el backend actualizado.
DECLARE
  cantidad NUMBER;
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
    RAISE_APPLICATION_ERROR(-20001, 'Conectarse al esquema PropuestaHospedajeRB');
  END IF;
  SELECT COUNT(*) INTO cantidad FROM user_tab_columns
    WHERE table_name = 'HABITACION' AND column_name = 'PRECIOPORNOCHE';
  IF cantidad = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE habitacion ADD (precioPorNoche NUMBER(10,2))';
  END IF;
  SELECT COUNT(*) INTO cantidad FROM user_constraints
    WHERE constraint_name = 'HABITACION_PRECIO_NOCHE_CHK';
  IF cantidad = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE habitacion ADD CONSTRAINT habitacion_precio_noche_chk CHECK (precioPorNoche IS NULL OR precioPorNoche >= 0)';
  END IF;
END;
/
