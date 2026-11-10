-- Catálogos operativos y cuenta de gerencia para el esquema nuevo de desarrollo.
DECLARE
  v_perfil NUMBER;
  v_empleado NUMBER;
BEGIN
  IF UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'REBOLSYSDBV2' THEN
    RAISE_APPLICATION_ERROR(-20042, 'Ejecuta la carga en el esquema local REBOLSYSDBV2');
  END IF;

  MERGE INTO tipo_documento t USING (
    SELECT 'DNI' nombre, '1' codigo FROM dual UNION ALL SELECT 'RUC','6' FROM dual
    UNION ALL SELECT 'CARNET EXTRANJERIA','4' FROM dual UNION ALL SELECT 'PASAPORTE','7' FROM dual
    UNION ALL SELECT 'INTERNO',NULL FROM dual
  ) s ON (t.nombreDocumento = s.nombre)
  WHEN NOT MATCHED THEN INSERT (nombreDocumento,codigoSunat) VALUES (s.nombre,s.codigo);

  MERGE INTO tipo_movimiento_almacen t USING (
    SELECT 'AJ_ENT' codigo,'Ajuste de entrada' nombre,1 signo FROM dual
    UNION ALL SELECT 'AJ_SAL','Ajuste de salida',-1 FROM dual
    UNION ALL SELECT 'VENTA','Salida por venta',-1 FROM dual
    UNION ALL SELECT 'DEV_VENTA','Devolución de venta',1 FROM dual
    UNION ALL SELECT 'ASIG_ITEM','Asignación de mobiliario',0 FROM dual
    UNION ALL SELECT 'RET_ITEM','Retiro de mobiliario',0 FROM dual
    UNION ALL SELECT 'EST_ITEM','Estado de conservación de mobiliario',0 FROM dual
  ) s ON (t.codigo = s.codigo)
  WHEN NOT MATCHED THEN INSERT (codigo,nombreMovimiento,signo) VALUES (s.codigo,s.nombre,s.signo);

  MERGE INTO tipo_movimiento_caja t USING (
    SELECT 'GASTO' nombre,-1 signo FROM dual UNION ALL SELECT 'INGRESO_MANUAL',1 FROM dual
    UNION ALL SELECT 'COBRO_RESERVA',1 FROM dual UNION ALL SELECT 'COBRO_VENTA',1 FROM dual
  ) s ON (t.nombreMovimiento = s.nombre)
  WHEN NOT MATCHED THEN INSERT (nombreMovimiento,signo) VALUES (s.nombre,s.signo);

  MERGE INTO forma_pago t USING (
    SELECT 'EFECTIVO' nombre FROM dual UNION ALL SELECT 'TARJETA' FROM dual
    UNION ALL SELECT 'YAPE' FROM dual UNION ALL SELECT 'PLIN' FROM dual
  ) s ON (UPPER(TRIM(t.nombrePago)) = s.nombre)
  WHEN NOT MATCHED THEN INSERT (nombrePago) VALUES (s.nombre);

  MERGE INTO tipo_comprobante t USING (
    SELECT 'CUENTA' codigo, 'Cuenta interna' nombre, CAST(NULL AS VARCHAR2(2)) sunat FROM dual
    UNION ALL SELECT 'BOLETA','Boleta','03' FROM dual UNION ALL SELECT 'FACTURA','Factura','01' FROM dual
  ) s ON (t.codigo = s.codigo)
  WHEN NOT MATCHED THEN INSERT (codigo,nombreComprobante,codigoSunat) VALUES (s.codigo,s.nombre,s.sunat);

  MERGE INTO serie_comprobante t USING (
    SELECT idTipoComprobante, CASE codigo WHEN 'CUENTA' THEN 'CTA' WHEN 'BOLETA' THEN 'BINT' ELSE 'FINT' END serie
    FROM tipo_comprobante WHERE codigo IN ('CUENTA','BOLETA','FACTURA')
  ) s ON (t.idTipoComprobante = s.idTipoComprobante AND t.serie = s.serie)
  WHEN NOT MATCHED THEN INSERT (idTipoComprobante,serie,siguienteNumero) VALUES (s.idTipoComprobante,s.serie,1);

  INSERT INTO perfil (idTipoDocumento,numeroDocumento,esCliente)
  SELECT t.idTipoDocumento, 'GERENCIA_LOCAL', 0 FROM tipo_documento t WHERE t.nombreDocumento = 'INTERNO'
    AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.idTipoDocumento = t.idTipoDocumento AND p.numeroDocumento = 'GERENCIA_LOCAL');
  SELECT p.idPerfil INTO v_perfil FROM perfil p JOIN tipo_documento t ON t.idTipoDocumento = p.idTipoDocumento
  WHERE t.nombreDocumento = 'INTERNO' AND p.numeroDocumento = 'GERENCIA_LOCAL';

  INSERT INTO persona_natural (idPerfil,nombres,apellidos,fechaNacimiento)
  SELECT v_perfil,'Gerencia','Local de desarrollo',DATE '1990-01-01' FROM dual
  WHERE NOT EXISTS (SELECT 1 FROM persona_natural n WHERE n.idPerfil = v_perfil);

  INSERT INTO empleado (idPerfil,fechaContratacion)
  SELECT v_perfil,TRUNC(SYSDATE) FROM dual WHERE NOT EXISTS (SELECT 1 FROM empleado e WHERE e.idPerfil = v_perfil);
  SELECT idEmpleado INTO v_empleado FROM empleado WHERE idPerfil = v_perfil;

  INSERT INTO usuario (idEmpleado,username,estadoUsuario)
  SELECT v_empleado,'gerencia_local','ACTIVO' FROM dual
  WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.idEmpleado = v_empleado OR u.username = 'gerencia_local');
END;
/
