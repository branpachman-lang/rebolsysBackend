-- Evolución de FLUJO_CAJA para evitar gastos duplicados en reintentos.
-- Conserva los movimientos existentes; NULL permite peticiones anteriores sin clave.
ALTER TABLE flujo_caja ADD (claveRegistro VARCHAR2(64));
ALTER TABLE flujo_caja ADD CONSTRAINT pn_flujo_clave_uq UNIQUE (claveRegistro);
