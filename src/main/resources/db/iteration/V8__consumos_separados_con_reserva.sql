-- Evolución del DDL propuesto con cliente unificado.
-- Oracle; conectado al esquema PropuestaHospedajeRB.
-- Conserva todos los datos y permite identificar la reserva de un cobro separado.
ALTER TABLE venta DROP CONSTRAINT pn_venta_modalidad_ck;
ALTER TABLE venta ADD CONSTRAINT pn_venta_modalidad_ck CHECK (
    (modalidadCobro='RESERVA' AND idDetalleReserva IS NOT NULL)
    OR modalidadCobro='SEPARADA'
);
