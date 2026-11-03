-- Preparación de cobros directos de productos. Confirmar con COMMIT.
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    MERGE INTO tipo_movimiento_caja t USING (
        SELECT 'COBRO_VENTA' nombre FROM dual
    ) s ON(t.nombreMovimiento=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreMovimiento,signo) VALUES(s.nombre,1);
END;
/
