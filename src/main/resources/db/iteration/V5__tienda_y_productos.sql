-- Autorizado: cabecera de venta, relación con consumos, ruta de imagen y catálogos.
-- No crea productos ficticios ni modifica cobros o reservas existentes.
CREATE TABLE venta (
    idVenta NUMBER GENERATED ALWAYS AS IDENTITY,
    claveRegistro VARCHAR2(64) NOT NULL,
    firmaSolicitud VARCHAR2(64) NOT NULL,
    fechaVenta TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modoVenta VARCHAR2(20) NOT NULL,
    compradorNombre VARCHAR2(200),
    idCliente NUMBER,
    idReserva NUMBER,
    idUsuario NUMBER NOT NULL,
    idComprobante NUMBER,
    CONSTRAINT venta_pk PRIMARY KEY (idVenta),
    CONSTRAINT venta_clave_uq UNIQUE (claveRegistro),
    CONSTRAINT venta_cliente_fk FOREIGN KEY (idCliente) REFERENCES cliente(idCliente),
    CONSTRAINT venta_reserva_fk FOREIGN KEY (idReserva) REFERENCES reserva(idReserva),
    CONSTRAINT venta_usuario_fk FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario),
    CONSTRAINT venta_comprobante_fk FOREIGN KEY (idComprobante) REFERENCES comprobante(idComprobante),
    CONSTRAINT venta_modo_chk CHECK (
        (modoVenta='CARGO_RESERVA' AND idReserva IS NOT NULL AND idComprobante IS NULL)
        OR (modoVenta='SEPARADA' AND idReserva IS NULL)
    )
);
ALTER TABLE consumo ADD idVenta NUMBER;
ALTER TABLE consumo ADD CONSTRAINT consumo_venta_fk FOREIGN KEY (idVenta) REFERENCES venta(idVenta);
CREATE INDEX consumo_venta_ix ON consumo(idVenta);
ALTER TABLE producto ADD imagenRuta VARCHAR2(300);
INSERT INTO tipo_movimiento_caja(nombreMovimiento,signo)
SELECT 'INGRESO VENTA',1 FROM dual WHERE NOT EXISTS
    (SELECT 1 FROM tipo_movimiento_caja WHERE nombreMovimiento='INGRESO VENTA');
INSERT INTO tipo_movimiento_almacen(codigo,nombreMovimiento,signo)
SELECT 'SAL_VENTA','Salida por venta',-1 FROM dual WHERE NOT EXISTS
    (SELECT 1 FROM tipo_movimiento_almacen WHERE codigo='SAL_VENTA');
INSERT INTO tipo_movimiento_almacen(codigo,nombreMovimiento,signo)
SELECT 'AJ_ENT','Ajuste de entrada de productos',1 FROM dual WHERE NOT EXISTS
    (SELECT 1 FROM tipo_movimiento_almacen WHERE codigo='AJ_ENT');
INSERT INTO tipo_movimiento_almacen(codigo,nombreMovimiento,signo)
SELECT 'AJ_SAL','Ajuste de salida de productos',-1 FROM dual WHERE NOT EXISTS
    (SELECT 1 FROM tipo_movimiento_almacen WHERE codigo='AJ_SAL');
COMMIT;
