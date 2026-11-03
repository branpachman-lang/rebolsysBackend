-- Preparación explícita de categorías para desarrollo. Confirmar con COMMIT.
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    MERGE INTO tipo_movimiento_caja t USING (
        SELECT 'TAXIS Y MOVILIDAD' nombre FROM dual UNION ALL
        SELECT 'REPUESTOS Y MANTENIMIENTO' FROM dual UNION ALL
        SELECT 'SUMINISTROS' FROM dual UNION ALL
        SELECT 'OTROS GASTOS' FROM dual
    ) s ON(t.nombreMovimiento=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreMovimiento,signo) VALUES(s.nombre,-1);
END;
/
