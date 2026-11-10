-- Conserva las credenciales existentes. Las nuevas contraseñas temporales vencen.
DECLARE
  cantidad NUMBER;
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
    RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
  END IF;
  SELECT COUNT(*) INTO cantidad FROM user_tab_columns
    WHERE table_name='USUARIO_CREDENCIAL' AND column_name='FECHAVENCIMIENTO';
  IF cantidad=0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE usuario_credencial ADD (fechaVencimiento TIMESTAMP)';
  END IF;
END;
/
