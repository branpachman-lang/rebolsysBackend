-- Carga inicial OPCIONAL para desarrollo local; NO se ejecuta al arrancar.
-- Ejecutar conectado a PropuestaHospedajeRB, después del DDL propuesto.
-- No contiene contraseñas, no modifica tablas ni reemplaza registros existentes.
-- El operador local identifica movimientos; no permite iniciar sesión.
DECLARE
    v_tipo NUMBER; v_perfil NUMBER; v_empleado NUMBER; v_usuario NUMBER; v_n NUMBER;
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    MERGE INTO tipo_documento t USING (
        SELECT 'DNI' nombre, '1' codigo FROM dual UNION ALL
        SELECT 'RUC','6' FROM dual UNION ALL
        SELECT 'CARNET EXTRANJERIA','4' FROM dual UNION ALL
        SELECT 'PASAPORTE','7' FROM dual UNION ALL
        SELECT 'INTERNO',NULL FROM dual
    ) s ON (t.nombreDocumento=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreDocumento,codigoSunat) VALUES(s.nombre,s.codigo);
    MERGE INTO tipo_movimiento_almacen t USING (
        SELECT 'AJ_ENT' codigo,'Ajuste de entrada' nombre,1 signo FROM dual UNION ALL
        SELECT 'AJ_SAL','Ajuste de salida',-1 FROM dual UNION ALL
        SELECT 'VENTA','Salida por venta',-1 FROM dual UNION ALL
        SELECT 'DEV_VENTA','Devolucion de venta',1 FROM dual UNION ALL
        SELECT 'ASIG_ITEM','Asignacion de mobiliario',0 FROM dual UNION ALL
        SELECT 'RET_ITEM','Retiro de mobiliario',0 FROM dual
    ) s ON (t.codigo=s.codigo)
    WHEN NOT MATCHED THEN INSERT(codigo,nombreMovimiento,signo) VALUES(s.codigo,s.nombre,s.signo);
    MERGE INTO tipo_movimiento_caja t USING (SELECT 'GASTO' nombre,-1 signo FROM dual) s ON(t.nombreMovimiento=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreMovimiento,signo) VALUES(s.nombre,s.signo);
    MERGE INTO tipo_habitacion t USING (
        SELECT 'Simple' nombre,2 capacidad,80 precio FROM dual UNION ALL
        SELECT 'Doble',4,120 FROM dual UNION ALL SELECT 'Familiar',6,160 FROM dual
    ) s ON(t.nombreTipo=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreTipo,capacidadMax,precioBase) VALUES(s.nombre,s.capacidad,s.precio);
    MERGE INTO tipo_item t USING (
        SELECT 'Cama' nombre FROM dual UNION ALL SELECT 'Televisor' FROM dual UNION ALL SELECT 'Mesa' FROM dual
    ) s ON(t.nombreItem=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreItem) VALUES(s.nombre);
    MERGE INTO categoria t USING (SELECT 'General' nombre FROM dual) s ON(t.nombreCategoria=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreCategoria) VALUES(s.nombre);
    SELECT COUNT(*) INTO v_n FROM usuario WHERE username='operacion_local';
    IF v_n=0 THEN
        SELECT idTipoDocumento INTO v_tipo FROM tipo_documento WHERE nombreDocumento='INTERNO';
        SELECT COUNT(*) INTO v_n FROM perfil WHERE idTipoDocumento=v_tipo AND numeroDocumento='OPERADOR_LOCAL';
        IF v_n=0 THEN
            INSERT INTO perfil(idTipoDocumento,numeroDocumento) VALUES(v_tipo,'OPERADOR_LOCAL') RETURNING idPerfil INTO v_perfil;
            INSERT INTO persona_natural(idPerfil,nombres,apellidos,fechaNacimiento) VALUES(v_perfil,'Operador','Local de desarrollo',DATE '1990-01-01');
        ELSE SELECT idPerfil INTO v_perfil FROM perfil WHERE idTipoDocumento=v_tipo AND numeroDocumento='OPERADOR_LOCAL';
        END IF;
        SELECT COUNT(*) INTO v_n FROM empleado WHERE idPerfil=v_perfil;
        IF v_n=0 THEN
            INSERT INTO empleado(idPerfil,fechaContratacion) VALUES(v_perfil,TRUNC(SYSDATE)) RETURNING idEmpleado INTO v_empleado;
        ELSE SELECT idEmpleado INTO v_empleado FROM empleado WHERE idPerfil=v_perfil;
        END IF;
        INSERT INTO usuario(idEmpleado,username,estadoUsuario) VALUES(v_empleado,'operacion_local','ACTIVO') RETURNING idUsuario INTO v_usuario;
    ELSE SELECT idUsuario INTO v_usuario FROM usuario WHERE username='operacion_local';
    END IF;
    SELECT COUNT(*) INTO v_n FROM turno_caja WHERE idUsuario=v_usuario AND estadoSesion=1 AND fechaCierre IS NULL;
    IF v_n=0 THEN INSERT INTO turno_caja(idUsuario,montoBase,estadoSesion) VALUES(v_usuario,0,1); END IF;
END;
/
-- Revisar y confirmar con COMMIT; o ROLLBACK para descartar esta carga.
SELECT idUsuario,username,estadoUsuario FROM usuario WHERE username='operacion_local';
SELECT idTurno,idUsuario,estadoSesion,fechaCierre FROM turno_caja WHERE fechaCierre IS NULL;
