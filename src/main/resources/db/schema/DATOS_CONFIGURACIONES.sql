-- Preparación explícita de conservación de mobiliario. Confirmar con COMMIT.
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    MERGE INTO tipo_movimiento_almacen t USING (
        SELECT 'EST_ITEM' codigo,'Estado de conservacion de mobiliario' nombre FROM dual
    ) s ON(t.codigo=s.codigo)
    WHEN NOT MATCHED THEN INSERT(codigo,nombreMovimiento,signo) VALUES(s.codigo,s.nombre,0);
END;
/
