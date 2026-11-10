-- Datos ficticios para el esquema Oracle de «DDL Actual.txt». Ejecutar con autocommit desactivado.
-- Si el esquema está vacío, el lote RB01 prepara 20 habitaciones y un operador demo sin credencial.
-- Con datos existentes se usan las habitaciones actuales y un usuario activo, de preferencia operacion_local.
-- La fecha de referencia es el día de ejecución en Lima.
-- Se necesitan 10 habitaciones aptas para estancias en curso y otras 10 libres para los informes pendientes.
-- Los estados físicos de las habitaciones existentes y las fotos se gestionan desde la aplicación.
-- En la carga inicial se reparten los cobros de reserva y se incluyen 30 ventas de tienda.
-- Cada INSERT conserva los registros ya cargados, incluidos sus pagos y stock.
-- Revisar el resultado de todos los INSERT y confirmar la transacción desde el cliente SQL.
-- Ante cualquier error, revertir la transacción completa desde el cliente SQL.

-- 0. Catálogos indispensables para la carga, cuando todavía no existen.
INSERT INTO tipo_documento (nombreDocumento, codigoSunat)
WITH documentos (nombre, codigo) AS (
    SELECT 'DNI', '1' FROM dual UNION ALL SELECT 'PASAPORTE', '7' FROM dual
    UNION ALL SELECT 'CARNET EXTRANJERIA', '4' FROM dual UNION ALL SELECT 'RUC', '6' FROM dual
)
SELECT s.nombre, s.codigo FROM documentos s
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento t WHERE t.codigoSunat = s.codigo
                  OR UPPER(TRIM(t.nombreDocumento)) = s.nombre);

INSERT INTO forma_pago (nombrePago)
WITH medios (nombre) AS (
    SELECT 'EFECTIVO' FROM dual UNION ALL SELECT 'TARJETA' FROM dual
    UNION ALL SELECT 'YAPE' FROM dual UNION ALL SELECT 'PLIN' FROM dual
)
SELECT m.nombre FROM medios m
WHERE NOT EXISTS (SELECT 1 FROM forma_pago f WHERE UPPER(TRIM(f.nombrePago)) = m.nombre);

INSERT INTO tipo_comprobante (codigo, nombreComprobante)
SELECT 'CUENTA', 'Cuenta interna de desarrollo' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM tipo_comprobante t WHERE t.codigo = 'CUENTA');

INSERT INTO serie_comprobante (idTipoComprobante, serie, siguienteNumero)
SELECT t.idTipoComprobante, 'CTA', 1 FROM tipo_comprobante t
WHERE t.codigo = 'CUENTA'
  AND NOT EXISTS (SELECT 1 FROM serie_comprobante s WHERE s.idTipoComprobante = t.idTipoComprobante AND s.serie = 'CTA');

INSERT INTO tipo_movimiento_caja (nombreMovimiento, signo)
WITH tipos (nombre) AS (
    SELECT 'COBRO_RESERVA' FROM dual UNION ALL SELECT 'COBRO_VENTA' FROM dual
)
SELECT s.nombre, 1 FROM tipos s
WHERE NOT EXISTS (SELECT 1 FROM tipo_movimiento_caja t WHERE t.nombreMovimiento = s.nombre);

INSERT INTO tipo_movimiento_almacen (codigo, nombreMovimiento, signo)
WITH tipos (codigo, nombre, signo) AS (
    SELECT 'AJ_ENT', 'Ajuste de entrada', 1 FROM dual
    UNION ALL SELECT 'VENTA', 'Salida por venta', -1 FROM dual
    UNION ALL SELECT 'ASIG_ITEM', 'Asignación de mobiliario', 0 FROM dual
    UNION ALL SELECT 'EST_ITEM', 'Estado de conservación de mobiliario', 0 FROM dual
)
SELECT s.codigo, s.nombre, s.signo FROM tipos s
WHERE NOT EXISTS (SELECT 1 FROM tipo_movimiento_almacen t WHERE t.codigo = s.codigo);

-- 0.1. Operador de referencia para las claves foráneas si no hay ningún usuario activo.
INSERT INTO tipo_documento (nombreDocumento)
SELECT 'INTERNO' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento t WHERE t.nombreDocumento = 'INTERNO')
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.estadoUsuario = 'ACTIVO');

INSERT INTO perfil (idTipoDocumento, numeroDocumento, correo, esCliente)
SELECT t.idTipoDocumento, 'RB01-OPERADOR', 'rb01-operador@example.test', 0
FROM tipo_documento t WHERE t.nombreDocumento = 'INTERNO'
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.estadoUsuario = 'ACTIVO')
  AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.idTipoDocumento = t.idTipoDocumento AND p.numeroDocumento = 'RB01-OPERADOR');

INSERT INTO persona_natural (idPerfil, nombres, apellidos, fechaNacimiento)
SELECT p.idPerfil, 'Operador', 'Demostración RB01', DATE '1985-01-01'
FROM perfil p WHERE p.correo = 'rb01-operador@example.test' AND p.esCliente = 0
  AND NOT EXISTS (SELECT 1 FROM persona_natural pn WHERE pn.idPerfil = p.idPerfil);

INSERT INTO empleado (idPerfil, fechaContratacion)
SELECT p.idPerfil, DATE '2020-01-01'
FROM perfil p JOIN persona_natural pn ON pn.idPerfil = p.idPerfil
WHERE p.correo = 'rb01-operador@example.test' AND p.esCliente = 0
  AND NOT EXISTS (SELECT 1 FROM empleado e WHERE e.idPerfil = p.idPerfil);

INSERT INTO usuario (idEmpleado, username, estadoUsuario)
SELECT e.idEmpleado, 'rb01_operador_demo', 'ACTIVO'
FROM empleado e JOIN perfil p ON p.idPerfil = e.idPerfil
WHERE p.correo = 'rb01-operador@example.test' AND p.esCliente = 0
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.estadoUsuario = 'ACTIVO' OR u.idEmpleado = e.idEmpleado
                  OR u.username = 'rb01_operador_demo');

-- 0.2. Inventario base de habitaciones de Real Bolognesi, solo si HABITACION está vacía.
-- El DDL guarda el precio en el tipo. Se distinguen los tipos que tienen tarifas diferentes.
INSERT INTO tipo_habitacion (nombreTipo, capacidadMax, precioBase)
WITH tipos (nombre, capacidad, tarifa) AS (
    SELECT 'Matrimonial con baño RB01', 3, 100 FROM dual
    UNION ALL SELECT 'Matrimonial doble baño RB01', 5, 120 FROM dual
    UNION ALL SELECT 'Matrimonial triple baño RB01', 7, 150 FROM dual
    UNION ALL SELECT 'Doble sin baño RB01', 3, 80 FROM dual
    UNION ALL SELECT 'Matrimonial sin baño 100 RB01', 3, 100 FROM dual
    UNION ALL SELECT 'Matrimonial sin baño 80 RB01', 3, 80 FROM dual
    UNION ALL SELECT 'Simple con baño 50 RB01', 2, 50 FROM dual
    UNION ALL SELECT 'Simple con baño 40 RB01', 2, 40 FROM dual
    UNION ALL SELECT 'Simple sin baño RB01', 2, 30 FROM dual
)
SELECT s.nombre, s.capacidad, s.tarifa FROM tipos s
WHERE NOT EXISTS (SELECT 1 FROM habitacion)
  AND NOT EXISTS (SELECT 1 FROM tipo_habitacion t WHERE t.nombreTipo = s.nombre);

INSERT INTO habitacion (numeroCuarto, idTipo, estadoHabitacion, capacidadPersonas, plazasCama, maxMenoresSinCama)
WITH grupos (nombre, numeros, plazas) AS (
    SELECT 'Matrimonial con baño RB01', '201,301,401', 2 FROM dual
    UNION ALL SELECT 'Matrimonial doble baño RB01', '202,203', 4 FROM dual
    UNION ALL SELECT 'Matrimonial triple baño RB01', '303', 6 FROM dual
    UNION ALL SELECT 'Doble sin baño RB01', '302,304', 2 FROM dual
    UNION ALL SELECT 'Matrimonial sin baño 100 RB01', '403', 2 FROM dual
    UNION ALL SELECT 'Matrimonial sin baño 80 RB01', '502,503,504', 2 FROM dual
    UNION ALL SELECT 'Simple con baño 50 RB01', '402,405', 1 FROM dual
    UNION ALL SELECT 'Simple con baño 40 RB01', '506', 1 FROM dual
    UNION ALL SELECT 'Simple sin baño RB01', '404,406,501,505,507', 1 FROM dual
), posiciones AS (
    SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 5
)
SELECT REGEXP_SUBSTR(g.numeros, '[^,]+', 1, p.n), t.idTipoHabitacion, 'DISPONIBLE', g.plazas + 1, g.plazas, 1
FROM grupos g JOIN tipo_habitacion t ON t.nombreTipo = g.nombre CROSS JOIN posiciones p
WHERE p.n <= REGEXP_COUNT(g.numeros, ',') + 1
  AND NOT EXISTS (SELECT 1 FROM habitacion);

-- 1. Clientes. DNI (700), pasaporte (100), carné de extranjería (100), RUC 10 (50) y RUC 20 (50).
INSERT INTO perfil (idTipoDocumento, numeroDocumento, correo, esCliente)
WITH numeros AS (
    SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 1000
), bases AS (
    SELECT n,
           CASE WHEN n <= 700 THEN '1' WHEN n <= 800 THEN '7' WHEN n <= 900 THEN '4' ELSE '6' END codigo,
           CASE WHEN n <= 700 THEN '98' || LPAD(n, 6, '0')
                WHEN n <= 800 THEN 'ZQ' || LPAD(n, 7, '0')
                WHEN n <= 900 THEN TO_CHAR(970000000 + n)
                WHEN n <= 950 THEN '1097' || LPAD(n, 6, '0')
                ELSE '2097' || LPAD(n, 6, '0') END documento,
           'rb01-' || LPAD(n, 4, '0') || '@example.test' correo
    FROM numeros
), pesos AS (
    SELECT LEVEL pos, TO_NUMBER(SUBSTR('5432765432', LEVEL, 1)) peso
    FROM dual CONNECT BY LEVEL <= 10
), documentos AS (
    SELECT b.codigo, b.correo,
           b.documento || CASE WHEN b.codigo = '6' THEN TO_CHAR(MOD(11 - MOD(
               (SELECT SUM(TO_NUMBER(SUBSTR(b.documento, p.pos, 1)) * p.peso) FROM pesos p), 11), 10)) END numero
    FROM bases b
)
SELECT t.idTipoDocumento, d.numero, d.correo, 1
FROM documentos d
JOIN tipo_documento t ON t.idTipoDocumento = (
    SELECT MIN(a.idTipoDocumento) FROM tipo_documento a
    WHERE a.codigoSunat = d.codigo OR UPPER(TRIM(a.nombreDocumento)) =
          CASE d.codigo WHEN '1' THEN 'DNI' WHEN '7' THEN 'PASAPORTE'
                        WHEN '4' THEN 'CARNET EXTRANJERIA' WHEN '6' THEN 'RUC' END
)
WHERE NOT EXISTS (SELECT 1 FROM perfil p WHERE p.correo = d.correo);

INSERT INTO persona_natural (idPerfil, nombres, apellidos, fechaNacimiento)
WITH personas AS (
    SELECT idPerfil, TO_NUMBER(SUBSTR(correo, 6, 4) DEFAULT NULL ON CONVERSION ERROR) n
    FROM perfil WHERE REGEXP_LIKE(correo, '^rb01-[0-9]{4}@example[.]test$')
)
SELECT p.idPerfil,
       REGEXP_SUBSTR('Lucía,Renato,Mónica,Diego,Elena,Andrés,Valeria,Jorge,Rosa,Adrián,Mariela,César',
           '[^,]+', 1, MOD(p.n - 1, 12) + 1),
       REGEXP_SUBSTR('Quispe,Vargas,Mendoza,Salas,Rojas,Flores,Huamán,Navarro,Torres,Lozano',
           '[^,]+', 1, MOD(TRUNC((p.n - 1) / 12), 10) + 1) || ' ' ||
       REGEXP_SUBSTR('Soto,Mejía,Silva,Paredes,Aguilar,Cruz,Campos,Vega,Reyes,León',
           '[^,]+', 1, MOD(TRUNC((p.n - 1) / 120) + MOD(p.n - 1, 12), 10) + 1),
       DATE '1965-01-01' + MOD(p.n * 137, 14200)
FROM personas p
WHERE p.n BETWEEN 1 AND 900
  AND NOT EXISTS (SELECT 1 FROM persona_natural pn WHERE pn.idPerfil = p.idPerfil);

INSERT INTO persona_juridica (idPerfil, razonSocial, direccionFiscal)
WITH empresas AS (
    SELECT idPerfil, TO_NUMBER(SUBSTR(correo, 6, 4) DEFAULT NULL ON CONVERSION ERROR) n
    FROM perfil WHERE REGEXP_LIKE(correo, '^rb01-[0-9]{4}@example[.]test$')
)
SELECT e.idPerfil,
       CASE WHEN e.n <= 950 THEN 'Comercial Demo ' ELSE 'Servicios Demo ' END ||
       REGEXP_SUBSTR('Altiplano,Lago,Valles,Mirador,Andes,Estación,Colinas,Sendero',
           '[^,]+', 1, MOD(e.n, 8) + 1) || ' ' || e.n || CASE WHEN e.n > 950 THEN ' S.A.C.' END,
       'Calle de prueba ' || (e.n - 900) || ', Puno'
FROM empresas e
WHERE e.n BETWEEN 901 AND 1000
  AND NOT EXISTS (SELECT 1 FROM persona_juridica pj WHERE pj.idPerfil = e.idPerfil);

INSERT INTO huesped (idPerfil)
SELECT p.idPerfil FROM perfil p JOIN persona_natural pn ON pn.idPerfil = p.idPerfil
WHERE p.esCliente = 1 AND REGEXP_LIKE(p.correo, '^rb01-[0-9]{4}@example[.]test$')
  AND NOT EXISTS (SELECT 1 FROM huesped h WHERE h.idPerfil = p.idPerfil);

-- 2. Una reserva y una estancia por cliente. El sistema deriva el estado desde DETALLE_RESERVA.
INSERT INTO reserva (idPerfil, idUsuario, fechaRegistro, claveRegistro, firmaSolicitud)
WITH titulares AS (
    SELECT p.idPerfil, 1 + MOD((TO_NUMBER(SUBSTR(p.correo, 6, 4) DEFAULT NULL ON CONVERSION ERROR) - 1) * 37, 1000) n
    FROM perfil p
    WHERE p.esCliente = 1 AND REGEXP_LIKE(p.correo, '^rb01-[0-9]{4}@example[.]test$')
), reloj AS (
    SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE)) hoy FROM dual
)
SELECT c.idPerfil, u.idUsuario,
       CAST(f.hoy - CASE WHEN c.n <= 800 THEN 210 + TRUNC(c.n / 21)
                         WHEN c.n <= 990 THEN MOD(c.n, 14) + 1 ELSE 2 END AS TIMESTAMP),
       'RB01-RES-' || LPAD(c.n, 4, '0'),
       LOWER(RAWTOHEX(STANDARD_HASH('RB01-RES-' || LPAD(c.n, 4, '0'), 'SHA256')))
FROM titulares c CROSS JOIN reloj f
JOIN usuario u ON u.idUsuario = (
    SELECT MIN(a.idUsuario) KEEP (DENSE_RANK FIRST ORDER BY CASE WHEN a.username = 'operacion_local' THEN 0 ELSE 1 END)
    FROM usuario a WHERE a.estadoUsuario = 'ACTIVO'
)
WHERE NOT EXISTS (SELECT 1 FROM reserva r WHERE r.claveRegistro = 'RB01-RES-' || LPAD(c.n, 4, '0'));

-- 800 finalizadas, con ingreso y salida reales. Hasta tres noches por estancia.
INSERT INTO detalle_reserva (idReserva, idHabitacion, fechaIngreso, fechaSalida,
                             fechaIngresoReal, fechaSalidaReal, costoEstadia, estadoDetalle)
WITH titulares AS (
    SELECT idReserva, TO_NUMBER(SUBSTR(claveRegistro, 10) DEFAULT NULL ON CONVERSION ERROR) n
    FROM reserva WHERE REGEXP_LIKE(claveRegistro, '^RB01-RES-[0-9]{4}$')
), dias AS (
    SELECT LEVEL bloque, TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE)) - 4 * LEVEL dia
    FROM dual CONNECT BY LEVEL <= 180
), horarios AS (
    SELECT h.idHabitacion, h.numeroCuarto, f.dia, t.precioBase tarifa,
           1 + MOD(f.bloque + h.idHabitacion, 3) noches
    FROM habitacion h JOIN tipo_habitacion t ON t.idTipoHabitacion = h.idTipo CROSS JOIN dias f
    WHERE t.precioBase > 0
), cupos AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.dia DESC, s.numeroCuarto) posicion
    FROM horarios s
    WHERE NOT EXISTS (
        SELECT 1 FROM detalle_reserva d JOIN reserva r ON r.idReserva = d.idReserva
        WHERE d.idHabitacion = s.idHabitacion AND d.estadoDetalle <> 'CANCELADA'
          AND r.claveRegistro NOT LIKE 'RB01-RES-%'
          AND COALESCE(d.fechaIngresoReal, d.fechaIngreso) < s.dia + s.noches + 12/24
          AND COALESCE(d.fechaSalidaReal, d.fechaSalida) > s.dia + 14/24
    )
)
SELECT r.idReserva, s.idHabitacion, s.dia + 14/24, s.dia + s.noches + 12/24,
       s.dia + 15/24, s.dia + s.noches + 11/24, s.noches * s.tarifa, 'FINALIZADA'
FROM cupos s JOIN titulares r ON r.n = s.posicion
WHERE s.posicion <= 800
  AND NOT EXISTS (SELECT 1 FROM detalle_reserva d WHERE d.idReserva = r.idReserva);

-- 10 en curso, en cuartos distintos, con llegada ayer y salida prevista mañana.
INSERT INTO detalle_reserva (idReserva, idHabitacion, fechaIngreso, fechaSalida,
                             fechaIngresoReal, costoEstadia, estadoDetalle)
WITH reloj AS (
    SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE)) hoy FROM dual
), cuartos AS (
    SELECT h.idHabitacion, t.precioBase tarifa, f.hoy,
           ROW_NUMBER() OVER (ORDER BY h.numeroCuarto) posicion
    FROM habitacion h JOIN tipo_habitacion t ON t.idTipoHabitacion = h.idTipo CROSS JOIN reloj f
    WHERE h.estadoHabitacion = 'DISPONIBLE' AND t.precioBase > 0
      AND NOT EXISTS (SELECT 1 FROM informe_revision i WHERE i.idHabitacion = h.idHabitacion
                      AND (i.estadoInforme IS NULL OR i.estadoInforme = 'POR_APROBAR'))
      AND NOT EXISTS (
          SELECT 1 FROM detalle_reserva d JOIN reserva r ON r.idReserva = d.idReserva
          WHERE d.idHabitacion = h.idHabitacion AND r.claveRegistro NOT LIKE 'RB01-RES-%'
            AND (d.estadoDetalle = 'EN_CURSO' OR (d.estadoDetalle = 'CONFIRMADA'
                 AND d.fechaIngreso < f.hoy + 1 + 12/24 AND d.fechaSalida > f.hoy - 1 + 14/24))
      )
)
SELECT r.idReserva, h.idHabitacion, h.hoy - 1 + 14/24, h.hoy + 1 + 12/24,
       h.hoy - 1 + 15/24, 2 * h.tarifa, 'EN_CURSO'
FROM cuartos h JOIN reserva r ON r.claveRegistro = 'RB01-RES-' || LPAD(990 + h.posicion, 4, '0')
WHERE h.posicion <= 10
  AND NOT EXISTS (SELECT 1 FROM detalle_reserva d WHERE d.idReserva = r.idReserva);

-- 190 confirmadas para fechas futuras, sin llegada registrada. El esquema no tiene estado POR_CONFIRMAR.
INSERT INTO detalle_reserva (idReserva, idHabitacion, fechaIngreso, fechaSalida, costoEstadia, estadoDetalle)
WITH dias AS (
    SELECT TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE)) + 2 + 4 * (LEVEL - 1) dia
    FROM dual CONNECT BY LEVEL <= 180
), cupos AS (
    SELECT h.idHabitacion, f.dia, t.precioBase tarifa,
           ROW_NUMBER() OVER (ORDER BY f.dia, h.numeroCuarto) posicion
    FROM habitacion h JOIN tipo_habitacion t ON t.idTipoHabitacion = h.idTipo CROSS JOIN dias f
    WHERE h.estadoHabitacion = 'DISPONIBLE' AND t.precioBase > 0
      AND NOT EXISTS (
          SELECT 1 FROM detalle_reserva d JOIN reserva r ON r.idReserva = d.idReserva
          WHERE d.idHabitacion = h.idHabitacion AND (d.estadoDetalle = 'EN_CURSO'
            OR (d.estadoDetalle = 'CONFIRMADA' AND r.claveRegistro NOT LIKE 'RB01-RES-%'
                AND d.fechaIngreso < f.dia + 2 + 12/24 AND d.fechaSalida > f.dia + 14/24))
      )
)
SELECT r.idReserva, c.idHabitacion, c.dia + 14/24, c.dia + 2 + 12/24, c.tarifa * 2, 'CONFIRMADA'
FROM cupos c JOIN reserva r ON r.claveRegistro = 'RB01-RES-' || LPAD(800 + c.posicion, 4, '0')
WHERE c.posicion <= 190
  AND NOT EXISTS (SELECT 1 FROM detalle_reserva d WHERE d.idReserva = r.idReserva);

-- Las empresas son titulares. Cada estancia tiene un adulto natural como ocupante.
INSERT INTO reserva_huesped (idDetalleReserva, idHuesped, ocupaPlazaCama)
WITH titulares AS (
    SELECT d.idDetalleReserva, TO_NUMBER(SUBSTR(p.correo, 6, 4) DEFAULT NULL ON CONVERSION ERROR) n
    FROM detalle_reserva d JOIN reserva r ON r.idReserva = d.idReserva
    JOIN perfil p ON p.idPerfil = r.idPerfil
    WHERE REGEXP_LIKE(r.claveRegistro, '^RB01-RES-[0-9]{4}$')
)
SELECT d.idDetalleReserva, h.idHuesped, 1
FROM titulares d JOIN perfil p ON p.correo = 'rb01-' || LPAD(CASE WHEN d.n > 900 THEN d.n - 900 ELSE d.n END, 4, '0') || '@example.test'
JOIN huesped h ON h.idPerfil = p.idPerfil
WHERE NOT EXISTS (SELECT 1 FROM reserva_huesped o WHERE o.idDetalleReserva = d.idDetalleReserva);

-- 3. Alojamiento y caja. Los cobros ficticios se registran en turnos históricos del operador.
INSERT INTO turno_caja (idUsuario, fechaApertura, fechaCierre, montoBase, estadoSesion)
WITH dias AS (
    SELECT DISTINCT r.idUsuario, TRUNC(d.fechaIngresoReal) dia
    FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
    WHERE r.claveRegistro LIKE 'RB01-RES-%' AND d.fechaIngresoReal IS NOT NULL
)
SELECT s.idUsuario, CAST(s.dia AS TIMESTAMP) + INTERVAL '1' SECOND,
       CAST(s.dia AS TIMESTAMP) + INTERVAL '23:59:59' HOUR TO SECOND, 0, 0
FROM dias s
WHERE NOT EXISTS (SELECT 1 FROM turno_caja t WHERE t.idUsuario = s.idUsuario
                  AND t.fechaApertura = CAST(s.dia AS TIMESTAMP) + INTERVAL '1' SECOND);

INSERT INTO comprobante (idReserva, idPerfilReceptor, idTipoComprobante, idTurno, serie,
                         subtotal, igv, totalPagar, estadoComprobante, claveIdempotencia)
SELECT r.idReserva, r.idPerfil, tc.idTipoComprobante, t.idTurno, s.serie,
       d.costoEstadia, 0, d.costoEstadia, 'BORRADOR', 'CUENTA-R-' || r.idReserva
FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
JOIN turno_caja t ON t.idUsuario = r.idUsuario AND t.estadoSesion = 0
  AND t.fechaApertura = CAST(TRUNC(d.fechaIngresoReal) AS TIMESTAMP) + INTERVAL '1' SECOND
JOIN tipo_comprobante tc ON tc.codigo = 'CUENTA'
JOIN serie_comprobante s ON s.idTipoComprobante = tc.idTipoComprobante AND s.serie = 'CTA'
WHERE r.claveRegistro LIKE 'RB01-RES-%' AND d.fechaIngresoReal IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM comprobante c WHERE c.claveIdempotencia = 'CUENTA-R-' || r.idReserva);

INSERT INTO comprobante_detalle (idComprobante, idDetalleReserva, numeroLinea, descripcion,
                                 cantidad, valorUnitario, igv)
SELECT c.idComprobante, d.idDetalleReserva, 1, 'Alojamiento habitación ' || h.numeroCuarto,
       1, d.costoEstadia, 0
FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
JOIN habitacion h ON h.idHabitacion = d.idHabitacion
JOIN comprobante c ON c.claveIdempotencia = 'CUENTA-R-' || r.idReserva
WHERE r.claveRegistro LIKE 'RB01-RES-%'
  AND NOT EXISTS (SELECT 1 FROM comprobante_detalle l WHERE l.idComprobante = c.idComprobante AND l.numeroLinea = 1);

-- 4. Tienda. 50 referencias por 10 marcas ficticias, con existencias y su entrada de almacén.
INSERT INTO categoria (nombreCategoria)
WITH categorias (nombre) AS (
    SELECT 'Snacks' FROM dual UNION ALL SELECT 'Bebidas' FROM dual
    UNION ALL SELECT 'Botiquín' FROM dual UNION ALL SELECT 'Higiene personal' FROM dual
    UNION ALL SELECT 'Artículos de hospedaje' FROM dual
)
SELECT c.nombre FROM categorias c
WHERE NOT EXISTS (SELECT 1 FROM categoria e WHERE UPPER(TRIM(e.nombreCategoria)) = UPPER(c.nombre));

INSERT INTO producto (nombreProducto, precioActual, stock, stockMinimo, idCategoria, estadoProducto)
WITH referencias (n, categoria, nombre, precio) AS (
    SELECT 1, 'Snacks', 'Papas clásicas 45 g', 3 FROM dual
    UNION ALL SELECT 2, 'Snacks', 'Chifles salados 45 g', 3 FROM dual
    UNION ALL SELECT 3, 'Snacks', 'Cancha tostada 50 g', 3 FROM dual
    UNION ALL SELECT 4, 'Snacks', 'Galletas de avena 60 g', 3 FROM dual
    UNION ALL SELECT 5, 'Snacks', 'Galletas de chocolate 55 g', 3.5 FROM dual
    UNION ALL SELECT 6, 'Snacks', 'Barra de cereal 30 g', 3 FROM dual
    UNION ALL SELECT 7, 'Snacks', 'Chocolate con leche 30 g', 4 FROM dual
    UNION ALL SELECT 8, 'Snacks', 'Maní tostado 45 g', 3 FROM dual
    UNION ALL SELECT 9, 'Snacks', 'Alfajor 45 g', 4 FROM dual
    UNION ALL SELECT 10, 'Snacks', 'Gomitas de fruta 50 g', 3.5 FROM dual
    UNION ALL SELECT 11, 'Bebidas', 'Agua sin gas 500 ml', 2 FROM dual
    UNION ALL SELECT 12, 'Bebidas', 'Agua sin gas 1 L', 3 FROM dual
    UNION ALL SELECT 13, 'Bebidas', 'Agua sin gas 2 L', 5 FROM dual
    UNION ALL SELECT 14, 'Bebidas', 'Agua con gas 500 ml', 3 FROM dual
    UNION ALL SELECT 15, 'Bebidas', 'Gaseosa cola 500 ml', 4 FROM dual
    UNION ALL SELECT 16, 'Bebidas', 'Gaseosa limón 500 ml', 4 FROM dual
    UNION ALL SELECT 17, 'Bebidas', 'Gaseosa naranja 500 ml', 4 FROM dual
    UNION ALL SELECT 18, 'Bebidas', 'Gaseosa piña 500 ml', 4 FROM dual
    UNION ALL SELECT 19, 'Bebidas', 'Gaseosa cola sin azúcar 500 ml', 4.5 FROM dual
    UNION ALL SELECT 20, 'Bebidas', 'Gaseosa familiar 1.5 L', 7 FROM dual
    UNION ALL SELECT 21, 'Botiquín', 'Pastillas para garganta', 5 FROM dual
    UNION ALL SELECT 22, 'Botiquín', 'Paracetamol', 5 FROM dual
    UNION ALL SELECT 23, 'Botiquín', 'Ibuprofeno', 6 FROM dual
    UNION ALL SELECT 24, 'Botiquín', 'Tabletas de vitamina C', 7 FROM dual
    UNION ALL SELECT 25, 'Botiquín', 'Sales de rehidratación', 5 FROM dual
    UNION ALL SELECT 26, 'Botiquín', 'Curitas adhesivas', 3 FROM dual
    UNION ALL SELECT 27, 'Botiquín', 'Gasas estériles', 4 FROM dual
    UNION ALL SELECT 28, 'Botiquín', 'Cinta microporosa', 5 FROM dual
    UNION ALL SELECT 29, 'Botiquín', 'Alcohol en gel', 6 FROM dual
    UNION ALL SELECT 30, 'Botiquín', 'Compresa fría', 8 FROM dual
    UNION ALL SELECT 31, 'Higiene personal', 'Cepillo dental de viaje', 4 FROM dual
    UNION ALL SELECT 32, 'Higiene personal', 'Pasta dental 30 g', 4 FROM dual
    UNION ALL SELECT 33, 'Higiene personal', 'Jabón de tocador 80 g', 3 FROM dual
    UNION ALL SELECT 34, 'Higiene personal', 'Champú de viaje 60 ml', 5 FROM dual
    UNION ALL SELECT 35, 'Higiene personal', 'Acondicionador de viaje 60 ml', 5 FROM dual
    UNION ALL SELECT 36, 'Higiene personal', 'Desodorante mini', 8 FROM dual
    UNION ALL SELECT 37, 'Higiene personal', 'Pañuelos faciales', 3 FROM dual
    UNION ALL SELECT 38, 'Higiene personal', 'Toallitas húmedas', 5 FROM dual
    UNION ALL SELECT 39, 'Higiene personal', 'Peine de viaje', 3 FROM dual
    UNION ALL SELECT 40, 'Higiene personal', 'Kit de afeitado', 8 FROM dual
    UNION ALL SELECT 41, 'Artículos de hospedaje', 'Tapones para oídos', 4 FROM dual
    UNION ALL SELECT 42, 'Artículos de hospedaje', 'Antifaz de descanso', 7 FROM dual
    UNION ALL SELECT 43, 'Artículos de hospedaje', 'Pilas AA 2 unidades', 8 FROM dual
    UNION ALL SELECT 44, 'Artículos de hospedaje', 'Pilas AAA 2 unidades', 8 FROM dual
    UNION ALL SELECT 45, 'Artículos de hospedaje', 'Poncho de lluvia plegable', 7 FROM dual
    UNION ALL SELECT 46, 'Artículos de hospedaje', 'Costurero de viaje', 5 FROM dual
    UNION ALL SELECT 47, 'Artículos de hospedaje', 'Adaptador de enchufe', 16 FROM dual
    UNION ALL SELECT 48, 'Artículos de hospedaje', 'Bolsa impermeable de viaje', 7 FROM dual
    UNION ALL SELECT 49, 'Artículos de hospedaje', 'Candado de equipaje', 9 FROM dual
    UNION ALL SELECT 50, 'Artículos de hospedaje', 'Cable USB-C', 13 FROM dual
), marcas AS (
    SELECT LEVEL n,
           REGEXP_SUBSTR('Qori,Nival,Sumaq,Inti,Luma,Aruna,Wayra,Lirio,Orbe,Alero', '[^,]+', 1, LEVEL) marca
    FROM dual CONNECT BY LEVEL <= 10
), catalogo AS (
    SELECT (r.n - 1) * 10 + m.n n, r.categoria,
           r.nombre || ' ' || m.marca || ' [RB01-' || LPAD((r.n - 1) * 10 + m.n, 3, '0') || ']' nombre,
           r.precio + (m.n - 1) * 0.30 precio
    FROM referencias r CROSS JOIN marcas m
)
SELECT s.nombre, s.precio, 10 + MOD(s.n * 13, 44) - CASE WHEN s.n <= 30 THEN 1 ELSE 0 END,
       4 + MOD(s.n, 5),
       (SELECT MIN(c.idCategoria) FROM categoria c WHERE UPPER(TRIM(c.nombreCategoria)) = UPPER(s.categoria)),
       'ACTIVO'
FROM catalogo s
WHERE NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombreProducto = s.nombre);

INSERT INTO movimiento_inventario (idProducto, idTipoMovimientoAlmacen, cantidad,
                                   stockResultante, idUsuario, observacion, fechaMovimiento, claveOperacion)
WITH productos_demo AS (
    SELECT p.idProducto, p.stock, TO_NUMBER(SUBSTR(p.nombreProducto, -4, 3) DEFAULT NULL ON CONVERSION ERROR) n
    FROM producto p WHERE REGEXP_LIKE(p.nombreProducto, ' \[RB01-[0-9]{3}\]$')
), entradas AS (
    SELECT p.idProducto, p.stock,
           CASE WHEN p.n BETWEEN 1 AND 30 AND p.stock = 9 + MOD(p.n * 13, 44) THEN 1 ELSE 0 END vendido
    FROM productos_demo p
)
SELECT p.idProducto, t.idTipoMovimientoAlmacen, p.stock + p.vendido, p.stock + p.vendido, u.idUsuario,
       CASE WHEN p.vendido = 1 THEN 'Existencias iniciales del catálogo RB01 (venta demo)'
            ELSE 'Existencias iniciales del catálogo RB01' END,
       CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP),
       'RB01-STOCK-' || p.idProducto
FROM entradas p JOIN tipo_movimiento_almacen t ON t.codigo = 'AJ_ENT' AND t.signo = 1
JOIN usuario u ON u.idUsuario = (
    SELECT MIN(a.idUsuario) KEEP (DENSE_RANK FIRST ORDER BY CASE WHEN a.username = 'operacion_local' THEN 0 ELSE 1 END)
    FROM usuario a WHERE a.estadoUsuario = 'ACTIVO'
)
WHERE p.stock + p.vendido > 0
  AND NOT EXISTS (SELECT 1 FROM movimiento_inventario m WHERE m.claveOperacion = 'RB01-STOCK-' || p.idProducto);

-- 4.1. Treinta ventas pagadas. Cada producto preparado registra una unidad de salida.
INSERT INTO venta (compradorNombreLibre, idUsuario, modalidadCobro, estadoVenta,
                   fechaRegistro, fechaConfirmacion, claveRegistro, firmaSolicitud)
WITH ventas_demo AS (
    SELECT m.idUsuario, m.fechaMovimiento, 'RB02-VTA-' || SUBSTR(p.nombreProducto, -4, 3) clave
    FROM movimiento_inventario m JOIN producto p ON p.idProducto = m.idProducto
    WHERE m.claveOperacion = 'RB01-STOCK-' || p.idProducto
      AND m.observacion = 'Existencias iniciales del catálogo RB01 (venta demo)'
)
SELECT 'Cliente demo de tienda', s.idUsuario, 'SEPARADA', 'CONFIRMADA', s.fechaMovimiento,
       s.fechaMovimiento, s.clave, LOWER(RAWTOHEX(STANDARD_HASH(s.clave, 'SHA256')))
FROM ventas_demo s
WHERE NOT EXISTS (SELECT 1 FROM venta v WHERE v.claveRegistro = s.clave);

INSERT INTO detalle_venta (idVenta, idProducto, descripcion, cantidad, valorUnitario, igv, estadoLinea)
SELECT v.idVenta, p.idProducto, p.nombreProducto, 1, p.precioActual, 0, 'ACTIVA'
FROM venta v JOIN producto p ON p.nombreProducto LIKE '% [RB01-' || SUBSTR(v.claveRegistro, -3) || ']'
WHERE REGEXP_LIKE(v.claveRegistro, '^RB02-VTA-[0-9]{3}$')
  AND NOT EXISTS (SELECT 1 FROM detalle_venta d WHERE d.idVenta = v.idVenta);

INSERT INTO movimiento_inventario (idProducto, idDetalleVenta, idTipoMovimientoAlmacen, cantidad,
                                   stockResultante, idUsuario, observacion, fechaMovimiento, claveOperacion)
SELECT d.idProducto, d.idDetalleVenta, t.idTipoMovimientoAlmacen, d.cantidad, p.stock, v.idUsuario,
       'Salida demo por ' || v.claveRegistro, v.fechaConfirmacion, 'STOCK-' || v.claveRegistro
FROM venta v JOIN detalle_venta d ON d.idVenta = v.idVenta JOIN producto p ON p.idProducto = d.idProducto
JOIN tipo_movimiento_almacen t ON t.codigo = 'VENTA' AND t.signo = -1
WHERE v.claveRegistro LIKE 'RB02-VTA-%'
  AND NOT EXISTS (SELECT 1 FROM movimiento_inventario m WHERE m.claveOperacion = 'STOCK-' || v.claveRegistro);

INSERT INTO turno_caja (idUsuario, fechaApertura, fechaCierre, montoBase, estadoSesion)
WITH horarios AS (
    SELECT v.idUsuario, MIN(v.fechaConfirmacion) apertura, MAX(v.fechaConfirmacion) cierre
    FROM venta v WHERE v.claveRegistro LIKE 'RB02-VTA-%' GROUP BY v.idUsuario
)
SELECT h.idUsuario, h.apertura, h.cierre, 0, 0 FROM horarios h
WHERE EXISTS (SELECT 1 FROM venta v WHERE v.idUsuario = h.idUsuario AND v.claveRegistro LIKE 'RB02-VTA-%'
              AND NOT EXISTS (SELECT 1 FROM comprobante c WHERE c.idVenta = v.idVenta))
  AND NOT EXISTS (SELECT 1 FROM turno_caja t WHERE t.idUsuario = h.idUsuario
                  AND t.fechaApertura = h.apertura AND t.fechaCierre = h.cierre);

INSERT INTO comprobante (idVenta, idTipoComprobante, idTurno, serie, subtotal, igv, totalPagar,
                         estadoComprobante, claveIdempotencia)
WITH ventas_demo AS (
    SELECT v.idVenta, v.idUsuario, MIN(v.fechaConfirmacion) OVER (PARTITION BY v.idUsuario) apertura,
           MAX(v.fechaConfirmacion) OVER (PARTITION BY v.idUsuario) cierre
    FROM venta v WHERE v.claveRegistro LIKE 'RB02-VTA-%'
)
SELECT v.idVenta, tc.idTipoComprobante, t.idTurno, s.serie, d.subtotal, d.igv, d.total,
       'BORRADOR', 'CUENTA-V-' || v.idVenta
FROM ventas_demo v JOIN detalle_venta d ON d.idVenta = v.idVenta
JOIN turno_caja t ON t.idUsuario = v.idUsuario AND t.fechaApertura = v.apertura AND t.fechaCierre = v.cierre
JOIN tipo_comprobante tc ON tc.codigo = 'CUENTA'
JOIN serie_comprobante s ON s.idTipoComprobante = tc.idTipoComprobante AND s.serie = 'CTA'
WHERE NOT EXISTS (SELECT 1 FROM comprobante c WHERE c.claveIdempotencia = 'CUENTA-V-' || v.idVenta);

INSERT INTO comprobante_detalle (idComprobante, idDetalleVenta, numeroLinea, descripcion,
                                 cantidad, valorUnitario, igv)
SELECT c.idComprobante, d.idDetalleVenta, 1, d.descripcion, d.cantidad, d.valorUnitario, d.igv
FROM venta v JOIN detalle_venta d ON d.idVenta = v.idVenta
JOIN comprobante c ON c.claveIdempotencia = 'CUENTA-V-' || v.idVenta
WHERE v.claveRegistro LIKE 'RB02-VTA-%'
  AND NOT EXISTS (SELECT 1 FROM comprobante_detalle l WHERE l.idComprobante = c.idComprobante AND l.numeroLinea = 1);

-- 4.2. Cobros de alojamiento y tienda. Conserva los abonos y saldos pendientes del lote inicial.
INSERT INTO pago (idComprobante, idFormaPago, idUsuario, idTurno, monto, numeroOperacion,
                  fechaPago, verificado, claveIdempotencia)
WITH reservas_demo AS (
    SELECT r.idUsuario, r.claveRegistro, c.idComprobante, c.idTurno, d.fechaIngresoReal, d.costoEstadia,
           TO_NUMBER(SUBSTR(r.claveRegistro, 10) DEFAULT NULL ON CONVERSION ERROR) n
    FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
    JOIN comprobante c ON c.claveIdempotencia = 'CUENTA-R-' || r.idReserva
    WHERE REGEXP_LIKE(r.claveRegistro, '^RB01-RES-[0-9]{4}$') AND d.fechaIngresoReal IS NOT NULL
), cobros AS (
    SELECT r.idComprobante, r.idUsuario, r.idTurno, r.claveRegistro,
           CASE WHEN r.n BETWEEN 996 AND 997 THEN ROUND(r.costoEstadia / 2, 2) ELSE r.costoEstadia END monto,
           r.fechaIngresoReal + INTERVAL '15' MINUTE fecha,
           CASE MOD(r.n - 1, 4) WHEN 0 THEN 'EFECTIVO' WHEN 1 THEN 'TARJETA' WHEN 2 THEN 'YAPE' ELSE 'PLIN' END medio
    FROM reservas_demo r WHERE r.n <= 800 OR r.n BETWEEN 991 AND 997
    UNION ALL
    SELECT c.idComprobante, v.idUsuario, c.idTurno, v.claveRegistro, c.totalPagar, v.fechaConfirmacion,
           CASE MOD(TO_NUMBER(SUBSTR(v.claveRegistro, -3) DEFAULT NULL ON CONVERSION ERROR) - 1, 3)
               WHEN 0 THEN 'TARJETA' WHEN 1 THEN 'YAPE' ELSE 'PLIN' END
    FROM venta v JOIN comprobante c ON c.claveIdempotencia = 'CUENTA-V-' || v.idVenta
    WHERE REGEXP_LIKE(v.claveRegistro, '^RB02-VTA-[0-9]{3}$')
), medios AS (
    SELECT UPPER(TRIM(nombrePago)) nombre, MIN(idFormaPago) idFormaPago
    FROM forma_pago GROUP BY UPPER(TRIM(nombrePago))
)
SELECT c.idComprobante, f.idFormaPago, c.idUsuario, c.idTurno, c.monto,
       CASE WHEN c.medio <> 'EFECTIVO' THEN 'DEMO-' || c.claveRegistro END,
       c.fecha, 1, 'PAGO-' || c.claveRegistro
FROM cobros c JOIN medios f ON f.nombre = c.medio
WHERE NOT EXISTS (SELECT 1 FROM pago p WHERE p.claveIdempotencia = 'PAGO-' || c.claveRegistro);

INSERT INTO flujo_caja (idTurno, idTipoMovimiento, idPago, concepto, monto, fechaHora)
SELECT p.idTurno, t.idTipoMovimiento, p.idPago, 'Cobro demo ' || p.claveIdempotencia, p.monto, p.fechaPago
FROM pago p JOIN comprobante c ON c.idComprobante = p.idComprobante
JOIN tipo_movimiento_caja t ON t.nombreMovimiento = CASE WHEN c.idVenta IS NULL THEN 'COBRO_RESERVA' ELSE 'COBRO_VENTA' END
  AND t.signo = 1
WHERE (p.claveIdempotencia LIKE 'PAGO-RB01-RES-%' OR p.claveIdempotencia LIKE 'PAGO-RB02-VTA-%')
  AND NOT EXISTS (SELECT 1 FROM flujo_caja f WHERE f.idPago = p.idPago);

INSERT INTO arqueo_caja (idTurno, idFormaPago, montoCalculado, montoDeclarado)
WITH turnos_demo AS (
    SELECT DISTINCT p.idTurno FROM pago p
    WHERE p.claveIdempotencia LIKE 'PAGO-RB01-RES-%' OR p.claveIdempotencia LIKE 'PAGO-RB02-VTA-%'
), totales AS (
    SELECT p.idTurno, p.idFormaPago, SUM(p.monto) importe
    FROM pago p JOIN turnos_demo t ON t.idTurno = p.idTurno
    WHERE p.verificado = 1 GROUP BY p.idTurno, p.idFormaPago
)
SELECT t.idTurno, f.idFormaPago, NVL(s.importe, 0), NVL(s.importe, 0)
FROM turnos_demo t CROSS JOIN forma_pago f
LEFT JOIN totales s ON s.idTurno = t.idTurno AND s.idFormaPago = f.idFormaPago
WHERE NOT EXISTS (SELECT 1 FROM arqueo_caja a WHERE a.idTurno = t.idTurno AND a.idFormaPago = f.idFormaPago);

-- 5. Mobiliario por cuarto. Se conservan las camas y piezas equivalentes ya asignadas.
INSERT INTO tipo_item (nombreItem)
WITH tipos (nombre) AS (
    SELECT 'Televisor' FROM dual UNION ALL SELECT 'Ropero' FROM dual
    UNION ALL SELECT 'Mesa' FROM dual UNION ALL SELECT 'Florero artificial' FROM dual
    UNION ALL SELECT 'Cama' FROM dual
)
SELECT t.nombre FROM tipos t
WHERE NOT EXISTS (SELECT 1 FROM tipo_item e WHERE UPPER(TRIM(e.nombreItem)) = UPPER(t.nombre));

INSERT INTO item (nombreItem, idTipoItem, idHabitacion, estadoItem, condicionItem)
WITH piezas (tipo, nombre) AS (
    SELECT 'Televisor', 'Televisor' FROM dual UNION ALL SELECT 'Ropero', 'Ropero pequeño' FROM dual
    UNION ALL SELECT 'Mesa', 'Mesa' FROM dual UNION ALL SELECT 'Florero artificial', 'Florero artificial' FROM dual
    UNION ALL SELECT 'Cama', 'Cama' FROM dual
), porCuarto AS (
    SELECT h.idHabitacion, p.tipo,
           h.numeroCuarto,
           p.nombre || CASE
               WHEN p.tipo = 'Mesa' AND UPPER(th.nombreTipo) LIKE '%MATRIMONIAL%' THEN ' grande'
               WHEN p.tipo = 'Mesa' THEN ' pequeña'
               WHEN p.tipo = 'Cama' AND UPPER(th.nombreTipo) LIKE '%MATRIMONIAL%' THEN ' de dos plazas'
               WHEN p.tipo = 'Cama' THEN ' de plaza y media' END nombre,
           CASE WHEN p.tipo = 'Cama' AND UPPER(th.nombreTipo) LIKE '%MATRIMONIAL%' THEN CEIL(h.plazasCama / 2)
                WHEN p.tipo = 'Cama' AND UPPER(th.nombreTipo) LIKE 'DOBLE%' THEN h.plazasCama
                ELSE 1 END cantidad
    FROM habitacion h JOIN tipo_habitacion th ON th.idTipoHabitacion = h.idTipo CROSS JOIN piezas p
    WHERE p.tipo <> 'Florero artificial' OR MOD(ORA_HASH(h.numeroCuarto, 999, 301), 4) = 0
), unidades AS (
    SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 99
), plan AS (
    SELECT p.idHabitacion, p.tipo,
           p.nombre || CASE WHEN p.cantidad > 1 THEN ' N.° ' || u.n END || ' [RB01 ' || p.numeroCuarto || ']' nombre
    FROM porCuarto p JOIN unidades u ON u.n <= p.cantidad
)
SELECT s.nombre, (SELECT MIN(t.idTipoItem) FROM tipo_item t WHERE UPPER(TRIM(t.nombreItem)) = UPPER(s.tipo)),
       s.idHabitacion, 'EN_HABITACION', 'OPTIMO'
FROM plan s
WHERE NOT EXISTS (SELECT 1 FROM item i WHERE i.nombreItem = s.nombre)
  AND NOT EXISTS (
      SELECT 1 FROM item i JOIN tipo_item t ON t.idTipoItem = i.idTipoItem
      WHERE i.idHabitacion = s.idHabitacion AND i.estadoItem = 'EN_HABITACION'
        AND (UPPER(TRIM(t.nombreItem)) = UPPER(s.tipo)
             OR (s.tipo = 'Ropero' AND UPPER(TRIM(t.nombreItem)) IN ('CÓMODA', 'COMODA')))
  );

INSERT INTO movimiento_inventario (idItem, idHabitacion, idTipoMovimientoAlmacen, cantidad,
                                   idUsuario, observacion, fechaMovimiento, claveOperacion)
SELECT i.idItem, i.idHabitacion, t.idTipoMovimientoAlmacen, 1, u.idUsuario,
       'Asignación demo al cuarto ' || h.numeroCuarto,
       CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP) - INTERVAL '370' DAY(3),
       'RB01-ASIG-' || i.idItem
FROM item i JOIN habitacion h ON h.idHabitacion = i.idHabitacion AND i.nombreItem LIKE '% [RB01 ' || h.numeroCuarto || ']'
JOIN tipo_movimiento_almacen t ON t.codigo = 'ASIG_ITEM' AND t.signo = 0
JOIN usuario u ON u.idUsuario = (
    SELECT MIN(a.idUsuario) KEEP (DENSE_RANK FIRST ORDER BY CASE WHEN a.username = 'operacion_local' THEN 0 ELSE 1 END)
    FROM usuario a WHERE a.estadoUsuario = 'ACTIVO'
)
WHERE i.estadoItem = 'EN_HABITACION'
  AND NOT EXISTS (SELECT 1 FROM movimiento_inventario m WHERE m.claveOperacion = 'RB01-ASIG-' || i.idItem);

-- 6. Inventario de habitaciones. 190 revisiones aprobadas y 10 por atender, una incidencia por revisión.
INSERT INTO informe_revision (idDetalleReserva, idHabitacion, idUsuario, fechaHora, observaciones,
                              estadoInforme, idUsuarioAprobador)
WITH estancias AS (
    SELECT d.idDetalleReserva, d.idHabitacion, d.fechaSalidaReal, r.idUsuario,
           TO_NUMBER(SUBSTR(r.claveRegistro, 10) DEFAULT NULL ON CONVERSION ERROR) n,
           ROW_NUMBER() OVER (PARTITION BY d.idHabitacion ORDER BY d.fechaSalidaReal DESC, d.idDetalleReserva) ultima
    FROM reserva r JOIN detalle_reserva d ON d.idReserva = r.idReserva
    WHERE REGEXP_LIKE(r.claveRegistro, '^RB01-RES-[0-9]{4}$') AND d.estadoDetalle = 'FINALIZADA'
), pendientes AS (
    SELECT s.idDetalleReserva, s.idHabitacion, s.idUsuario,
           ROW_NUMBER() OVER (ORDER BY h.numeroCuarto) posicion
    FROM estancias s JOIN habitacion h ON h.idHabitacion = s.idHabitacion
    WHERE s.ultima = 1 AND h.estadoHabitacion IN ('DISPONIBLE', 'POR_LIMPIAR', 'EN_LIMPIEZA')
      AND NOT EXISTS (SELECT 1 FROM detalle_reserva d WHERE d.idHabitacion = h.idHabitacion AND d.estadoDetalle = 'EN_CURSO')
      AND NOT EXISTS (SELECT 1 FROM informe_revision i WHERE i.idHabitacion = h.idHabitacion
                      AND (i.estadoInforme IS NULL OR i.estadoInforme = 'POR_APROBAR')
                      AND (i.observaciones IS NULL OR i.observaciones NOT LIKE 'RB01-INV-%'))
), casos AS (
    SELECT s.n, s.idDetalleReserva, s.idHabitacion, s.idUsuario,
           CAST(s.fechaSalidaReal AS TIMESTAMP) + INTERVAL '45' MINUTE fecha,
           'APROBADO' estado, 'Revisión histórica de mobiliario' nota
    FROM estancias s WHERE s.n BETWEEN 1 AND 190
    UNION ALL
    SELECT 190 + p.posicion, p.idDetalleReserva, p.idHabitacion, p.idUsuario,
           CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP) - INTERVAL '30' MINUTE,
           'POR_APROBAR', 'Incidencia de mobiliario pendiente de atención'
    FROM pendientes p WHERE p.posicion <= 10
)
SELECT s.idDetalleReserva, s.idHabitacion, s.idUsuario, s.fecha,
       'RB01-INV-' || LPAD(s.n, 4, '0') || ' ' || s.nota, s.estado,
       CASE WHEN s.estado = 'APROBADO' THEN COALESCE((
           SELECT MIN(u.idUsuario) FROM usuario u JOIN usuario_rol ur ON ur.idUsuario = u.idUsuario
           JOIN rol r ON r.idRol = ur.idRol WHERE u.estadoUsuario = 'ACTIVO' AND UPPER(r.nombreRol) = 'GERENTE'
       ), s.idUsuario) END
FROM casos s
WHERE NOT EXISTS (SELECT 1 FROM informe_revision i WHERE i.observaciones LIKE 'RB01-INV-' || LPAD(s.n, 4, '0') || ' %')
  AND (s.estado = 'APROBADO' OR NOT EXISTS (
       SELECT 1 FROM informe_revision i WHERE i.idHabitacion = s.idHabitacion
         AND (i.estadoInforme IS NULL OR i.estadoInforme = 'POR_APROBAR')));

INSERT INTO detalle_informe (idInforme, idItem, idHabitacion, estadoEncontrado, observacion)
WITH revision AS (
    SELECT i.idInformeRevision, i2.idItem, i2.idHabitacion,
           TO_NUMBER(SUBSTR(i.observaciones, 10, 4) DEFAULT NULL ON CONVERSION ERROR) n,
           ROW_NUMBER() OVER (PARTITION BY i.idInformeRevision
               ORDER BY CASE WHEN i2.nombreItem LIKE '% [RB01 %]' AND i2.condicionItem = 'OPTIMO' THEN 0 ELSE 1 END,
                        ORA_HASH(i2.nombreItem || i.observaciones), i2.idItem) posicion
    FROM informe_revision i JOIN item i2 ON i2.idHabitacion = i.idHabitacion AND i2.estadoItem = 'EN_HABITACION'
    WHERE REGEXP_LIKE(i.observaciones, '^RB01-INV-[0-9]{4} ')
      AND NOT EXISTS (SELECT 1 FROM detalle_informe a WHERE a.idInforme = i.idInformeRevision)
)
SELECT r.idInformeRevision, r.idItem, r.idHabitacion,
       CASE WHEN r.posicion <> 1 THEN 'OPTIMO' WHEN MOD(r.n, 5) = 0 THEN 'FALTANTE' ELSE 'DANADO' END,
       CASE WHEN r.posicion = 1 THEN
           CASE WHEN MOD(r.n, 5) = 0 THEN 'Pieza faltante en revisión demo' ELSE 'Desgaste detectado en revisión demo' END END
FROM revision r;

INSERT INTO ticket_mantenimiento (idDetalleInforme, idEmpleado, tipoAccion, estadoTicket,
                                 fechaGeneracion, fechaResolucion, costoReparacion, observacionTecnica)
SELECT d.idDetalleInforme, u.idEmpleado,
       CASE WHEN d.estadoEncontrado = 'FALTANTE' THEN 'REPOSICION' ELSE 'REPARACION' END, 'EMITIDO',
       i.fechaHora + INTERVAL '10' MINUTE, i.fechaHora + INTERVAL '40' MINUTE,
       CASE WHEN d.estadoEncontrado = 'DANADO' THEN
           10 + MOD(TO_NUMBER(SUBSTR(i.observaciones, 10, 4) DEFAULT NULL ON CONVERSION ERROR), 12) * 2.50 END,
       CASE WHEN d.estadoEncontrado = 'FALTANTE' THEN 'RB01: pieza repuesta y verificada'
            ELSE 'RB01: ajuste y verificación de funcionamiento completados' END
FROM informe_revision i JOIN detalle_informe d ON d.idInforme = i.idInformeRevision AND d.estadoEncontrado IN ('DANADO', 'FALTANTE')
JOIN usuario u ON u.idUsuario = i.idUsuario
WHERE REGEXP_LIKE(i.observaciones, '^RB01-INV-[0-9]{4} ')
  AND SUBSTR(i.observaciones, 10, 4) BETWEEN '0001' AND '0190'
  AND NOT EXISTS (SELECT 1 FROM ticket_mantenimiento t WHERE t.idDetalleInforme = d.idDetalleInforme);

INSERT INTO movimiento_inventario (idItem, idHabitacion, idTipoMovimientoAlmacen, cantidad,
                                   idUsuario, observacion, fechaMovimiento, claveOperacion)
SELECT d.idItem, d.idHabitacion, m.idTipoMovimientoAlmacen, 1, i.idUsuario,
       'Resolución de ticket TKT-' || t.idTicket || ' (RB01)', t.fechaResolucion, 'ticket:' || t.idTicket
FROM informe_revision i JOIN detalle_informe d ON d.idInforme = i.idInformeRevision
JOIN ticket_mantenimiento t ON t.idDetalleInforme = d.idDetalleInforme
JOIN tipo_movimiento_almacen m ON m.codigo = 'EST_ITEM' AND m.signo = 0
WHERE REGEXP_LIKE(i.observaciones, '^RB01-INV-[0-9]{4} ') AND t.observacionTecnica LIKE 'RB01:%'
  AND t.estadoTicket = 'EMITIDO'
  AND NOT EXISTS (SELECT 1 FROM movimiento_inventario a WHERE a.claveOperacion = 'ticket:' || t.idTicket);

