-- Ejecutar en REBOLSYSDBV2 después de V16 y DATOS_INICIALES_REBOLSYS_V2.sql.
-- MODREBOLSYS es el origen de habitaciones y mobiliario. Confirma la carga con COMMIT.
DECLARE
  v_usuario NUMBER;
  v_asignacion NUMBER;
  v_entrada NUMBER;
  v_item NUMBER;
  v_producto NUMBER;
BEGIN
  IF SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') <> 'REBOLSYSDBV2' THEN
    RAISE_APPLICATION_ERROR(-20070, 'Ejecuta la carga en REBOLSYSDBV2');
  END IF;

  SELECT idUsuario INTO v_usuario FROM usuario
  WHERE username = 'gerencia_local' AND estadoUsuario = 'ACTIVO' FOR UPDATE;
  SELECT idTipoMovimientoAlmacen INTO v_asignacion FROM tipo_movimiento_almacen WHERE codigo = 'ASIG_ITEM';
  SELECT idTipoMovimientoAlmacen INTO v_entrada FROM tipo_movimiento_almacen WHERE codigo = 'AJ_ENT';

  INSERT INTO tipo_habitacion (nombreTipo, capacidadMax, precioBase)
  SELECT t.nombreTipo, t.capacidadMax, t.precioBase FROM MODREBOLSYS.tipo_habitacion t
  WHERE EXISTS (SELECT 1 FROM MODREBOLSYS.habitacion h WHERE h.idTipo = t.idTipoHabitacion)
    AND NOT EXISTS (SELECT 1 FROM tipo_habitacion d WHERE d.nombreTipo = t.nombreTipo);

  INSERT INTO habitacion (numeroCuarto, idTipo, estadoHabitacion, capacidadPersonas, plazasCama,
                          maxMenoresSinCama, precioPorNoche)
  SELECT h.numeroCuarto, td.idTipoHabitacion, h.estadoHabitacion, h.capacidadPersonas, h.plazasCama,
         h.maxMenoresSinCama, t.precioBase
  FROM MODREBOLSYS.habitacion h
  JOIN MODREBOLSYS.tipo_habitacion t ON t.idTipoHabitacion = h.idTipo
  JOIN tipo_habitacion td ON td.nombreTipo = t.nombreTipo
  WHERE NOT EXISTS (SELECT 1 FROM habitacion d WHERE d.numeroCuarto = h.numeroCuarto);

  INSERT INTO tipo_item (nombreItem)
  SELECT t.nombreItem FROM MODREBOLSYS.tipo_item t
  WHERE EXISTS (SELECT 1 FROM MODREBOLSYS.item i WHERE i.idTipoItem = t.idTipoItem)
    AND NOT EXISTS (SELECT 1 FROM tipo_item d WHERE d.nombreItem = t.nombreItem);

  FOR mueble IN (
    SELECT i.nombreItem, td.idTipoItem, hd.idHabitacion, hd.numeroCuarto, i.estadoItem, i.condicionItem
    FROM MODREBOLSYS.item i
    JOIN MODREBOLSYS.tipo_item t ON t.idTipoItem = i.idTipoItem
    JOIN tipo_item td ON td.nombreItem = t.nombreItem
    LEFT JOIN MODREBOLSYS.habitacion h ON h.idHabitacion = i.idHabitacion
    LEFT JOIN habitacion hd ON hd.numeroCuarto = h.numeroCuarto
    WHERE NOT EXISTS (SELECT 1 FROM item d WHERE UPPER(TRIM(d.nombreItem)) = UPPER(TRIM(i.nombreItem)))
    ORDER BY i.idItem
  ) LOOP
    INSERT INTO item (nombreItem, idTipoItem, idHabitacion, estadoItem, condicionItem)
    VALUES (mueble.nombreItem, mueble.idTipoItem, mueble.idHabitacion, mueble.estadoItem, mueble.condicionItem)
    RETURNING idItem INTO v_item;

    IF mueble.idHabitacion IS NOT NULL THEN
      INSERT INTO movimiento_inventario (idItem, idHabitacion, idTipoMovimientoAlmacen, cantidad, idUsuario,
                                         condicionItem, observacion, fechaMovimiento, claveOperacion)
      VALUES (v_item, mueble.idHabitacion, v_asignacion, 1, v_usuario, mueble.condicionItem,
              'Asignación inicial según inventario anterior, habitación ' || mueble.numeroCuarto,
              CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP), 'CARGA-V2-ITEM-' || v_item);
    END IF;
  END LOOP;

  INSERT INTO categoria (nombreCategoria)
  SELECT nombre FROM (
    SELECT 'Bebidas' nombre FROM dual UNION ALL SELECT 'Snacks' FROM dual
    UNION ALL SELECT 'Higiene personal' FROM dual UNION ALL SELECT 'Botiquín' FROM dual
    UNION ALL SELECT 'Accesorios de viaje' FROM dual
  ) datos
  WHERE NOT EXISTS (SELECT 1 FROM categoria c WHERE UPPER(TRIM(c.nombreCategoria)) = UPPER(datos.nombre));

  FOR mercaderia IN (
    WITH catalogo (categoria, nombre, descripcion, precio, stock, minimo) AS (
      SELECT 'Bebidas', 'Agua mineral sin gas 625 ml', 'Botella individual de agua mineral.', 2.50, 48, 12 FROM dual
      UNION ALL SELECT 'Bebidas', 'Agua mineral sin gas 1.5 L', 'Botella de agua mineral para compartir.', 4.50, 20, 6 FROM dual
      UNION ALL SELECT 'Bebidas', 'Agua mineral con gas 500 ml', 'Botella individual con gas.', 3.00, 16, 5 FROM dual
      UNION ALL SELECT 'Bebidas', 'Gaseosa cola 500 ml', 'Gaseosa de cola en botella personal.', 4.00, 28, 8 FROM dual
      UNION ALL SELECT 'Bebidas', 'Gaseosa cola sin azúcar 500 ml', 'Gaseosa de cola sin azúcar en botella personal.', 4.00, 14, 5 FROM dual
      UNION ALL SELECT 'Bebidas', 'Gaseosa amarilla 500 ml', 'Gaseosa de sabor dulce en botella personal.', 4.00, 24, 8 FROM dual
      UNION ALL SELECT 'Bebidas', 'Gaseosa lima limón 500 ml', 'Gaseosa cítrica en botella personal.', 3.80, 12, 5 FROM dual
      UNION ALL SELECT 'Bebidas', 'Jugo de durazno 300 ml', 'Néctar en envase individual.', 3.50, 12, 4 FROM dual
      UNION ALL SELECT 'Bebidas', 'Bebida isotónica de frutos rojos 500 ml', 'Bebida de sabor a frutos rojos en botella.', 5.50, 11, 4 FROM dual
      UNION ALL SELECT 'Bebidas', 'Té helado de limón 450 ml', 'Té frío sabor limón, listo para beber.', 4.50, 3, 4 FROM dual
      UNION ALL SELECT 'Snacks', 'Papas fritas clásicas 45 g', 'Bolsa individual de papas con sal.', 3.50, 25, 7 FROM dual
      UNION ALL SELECT 'Snacks', 'Papas fritas sabor queso 45 g', 'Bolsa individual de papas sabor queso.', 3.50, 14, 5 FROM dual
      UNION ALL SELECT 'Snacks', 'Chifles salados 50 g', 'Hojuelas de plátano con sal.', 3.00, 18, 6 FROM dual
      UNION ALL SELECT 'Snacks', 'Cancha serrana tostada 50 g', 'Maíz tostado en bolsa individual.', 3.00, 17, 5 FROM dual
      UNION ALL SELECT 'Snacks', 'Maní tostado sin sal 45 g', 'Bolsa individual de maní tostado.', 3.50, 13, 5 FROM dual
      UNION ALL SELECT 'Snacks', 'Mezcla de frutos secos 40 g', 'Mezcla de maní, pasas y almendras.', 5.00, 10, 4 FROM dual
      UNION ALL SELECT 'Snacks', 'Galletas de avena 60 g', 'Paquete individual de galletas de avena.', 2.80, 17, 6 FROM dual
      UNION ALL SELECT 'Snacks', 'Galletas rellenas de chocolate 54 g', 'Paquete individual de galletas con crema de chocolate.', 3.00, 20, 6 FROM dual
      UNION ALL SELECT 'Snacks', 'Barra de cereal con miel 30 g', 'Barra de avena y miel, envasada por unidad.', 3.50, 12, 4 FROM dual
      UNION ALL SELECT 'Snacks', 'Chocolate con leche 30 g', 'Tableta individual de chocolate con leche.', 4.50, 14, 5 FROM dual
      UNION ALL SELECT 'Snacks', 'Alfajor con manjar 45 g', 'Alfajor individual relleno de manjar blanco.', 4.00, 8, 4 FROM dual
      UNION ALL SELECT 'Snacks', 'Gomitas de frutas 50 g', 'Bolsa individual de gomitas surtidas.', 3.00, 10, 4 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Cepillo dental suave de viaje', 'Cepillo individual con cubierta protectora.', 5.00, 16, 5 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Pasta dental de viaje 30 g', 'Tubo compacto sabor menta.', 4.50, 15, 5 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Jabón de tocador 60 g', 'Barra individual envuelta.', 3.50, 20, 6 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Champú de viaje 60 ml', 'Frasco pequeño de champú de uso diario.', 6.00, 12, 4 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Toallitas húmedas 10 unidades', 'Paquete resellable de bolsillo.', 4.50, 14, 4 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Pañuelos descartables 10 unidades', 'Paquete de bolsillo.', 2.50, 20, 5 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Toallas higiénicas 8 unidades', 'Paquete de toallas con alas.', 8.50, 8, 3 FROM dual
      UNION ALL SELECT 'Higiene personal', 'Protector solar de viaje 50 ml', 'Frasco compacto de protector solar.', 19.00, 3, 4 FROM dual
      UNION ALL SELECT 'Botiquín', 'Curitas surtidas 10 unidades', 'Caja pequeña de apósitos adhesivos.', 6.50, 10, 3 FROM dual
      UNION ALL SELECT 'Botiquín', 'Gasas estériles 5 unidades', 'Paquete de gasas de 5 x 5 cm.', 5.50, 8, 3 FROM dual
      UNION ALL SELECT 'Botiquín', 'Alcohol en gel 60 ml', 'Frasco de bolsillo con tapa.', 6.00, 9, 3 FROM dual
      UNION ALL SELECT 'Botiquín', 'Pastillas de miel y limón 8 unidades', 'Pastillas de caramelo en envase individual.', 4.00, 10, 3 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Cable USB-A a USB-C 1 m', 'Cable de carga y datos de un metro.', 17.00, 7, 2 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Adaptador de enchufe de viaje', 'Adaptador compacto de enchufe.', 22.00, 5, 2 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Tapones de espuma para oídos', 'Par de tapones en estuche individual.', 5.00, 10, 3 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Antifaz para dormir', 'Antifaz de tela con banda elástica.', 10.00, 6, 2 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Kit de costura de bolsillo', 'Estuche compacto con hilos, agujas y botones.', 7.00, 6, 2 FROM dual
      UNION ALL SELECT 'Accesorios de viaje', 'Paraguas plegable compacto', 'Paraguas de viaje con funda.', 28.00, 2, 3 FROM dual
    )
    SELECT p.*, c.idCategoria FROM catalogo p
    JOIN categoria c ON UPPER(TRIM(c.nombreCategoria)) = UPPER(p.categoria)
    WHERE NOT EXISTS (SELECT 1 FROM producto d WHERE UPPER(TRIM(d.nombreProducto)) = UPPER(p.nombre))
  ) LOOP
    INSERT INTO producto (nombreProducto, descripcion, precioActual, stock, stockMinimo, idCategoria, estadoProducto)
    VALUES (mercaderia.nombre, mercaderia.descripcion, mercaderia.precio, mercaderia.stock,
            mercaderia.minimo, mercaderia.idCategoria, 'ACTIVO')
    RETURNING idProducto INTO v_producto;

    INSERT INTO movimiento_inventario (idProducto, idTipoMovimientoAlmacen, cantidad, stockResultante,
                                       idUsuario, observacion, fechaMovimiento, claveOperacion)
    VALUES (v_producto, v_entrada, mercaderia.stock, mercaderia.stock, v_usuario, 'Stock inicial de catálogo de prueba',
            CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS TIMESTAMP), 'CARGA-V2-PRODUCTO-' || v_producto);
  END LOOP;
END;
/
