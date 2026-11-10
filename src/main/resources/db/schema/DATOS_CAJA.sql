MERGE INTO tipo_movimiento_caja t
USING (SELECT 'INGRESO_MANUAL' nombre, 1 signo FROM dual) s
ON (t.nombreMovimiento = s.nombre)
WHEN NOT MATCHED THEN INSERT (nombreMovimiento, signo) VALUES (s.nombre, s.signo);

COMMIT;
