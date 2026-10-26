-- Autorizado el 26/10/2026: solo un registro de catálogo, sin cambios de estructura.
INSERT INTO tipo_movimiento_caja (nombreMovimiento, signo)
SELECT 'DEVOLUCION RESERVA', -1 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM tipo_movimiento_caja WHERE nombreMovimiento='DEVOLUCION RESERVA');

COMMIT;
