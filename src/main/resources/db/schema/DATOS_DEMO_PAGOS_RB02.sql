-- Complemento de DATOS_DEMO_REBOLSYS.sql para el DDL de MODREBOLSYS.
-- Crea 30 reservas confirmadas y 30 ventas de tienda cobradas: 10 de cada una por medio.
-- El lote usa un turno histórico separado, actualiza el stock vendido y genera su arqueo.
-- Ejecutar como script con autocommit desactivado y confirmar al revisar el resultado.

DECLARE
    c_cantidad CONSTANT PLS_INTEGER := 30;
    v_instante CONSTANT TIMESTAMP := CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP);
    v_usuario NUMBER;
    v_turno NUMBER;
    v_tipo_cuenta NUMBER;
    v_tipo_salida NUMBER;
    v_reserva NUMBER;
    v_detalle_reserva NUMBER;
    v_venta NUMBER;
    v_detalle_venta NUMBER;
    v_stock NUMBER;
    v_perfil NUMBER;
    v_venta_cantidad NUMBER;
    v_clave VARCHAR2(64);
    v_reservas PLS_INTEGER;
    v_ventas PLS_INTEGER;
    v_pagos PLS_INTEGER;
BEGIN
    SAVEPOINT rb02_inicio;
    IF SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> 'MODREBOLSYS' THEN
        RAISE_APPLICATION_ERROR(-20010, 'Ejecuta RB02 en MODREBOLSYS.');
    END IF;

    SELECT COUNT(*) INTO v_reservas FROM reserva WHERE claveRegistro LIKE 'RB02-RES-%';
    SELECT COUNT(*) INTO v_ventas FROM venta WHERE claveRegistro LIKE 'RB02-VTA-%';
    SELECT COUNT(*) INTO v_pagos FROM pago WHERE claveIdempotencia LIKE 'PAGO-RB02-%';
    IF v_reservas = c_cantidad AND v_ventas = c_cantidad AND v_pagos = 2 * c_cantidad THEN
        RETURN;
    END IF;
    IF v_reservas + v_ventas + v_pagos <> 0 THEN
        RAISE_APPLICATION_ERROR(-20011, 'RB02 tiene una carga parcial. Revisa las filas antes de repetirla.');
    END IF;

    SELECT MIN(u.idUsuario) KEEP (DENSE_RANK FIRST ORDER BY
        CASE WHEN u.username IN ('operacion_local', 'rb01_operador_demo') THEN 0 ELSE 1 END)
    INTO v_usuario FROM usuario u WHERE u.estadoUsuario = 'ACTIVO';
    SELECT MIN(tc.idTipoComprobante) INTO v_tipo_cuenta
    FROM tipo_comprobante tc JOIN serie_comprobante s ON s.idTipoComprobante = tc.idTipoComprobante
    WHERE tc.codigo = 'CUENTA' AND s.serie = 'CTA';
    IF v_usuario IS NULL OR v_tipo_cuenta IS NULL THEN
        RAISE_APPLICATION_ERROR(-20012, 'Carga RB01 antes de ejecutar este complemento.');
    END IF;

    INSERT INTO forma_pago (nombrePago)
    SELECT m.nombre FROM (
        SELECT 'TARJETA' nombre FROM dual UNION ALL SELECT 'YAPE' FROM dual UNION ALL SELECT 'PLIN' FROM dual
    ) m WHERE NOT EXISTS (SELECT 1 FROM forma_pago f WHERE UPPER(TRIM(f.nombrePago)) = m.nombre);

    INSERT INTO tipo_movimiento_caja (nombreMovimiento, signo)
    SELECT 'COBRO_VENTA', 1 FROM dual
    WHERE NOT EXISTS (SELECT 1 FROM tipo_movimiento_caja WHERE nombreMovimiento = 'COBRO_VENTA');

    INSERT INTO tipo_movimiento_almacen (codigo, nombreMovimiento, signo)
    SELECT 'VENTA', 'Salida por venta', -1 FROM dual
    WHERE NOT EXISTS (SELECT 1 FROM tipo_movimiento_almacen WHERE codigo = 'VENTA');

    SELECT idTipoMovimientoAlmacen INTO v_tipo_salida
    FROM tipo_movimiento_almacen WHERE codigo = 'VENTA' AND signo = -1;

    INSERT INTO turno_caja (idUsuario, fechaApertura, fechaCierre, montoBase, estadoSesion)
    VALUES (v_usuario, v_instante - INTERVAL '1' SECOND, v_instante, 0, 0)
    RETURNING idTurno INTO v_turno;

    -- Estancias futuras de dos noches, separadas de los cupos ocupados o confirmados.
    FOR e IN (
        WITH dias AS (
            SELECT TRUNC(CAST(v_instante AS DATE)) + 2 + 4 * (LEVEL - 1) dia
            FROM dual CONNECT BY LEVEL <= 180
        ), cupos AS (
            SELECT h.idHabitacion, t.precioBase, f.dia,
                   ROW_NUMBER() OVER (ORDER BY f.dia, h.numeroCuarto) n
            FROM habitacion h JOIN tipo_habitacion t ON t.idTipoHabitacion = h.idTipo CROSS JOIN dias f
            WHERE h.estadoHabitacion = 'DISPONIBLE' AND t.precioBase > 0
              AND NOT EXISTS (
                  SELECT 1 FROM detalle_reserva d WHERE d.idHabitacion = h.idHabitacion
                    AND (d.estadoDetalle = 'EN_CURSO' OR (d.estadoDetalle = 'CONFIRMADA'
                         AND d.fechaIngreso < f.dia + 2 + 12/24 AND d.fechaSalida > f.dia + 14/24))
              )
        ), huespedes AS (
            SELECT h.idHuesped, h.idPerfil, ROW_NUMBER() OVER (ORDER BY h.idHuesped) n
            FROM huesped h JOIN perfil p ON p.idPerfil = h.idPerfil
            WHERE REGEXP_LIKE(p.correo, '^rb01-[0-9]{4}@example[.]test$')
              AND NOT EXISTS (
                  SELECT 1 FROM reserva_huesped rh JOIN detalle_reserva d ON d.idDetalleReserva = rh.idDetalleReserva
                  WHERE rh.idHuesped = h.idHuesped AND d.estadoDetalle IN ('CONFIRMADA', 'EN_CURSO')
              )
        )
        SELECT c.idHabitacion, c.precioBase, c.dia, c.n, h.idHuesped, h.idPerfil
        FROM cupos c JOIN huespedes h ON h.n = c.n WHERE c.n <= c_cantidad ORDER BY c.n
    ) LOOP
        v_clave := 'RB02-RES-' || LPAD(e.n, 3, '0');
        INSERT INTO reserva (idPerfil, idUsuario, fechaRegistro, claveRegistro, firmaSolicitud)
        VALUES (e.idPerfil, v_usuario, v_instante, v_clave, LOWER(RAWTOHEX(STANDARD_HASH(v_clave, 'SHA256'))))
        RETURNING idReserva INTO v_reserva;

        INSERT INTO detalle_reserva (idReserva, idHabitacion, fechaIngreso, fechaSalida, costoEstadia, estadoDetalle)
        VALUES (v_reserva, e.idHabitacion, e.dia + 14/24, e.dia + 2 + 12/24, 2 * e.precioBase, 'CONFIRMADA')
        RETURNING idDetalleReserva INTO v_detalle_reserva;

        INSERT INTO reserva_huesped (idDetalleReserva, idHuesped, ocupaPlazaCama)
        VALUES (v_detalle_reserva, e.idHuesped, 1);
        v_reservas := v_reservas + 1;
    END LOOP;
    IF v_reservas <> c_cantidad THEN
        RAISE_APPLICATION_ERROR(-20013, 'Faltan huéspedes demo o cupos de habitación para RB02.');
    END IF;

    -- Una línea por venta. Se venden entre una y tres unidades de cada producto seleccionado.
    FOR p IN (
        SELECT idProducto, nombreProducto, precioActual FROM producto
        WHERE estadoProducto = 'ACTIVO' AND stock >= 3 AND precioActual > 0
          AND REGEXP_LIKE(nombreProducto, ' \[RB01-[0-9]{3}\]$')
        ORDER BY idProducto FETCH FIRST c_cantidad ROWS ONLY
    ) LOOP
        v_ventas := v_ventas + 1;
        v_clave := 'RB02-VTA-' || LPAD(v_ventas, 3, '0');
        v_venta_cantidad := 1 + MOD(v_ventas - 1, 3);
        SELECT idPerfil INTO v_perfil FROM reserva WHERE claveRegistro = 'RB02-RES-' || LPAD(v_ventas, 3, '0');

        INSERT INTO venta (idPerfilComprador, idUsuario, modalidadCobro, estadoVenta, fechaRegistro,
                           fechaConfirmacion, claveRegistro, firmaSolicitud)
        VALUES (v_perfil, v_usuario, 'SEPARADA', 'CONFIRMADA', v_instante, v_instante, v_clave,
                LOWER(RAWTOHEX(STANDARD_HASH(v_clave, 'SHA256'))))
        RETURNING idVenta INTO v_venta;

        INSERT INTO detalle_venta (idVenta, idProducto, descripcion, cantidad, valorUnitario, igv, estadoLinea)
        VALUES (v_venta, p.idProducto, p.nombreProducto, v_venta_cantidad, p.precioActual, 0, 'ACTIVA')
        RETURNING idDetalleVenta INTO v_detalle_venta;

        UPDATE producto SET stock = stock - v_venta_cantidad
        WHERE idProducto = p.idProducto AND estadoProducto = 'ACTIVO'
          AND precioActual = p.precioActual AND stock >= v_venta_cantidad
        RETURNING stock INTO v_stock;
        IF SQL%ROWCOUNT <> 1 THEN
            RAISE_APPLICATION_ERROR(-20014, 'Cambió el stock o el precio de un producto. Reintenta RB02.');
        END IF;

        INSERT INTO movimiento_inventario (idProducto, idDetalleVenta, idTipoMovimientoAlmacen,
                                            cantidad, stockResultante, idUsuario, observacion,
                                            fechaMovimiento, claveOperacion)
        VALUES (p.idProducto, v_detalle_venta, v_tipo_salida, v_venta_cantidad, v_stock, v_usuario,
                'Salida demo por ' || v_clave, v_instante, 'STOCK-' || v_clave);
    END LOOP;
    IF v_ventas <> c_cantidad THEN
        RAISE_APPLICATION_ERROR(-20015, 'Faltan productos RB01 activos con stock suficiente.');
    END IF;

    INSERT INTO comprobante (idReserva, idVenta, idPerfilReceptor, idTipoComprobante, idTurno,
                             serie, subtotal, igv, totalPagar, estadoComprobante, claveIdempotencia)
    SELECT r.idReserva, NULL, r.idPerfil, v_tipo_cuenta, v_turno, 'CTA', d.costoEstadia, 0,
           d.costoEstadia, 'BORRADOR', 'CUENTA-R-' || r.idReserva
    FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
    WHERE r.claveRegistro LIKE 'RB02-RES-%'
    UNION ALL
    SELECT NULL, v.idVenta, v.idPerfilComprador, v_tipo_cuenta, v_turno, 'CTA', d.subtotal, d.igv,
           d.total, 'BORRADOR', 'CUENTA-V-' || v.idVenta
    FROM venta v JOIN detalle_venta d ON d.idVenta = v.idVenta
    WHERE v.claveRegistro LIKE 'RB02-VTA-%';

    INSERT INTO comprobante_detalle (idComprobante, idDetalleReserva, idDetalleVenta, numeroLinea,
                                     descripcion, cantidad, valorUnitario, igv)
    SELECT c.idComprobante, d.idDetalleReserva, NULL, 1, 'Alojamiento habitación ' || h.numeroCuarto,
           1, d.costoEstadia, 0
    FROM comprobante c JOIN detalle_reserva d ON d.idReserva = c.idReserva
    JOIN habitacion h ON h.idHabitacion = d.idHabitacion WHERE c.idTurno = v_turno
    UNION ALL
    SELECT c.idComprobante, NULL, d.idDetalleVenta, 1, d.descripcion, d.cantidad, d.valorUnitario, d.igv
    FROM comprobante c JOIN detalle_venta d ON d.idVenta = c.idVenta WHERE c.idTurno = v_turno;

    INSERT INTO pago (idComprobante, idFormaPago, idUsuario, idTurno, monto, numeroOperacion,
                     fechaPago, verificado, claveIdempotencia)
    WITH cuentas AS (
        SELECT c.idComprobante, c.totalPagar, COALESCE(r.claveRegistro, v.claveRegistro) clave
        FROM comprobante c LEFT JOIN reserva r ON r.idReserva = c.idReserva
        LEFT JOIN venta v ON v.idVenta = c.idVenta WHERE c.idTurno = v_turno
    ), medios AS (
        SELECT UPPER(TRIM(nombrePago)) nombre, MIN(idFormaPago) idFormaPago
        FROM forma_pago GROUP BY UPPER(TRIM(nombrePago))
    )
    SELECT c.idComprobante, f.idFormaPago, v_usuario, v_turno, c.totalPagar,
           'DEMO-' || c.clave, v_instante, 1, 'PAGO-' || c.clave
    FROM cuentas c JOIN medios f ON f.nombre = CASE MOD(TO_NUMBER(SUBSTR(c.clave, -3) DEFAULT NULL ON CONVERSION ERROR) - 1, 3)
        WHEN 0 THEN 'TARJETA' WHEN 1 THEN 'YAPE' ELSE 'PLIN' END;
    IF SQL%ROWCOUNT <> 2 * c_cantidad THEN
        RAISE_APPLICATION_ERROR(-20016, 'Faltan pagos RB02. Se revirtió la carga.');
    END IF;

    INSERT INTO flujo_caja (idTurno, idTipoMovimiento, idPago, concepto, monto, fechaHora)
    SELECT v_turno, t.idTipoMovimiento, p.idPago, 'Cobro demo ' || p.numeroOperacion, p.monto, p.fechaPago
    FROM pago p JOIN comprobante c ON c.idComprobante = p.idComprobante
    JOIN tipo_movimiento_caja t ON t.nombreMovimiento = CASE WHEN c.idVenta IS NULL THEN 'COBRO_RESERVA' ELSE 'COBRO_VENTA' END
      AND t.signo = 1 WHERE p.idTurno = v_turno;
    IF SQL%ROWCOUNT <> 2 * c_cantidad THEN
        RAISE_APPLICATION_ERROR(-20017, 'Faltan movimientos de caja RB02. Se revirtió la carga.');
    END IF;

    INSERT INTO arqueo_caja (idTurno, idFormaPago, montoCalculado, montoDeclarado)
    SELECT v_turno, f.idFormaPago, NVL(p.total, 0), NVL(p.total, 0)
    FROM forma_pago f LEFT JOIN (
        SELECT idFormaPago, SUM(monto) total FROM pago WHERE idTurno = v_turno AND verificado = 1 GROUP BY idFormaPago
    ) p ON p.idFormaPago = f.idFormaPago;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO rb02_inicio;
        RAISE;
END;
/
