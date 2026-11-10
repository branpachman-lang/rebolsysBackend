-- Ejecutar en SQL*Plus conectado como MODREBOLSYS. Usa COLUMN NEW_VALUE para fijar los filtros.
-- En SQL Developer, también puedes ejecutar los SELECT de parámetros por separado y sustituir
-- los cuatro valores en los bloques ANTES y DESPUES antes de medir en la misma hoja.
-- Cargar y confirmar DATOS_DEMO_REBOLSYS.sql antes de iniciar la comparación.
-- El script crea cuatro índices de prueba. CREATE INDEX confirma transacciones en Oracle.
-- Se actualizan las estadísticas de las tablas una vez, antes de medir los dos escenarios.
-- Para DISPLAY_CURSOR se requiere lectura de V$SQL, V$SQL_PLAN, V$SQL_PLAN_STATISTICS_ALL y V$SESSION.

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT OFF
SET LINESIZE 220
SET PAGESIZE 200
COLUMN plan_table_output FORMAT A200

BEGIN
    IF SYS_CONTEXT('USERENV', 'SESSION_USER') <> 'MODREBOLSYS'
       OR SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> 'MODREBOLSYS' THEN
        RAISE_APPLICATION_ERROR(-20001, 'Conéctate como MODREBOLSYS para ejecutar la comparación.');
    END IF;
END;
/

DEFINE rb01_cuarto = 0
DEFINE rb01_huesped = 0
DEFINE rb01_forma = 0
DEFINE rb01_dia = SIN_DATOS
COLUMN cuarto_rb01 NEW_VALUE rb01_cuarto NOPRINT
COLUMN huesped_rb01 NEW_VALUE rb01_huesped NOPRINT
COLUMN forma_rb01 NEW_VALUE rb01_forma NOPRINT
COLUMN dia_rb01 NEW_VALUE rb01_dia NOPRINT

-- Habitación con más informes demo y huésped con más estancias del lote.
SELECT idHabitacion cuarto_rb01
FROM informe_revision
WHERE observaciones LIKE 'RB01-INV-%'
GROUP BY idHabitacion
ORDER BY COUNT(*) DESC, idHabitacion
FETCH FIRST 1 ROW ONLY;

SELECT h.idHuesped huesped_rb01
FROM reserva_huesped h
JOIN detalle_reserva d ON d.idDetalleReserva = h.idDetalleReserva
JOIN reserva r ON r.idReserva = d.idReserva
WHERE r.claveRegistro LIKE 'RB01-RES-%'
GROUP BY h.idHuesped
ORDER BY COUNT(*) DESC, h.idHuesped
FETCH FIRST 1 ROW ONLY;

-- Selecciona un medio que tenga pagos RB01. La carga inicial los genera en efectivo.
SELECT idFormaPago forma_rb01
FROM pago
WHERE claveIdempotencia LIKE 'PAGO-RB01-RES-%'
GROUP BY idFormaPago
ORDER BY COUNT(*), idFormaPago
FETCH FIRST 1 ROW ONLY;

-- Usa un día con resoluciones de tickets RB01 y poca actividad.
SELECT TO_CHAR(dia, 'YYYY-MM-DD') dia_rb01
FROM (
    SELECT TRUNC(fechaMovimiento) dia
    FROM movimiento_inventario
    WHERE claveOperacion LIKE 'ticket:%' AND observacion LIKE '%(RB01)'
    GROUP BY TRUNC(fechaMovimiento)
    ORDER BY COUNT(*), dia DESC
)
WHERE ROWNUM = 1;

DECLARE
    v_indices NUMBER;
BEGIN
    IF &rb01_cuarto = 0 OR &rb01_huesped = 0 OR &rb01_forma = 0 OR '&rb01_dia' = 'SIN_DATOS' THEN
        RAISE_APPLICATION_ERROR(-20002, 'Faltan datos RB01. Ejecuta la carga demo y confirma la transacción.');
    END IF;

    SELECT COUNT(*) INTO v_indices
    FROM user_indexes
    WHERE index_name IN ('IX_RB01_INF_HAB_FECHA', 'IX_RB01_RH_HUESPED_DET',
                         'IX_RB01_PAGO_FORMA', 'IX_RB01_MOV_FECHA');
    IF v_indices > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Ya existen índices de esta prueba. Usa una copia con la línea base original.');
    END IF;
END;
/

DECLARE
    v_permiso NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_permiso FROM v$sql WHERE 1 = 0;
    SELECT COUNT(*) INTO v_permiso FROM v$sql_plan WHERE 1 = 0;
    SELECT COUNT(*) INTO v_permiso FROM v$sql_plan_statistics_all WHERE 1 = 0;
    SELECT COUNT(*) INTO v_permiso FROM v$session WHERE 1 = 0;

    DBMS_STATS.GATHER_TABLE_STATS(USER, 'INFORME_REVISION', cascade => FALSE);
    DBMS_STATS.GATHER_TABLE_STATS(USER, 'RESERVA_HUESPED', cascade => FALSE);
    DBMS_STATS.GATHER_TABLE_STATS(USER, 'PAGO', cascade => FALSE);
    DBMS_STATS.GATHER_TABLE_STATS(USER, 'MOVIMIENTO_INVENTARIO', cascade => FALSE);
END;
/

PROMPT Valores fijos para ambas mediciones
PROMPT Habitacion=&rb01_cuarto Huesped=&rb01_huesped Medio=&rb01_forma Dia=&rb01_dia

PROMPT ===== ANTES 1. Informes por habitacion =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idInformeRevision, idHabitacion, fechaHora, estadoInforme
FROM informe_revision
WHERE idHabitacion = &rb01_cuarto
ORDER BY fechaHora DESC, idInformeRevision DESC
FETCH FIRST 20 ROWS ONLY;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== ANTES 2. Reservas por huesped =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idHuesped, idDetalleReserva
FROM reserva_huesped
WHERE idHuesped = &rb01_huesped
ORDER BY idDetalleReserva;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== ANTES 3. Pagos por medio =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idPago, idFormaPago, monto, fechaPago
FROM pago
WHERE idFormaPago = &rb01_forma;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== ANTES 4. Movimientos de inventario por fecha =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idMovimiento, fechaMovimiento, idProducto, idItem, cantidad
FROM movimiento_inventario
WHERE fechaMovimiento >= TIMESTAMP '&rb01_dia 00:00:00'
  AND fechaMovimiento <  TIMESTAMP '&rb01_dia 00:00:00' + INTERVAL '1' DAY;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE +PARTITION'));

PROMPT ===== CREACION DE INDICES =====
CREATE INDEX ix_rb01_inf_hab_fecha ON informe_revision (idHabitacion, fechaHora, idInformeRevision);
CREATE INDEX ix_rb01_rh_huesped_det ON reserva_huesped (idHuesped, idDetalleReserva);
CREATE INDEX ix_rb01_pago_forma ON pago (idFormaPago);
CREATE INDEX ix_rb01_mov_fecha ON movimiento_inventario (fechaMovimiento);

PROMPT ===== DESPUES 1. Informes por habitacion =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idInformeRevision, idHabitacion, fechaHora, estadoInforme
FROM informe_revision
WHERE idHabitacion = &rb01_cuarto
ORDER BY fechaHora DESC, idInformeRevision DESC
FETCH FIRST 20 ROWS ONLY;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== DESPUES 2. Reservas por huesped =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idHuesped, idDetalleReserva
FROM reserva_huesped
WHERE idHuesped = &rb01_huesped
ORDER BY idDetalleReserva;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== DESPUES 3. Pagos por medio =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idPago, idFormaPago, monto, fechaPago
FROM pago
WHERE idFormaPago = &rb01_forma;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE'));

PROMPT ===== DESPUES 4. Movimientos de inventario por fecha =====
SELECT /*+ GATHER_PLAN_STATISTICS */ idMovimiento, fechaMovimiento, idProducto, idItem, cantidad
FROM movimiento_inventario
WHERE fechaMovimiento >= TIMESTAMP '&rb01_dia 00:00:00'
  AND fechaMovimiento <  TIMESTAMP '&rb01_dia 00:00:00' + INTERVAL '1' DAY;
SELECT plan_table_output
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PREDICATE +PARTITION'));

UNDEFINE rb01_cuarto rb01_huesped rb01_forma rb01_dia
COLUMN cuarto_rb01 CLEAR
COLUMN huesped_rb01 CLEAR
COLUMN forma_rb01 CLEAR
COLUMN dia_rb01 CLEAR
COLUMN plan_table_output CLEAR
WHENEVER SQLERROR CONTINUE NONE
