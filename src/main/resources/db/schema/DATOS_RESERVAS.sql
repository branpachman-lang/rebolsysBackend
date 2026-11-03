-- Preparación explícita de catálogos de reservas para desarrollo. No arranca automáticamente.
-- Ejecutar en PropuestaHospedajeRB y confirmar con COMMIT.
BEGIN
    IF UPPER(SYS_CONTEXT('USERENV','CURRENT_SCHEMA')) <> 'PROPUESTAHOSPEDAJERB' THEN
        RAISE_APPLICATION_ERROR(-20001,'Conectarse al esquema PropuestaHospedajeRB');
    END IF;
    MERGE INTO tipo_comprobante t USING (
        SELECT 'CUENTA' codigo,'Cuenta interna de desarrollo' nombre FROM dual UNION ALL
        SELECT 'BOLETA','Boleta interna de desarrollo' FROM dual UNION ALL
        SELECT 'FACTURA','Factura interna de desarrollo' FROM dual
    ) s ON(t.codigo=s.codigo)
    WHEN NOT MATCHED THEN INSERT(codigo,nombreComprobante) VALUES(s.codigo,s.nombre);
    MERGE INTO serie_comprobante t USING (
        SELECT idTipoComprobante, CASE codigo WHEN 'CUENTA' THEN 'CTA'
            WHEN 'BOLETA' THEN 'BINT' ELSE 'FINT' END serie
        FROM tipo_comprobante WHERE codigo IN ('CUENTA','BOLETA','FACTURA')
    ) s ON(t.idTipoComprobante=s.idTipoComprobante AND t.serie=s.serie)
    WHEN NOT MATCHED THEN INSERT(idTipoComprobante,serie,siguienteNumero)
        VALUES(s.idTipoComprobante,s.serie,1);
    MERGE INTO forma_pago t USING (SELECT 'EFECTIVO' nombre FROM dual) s ON(t.nombrePago=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombrePago) VALUES(s.nombre);
    MERGE INTO tipo_movimiento_caja t USING (SELECT 'COBRO_RESERVA' nombre FROM dual) s ON(t.nombreMovimiento=s.nombre)
    WHEN NOT MATCHED THEN INSERT(nombreMovimiento,signo) VALUES(s.nombre,1);
END;
/
