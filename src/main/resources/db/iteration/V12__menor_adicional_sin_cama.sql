-- Política confirmada: plazas existentes + hasta un menor de 7 años sin cama.
-- Solo el inventario real. No modifica camas, precios, reservas ni pagos.
-- Ejecutar en una transacción y confirmar después de verificar el inventario.
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
    RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
  END IF;
  UPDATE tipo_habitacion t SET capacidadMax=GREATEST(t.capacidadMax,
    (SELECT MAX(GREATEST(h.capacidadPersonas,h.plazasCama+1)) FROM habitacion h
     WHERE h.idTipo=t.idTipoHabitacion AND h.numeroCuarto IN
      ('201','202','203','301','302','303','304','401','402','403','404','405','406','501','502','503','504','505','506','507')))
  WHERE EXISTS (SELECT 1 FROM habitacion h WHERE h.idTipo=t.idTipoHabitacion
    AND h.numeroCuarto IN ('201','202','203','301','302','303','304','401','402','403','404','405','406','501','502','503','504','505','506','507'));
  UPDATE habitacion SET capacidadPersonas=GREATEST(capacidadPersonas,plazasCama+1),maxMenoresSinCama=1
  WHERE numeroCuarto IN ('201','202','203','301','302','303','304','401','402','403','404','405','406','501','502','503','504','505','506','507')
    AND (capacidadPersonas<plazasCama+1 OR maxMenoresSinCama<>1);
END;
/
