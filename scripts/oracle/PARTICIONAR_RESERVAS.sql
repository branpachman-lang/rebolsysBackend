-- Ejecutar como script en MODREBOLSYS después de guardar el plan de referencia.
-- Se particiona DETALLE_RESERVA, donde vive la fecha de ingreso de cada habitación reservada.
-- El ALTER TABLE confirma transacciones en Oracle. Ejecutar con la carga ya confirmada.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

BEGIN
    IF SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> 'MODREBOLSYS' THEN
        RAISE_APPLICATION_ERROR(-20030, 'Ejecuta el script en el esquema MODREBOLSYS.');
    END IF;
END;
/

ALTER TABLE detalle_reserva MODIFY
    PARTITION BY RANGE (fechaIngreso) (
        PARTITION p_dr_historico VALUES LESS THAN (TIMESTAMP '2026-06-01 00:00:00'),
        PARTITION p_dr_202606 VALUES LESS THAN (TIMESTAMP '2026-07-01 00:00:00'),
        PARTITION p_dr_202607 VALUES LESS THAN (TIMESTAMP '2026-08-01 00:00:00'),
        PARTITION p_dr_202608 VALUES LESS THAN (TIMESTAMP '2026-09-01 00:00:00'),
        PARTITION p_dr_202609 VALUES LESS THAN (TIMESTAMP '2026-10-01 00:00:00'),
        PARTITION p_dr_202610 VALUES LESS THAN (TIMESTAMP '2026-11-01 00:00:00'),
        PARTITION p_dr_202611 VALUES LESS THAN (TIMESTAMP '2026-12-01 00:00:00'),
        PARTITION p_dr_202612 VALUES LESS THAN (TIMESTAMP '2027-01-01 00:00:00'),
        PARTITION p_dr_202701 VALUES LESS THAN (TIMESTAMP '2027-02-01 00:00:00'),
        PARTITION p_dr_202702 VALUES LESS THAN (TIMESTAMP '2027-03-01 00:00:00'),
        PARTITION p_dr_futuro VALUES LESS THAN (MAXVALUE)
    )
    UPDATE INDEXES (pn_dr_pk GLOBAL);

SELECT partition_name, partition_position
FROM user_tab_partitions WHERE table_name = 'DETALLE_RESERVA'
ORDER BY partition_position;

SELECT COUNT(*) AS estancias FROM detalle_reserva;

WHENEVER SQLERROR CONTINUE NONE
