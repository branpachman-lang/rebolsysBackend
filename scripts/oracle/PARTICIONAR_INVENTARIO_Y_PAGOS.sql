-- Ejecutar como script en una conexión de MODREBOLSYS después de guardar los planes de referencia.
-- Los ALTER TABLE confirman transacciones en Oracle. Ejecutar con la carga de datos ya confirmada.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_medios NUMBER;
BEGIN
    IF SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> 'MODREBOLSYS' THEN
        RAISE_APPLICATION_ERROR(-20030, 'Ejecuta el script en el esquema MODREBOLSYS.');
    END IF;

    SELECT COUNT(*) INTO v_medios FROM forma_pago
    WHERE (idFormaPago = 1 AND UPPER(TRIM(nombrePago)) = 'EFECTIVO')
       OR (idFormaPago = 2 AND UPPER(TRIM(nombrePago)) = 'PLIN')
       OR (idFormaPago = 3 AND UPPER(TRIM(nombrePago)) = 'TARJETA')
       OR (idFormaPago = 4 AND UPPER(TRIM(nombrePago)) = 'YAPE');
    IF v_medios <> 4 THEN
        RAISE_APPLICATION_ERROR(-20031, 'Revisa los IDs en FORMA_PAGO y ajusta los VALUES de las particiones.');
    END IF;
END;
/

-- 1. Inventario por fecha. Los límites superiores de RANGE son exclusivos.
ALTER TABLE movimiento_inventario MODIFY
    PARTITION BY RANGE (fechaMovimiento) (
        PARTITION p_mi_historico VALUES LESS THAN (TIMESTAMP '2026-10-01 00:00:00'),
        PARTITION p_mi_202610 VALUES LESS THAN (TIMESTAMP '2026-11-01 00:00:00'),
        PARTITION p_mi_202611 VALUES LESS THAN (TIMESTAMP '2026-12-01 00:00:00'),
        PARTITION p_mi_futuro VALUES LESS THAN (MAXVALUE)
    )
    UPDATE INDEXES (pn_mi_pk GLOBAL, pn_mi_clave_uq GLOBAL);

-- 2. Pagos por medio. DEFAULT admite medios que se creen después.
ALTER TABLE pago MODIFY
    PARTITION BY LIST (idFormaPago) (
        PARTITION p_pago_efectivo VALUES (1),
        PARTITION p_pago_plin VALUES (2),
        PARTITION p_pago_tarjeta VALUES (3),
        PARTITION p_pago_yape VALUES (4),
        PARTITION p_pago_otros VALUES (DEFAULT)
    )
    UPDATE INDEXES (pn_pago_pk GLOBAL, pn_pago_clave_uq GLOBAL);

SELECT table_name, partition_name, partition_position
FROM user_tab_partitions
WHERE table_name IN ('MOVIMIENTO_INVENTARIO', 'PAGO')
ORDER BY table_name, partition_position;

SELECT 'MOVIMIENTO_INVENTARIO' tabla, COUNT(*) filas FROM movimiento_inventario
UNION ALL
SELECT 'PAGO', COUNT(*) FROM pago;

WHENEVER SQLERROR CONTINUE NONE
