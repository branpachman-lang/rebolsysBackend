package pe.com.rebolsysBackend.propuesta;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.service.service.*;

/**
 * Transacciones independientes reales; elimina exclusivamente las filas creadas por cada prueba.
 */
@SpringBootTest(
    properties = {"spring.jpa.show-sql=false", "logging.level.pe.com.rebolsysBackend=INFO"})
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
class ConcurrenciaOracleTest {
  @Autowired EntityManager em;
  @Autowired PlatformTransactionManager manager;
  @Autowired VentaService ventas;
  @Autowired ReservaService reservas;
  @Autowired CuentaReservaService cuentas;
  @Autowired GastoService gastos;
  @Autowired CuentaVentaService cuentasVenta;

  @Test
  void dosOperadoresNoPuedenCobrarDosVecesLaMismaVentaDirecta() throws Exception {
    var d = preparar();
    Long[] forma = new Long[1];
    try {
      var turnos = abrirTurnos(d);
      forma[0] = tx().execute(s -> {
        var f = new FormaPago();
        f.setNombrePago("Venta " + UUID.randomUUID().toString().substring(0, 8));
        em.persist(f);
        return f.getIdFormaPago();
      });
      var v = ventas.registrarConfirmada(new VentaRequestDTO(UUID.randomUUID().toString(),
          d.usuarios().getFirst(), null, "Comprador de prueba", ModalidadCobro.SEPARADA, null,
          List.of(new VentaLineaRegistroDTO(d.producto(), 1))));
      assertThat(enParalelo(i -> {
        try {
          cuentasVenta.pagar(v.idVenta(), new PagoCuentaDTO(UUID.randomUUID().toString(),
              d.usuarios().get(i), turnos.get(i), forma[0], BigDecimal.ONE, null));
          return true;
        } catch (ReglaNegocioException ex) {
          assertThat(ex).hasMessageContaining("supera el saldo");
          return false;
        }
      })).containsExactlyInAnyOrder(true, false);
      var c = cuentasVenta.cuenta(v.idVenta());
      assertThat(c.pagado()).isEqualByComparingTo("1");
      assertThat(c.saldo()).isZero();
      assertThat(c.pagos()).hasSize(1);
    } finally {
      limpiar(d);
      if (forma[0] != null) tx().executeWithoutResult(s -> em.remove(em.find(FormaPago.class, forma[0])));
    }
  }

  @Test
  void registroYConfirmacionAtomicosNoDejanBorradorNiStockParcialAlFallar() {
    var d = preparar();
    try {
      abrirTurnos(d);
      String clave = UUID.randomUUID().toString();
      assertThatThrownBy(() -> ventas.registrarConfirmada(new VentaRequestDTO(clave,
          d.usuarios().getFirst(), null, "Prueba atómica", ModalidadCobro.SEPARADA, null,
          List.of(new VentaLineaRegistroDTO(d.producto(), 2)))))
          .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Stock insuficiente");
      tx().executeWithoutResult(s -> {
        assertThat(em.createQuery("select count(v) from Venta v where v.claveRegistro=:clave", Long.class)
            .setParameter("clave", clave).getSingleResult()).isZero();
        assertThat(em.find(Producto.class, d.producto()).getStock()).isEqualTo(1);
      });
    } finally {
      limpiar(d);
    }
  }

  @Test
  void dosSolicitudesSimultaneasConLaMismaClaveRegistranUnSoloGasto() throws Exception {
    var d = preparar();
    Long[] turno = new Long[1], tipo = new Long[1];
    try {
      tx().executeWithoutResult(s -> {
        var t = new TurnoCaja();
        t.setUsuario(em.find(Usuario.class, d.usuarios().getFirst()));
        t.setEstadoSesion(1);
        em.persist(t);
        turno[0] = t.getIdTurno();
        var m = new TipoMovimientoCaja();
        m.setNombreMovimiento("Gasto " + UUID.randomUUID().toString().substring(0, 8));
        m.setSigno(-1);
        em.persist(m);
        tipo[0] = m.getIdTipoMovimiento();
      });
      var request = new GastoRequestDTO(d.usuarios().getFirst(), turno[0], tipo[0], "Taxi para repuestos",
          BigDecimal.TEN, null, null, null, UUID.randomUUID().toString());
      var ids = new java.util.concurrent.ConcurrentLinkedQueue<Long>();
      assertThat(enParalelo(i -> {
        ids.add(gastos.registrar(request).idFlujo());
        return true;
      })).containsExactly(true, true);
      assertThat(new HashSet<>(ids)).hasSize(1);
      var h = gastos.historial(new GastoFiltroDTO(turno[0], null, null, null, null, null, null, null, null, null));
      assertThat(h.resumen().cantidadGastos()).isEqualTo(1);
      assertThat(h.resumen().totalGastado()).isEqualByComparingTo("10");
    } finally {
      tx().executeWithoutResult(s -> {
        em.createQuery("delete from FlujoCaja f where f.turnoCaja.usuario.idUsuario in :ids")
            .setParameter("ids", d.usuarios()).executeUpdate();
        if (turno[0] != null) em.remove(em.find(TurnoCaja.class, turno[0]));
        if (tipo[0] != null) em.remove(em.find(TipoMovimientoCaja.class, tipo[0]));
      });
      limpiar(d);
    }
  }

  @Test
  void dosOperadoresNoPuedenCobrarDosVecesElMismoSaldo() throws Exception {
    var d = preparar();
    List<Long> turnos = new ArrayList<>();
    Long[] forma = new Long[1];
    try {
      tx().executeWithoutResult(
              s -> {
                for (Long usuario : d.usuarios()) {
                  var t = new TurnoCaja();
                  t.setUsuario(em.find(Usuario.class, usuario));
                  t.setEstadoSesion(1);
                  em.persist(t);
                  turnos.add(t.getIdTurno());
                }
                var f = new FormaPago();
                f.setNombrePago("Conc_" + UUID.randomUUID().toString().substring(0, 8));
                em.persist(f);
                forma[0] = f.getIdFormaPago();
              });
      var r =
          reservas.registrar(
              new ReservaRegistroRequestDTO(
                  UUID.randomUUID().toString(),
                  d.clientes().getFirst(),
                  d.usuarios().getFirst(),
                  List.of(
                      new ReservaDetalleRegistroDTO(
                          d.habitacion(),
                          LocalDateTime.now(),
                          LocalDateTime.now().plusDays(1),
                          BigDecimal.TEN,
                          List.of()))));
      assertThat(
              enParalelo(
                  i -> {
                    try {
                      cuentas.pagar(
                          r.idReserva(),
                          new ReservaPagoRegistroDTO(
                              UUID.randomUUID().toString(),
                              d.usuarios().get(i),
                              turnos.get(i),
                              forma[0],
                              BigDecimal.TEN,
                              null,
                              null));
                      return true;
                    } catch (ReglaNegocioException ex) {
                      assertThat(ex).hasMessageContaining("supera el saldo");
                      return false;
                    }
                  }))
          .containsExactlyInAnyOrder(true, false);
      var cuenta = cuentas.cuenta(r.idReserva());
      assertThat(cuenta.totalPagado()).isEqualByComparingTo("10");
      assertThat(cuenta.saldoPendiente()).isZero();
    } finally {
      tx().executeWithoutResult(
              s -> {
                em.createQuery(
                        "delete from FlujoCaja f where f.turnoCaja.usuario.idUsuario in :ids")
                    .setParameter("ids", d.usuarios())
                    .executeUpdate();
                em.createQuery("delete from Pago p where p.usuario.idUsuario in :ids")
                    .setParameter("ids", d.usuarios())
                    .executeUpdate();
                em.createQuery(
                        "delete from ComprobanteDetalle l where"
                            + " l.comprobante.reserva.usuario.idUsuario in :ids")
                    .setParameter("ids", d.usuarios())
                    .executeUpdate();
                em.createQuery(
                        "delete from Comprobante c where c.reserva.usuario.idUsuario in :ids")
                    .setParameter("ids", d.usuarios())
                    .executeUpdate();
                for (Long id : turnos) em.remove(em.find(TurnoCaja.class, id));
                if (forma[0] != null) em.remove(em.find(FormaPago.class, forma[0]));
              });
      limpiar(d);
    }
  }

  record Datos(
      List<Long> usuarios,
      List<Long> empleados,
      List<Long> perfiles,
      List<Long> clientes,
      Long documento,
      Long categoria,
      Long producto,
      Long tipoHabitacion,
      Long habitacion,
      Long tipoVentaCreado) {}

  TransactionTemplate tx() {
    return new TransactionTemplate(manager);
  }

  List<Long> abrirTurnos(Datos d) {
    return tx().execute(s -> {
      List<Long> resultado = new ArrayList<>();
      for (Long id : d.usuarios()) {
        var t = new TurnoCaja();
        t.setUsuario(em.find(Usuario.class, id));
        t.setEstadoSesion(1);
        em.persist(t);
        resultado.add(t.getIdTurno());
      }
      return resultado;
    });
  }

  Datos preparar() {
    return tx().execute(
            s -> {
              String token = UUID.randomUUID().toString().substring(0, 8);
              var doc = new TipoDocumento();
              doc.setNombreDocumento("CONC_" + token);
              doc.setCodigoSunat("1");
              em.persist(doc);
              List<Long> us = new ArrayList<>(),
                  es = new ArrayList<>(),
                  ps = new ArrayList<>(),
                  cs = new ArrayList<>();
              for (int i = 0; i < 2; i++) {
                var p = new Perfil();
                p.setTipoDocumento(doc);
                p.setNumeroDocumento("7654321" + i);
                em.persist(p);
                ps.add(p.getIdPerfil());
                var n = new PersonaNatural();
                n.setPerfil(p);
                n.setNombres("Concurrencia");
                n.setApellidos("Prueba");
                n.setFechaNacimiento(LocalDate.of(1990, 1, 1));
                em.persist(n);
                p.setEsCliente(1);
                cs.add(p.getIdPerfil());
                var e = new Empleado();
                e.setPersonaNatural(n);
                e.setFechaContratacion(LocalDate.now());
                em.persist(e);
                es.add(e.getIdEmpleado());
                var u = new Usuario();
                u.setEmpleado(e);
                u.setUsername("conc_" + token + i);
                u.setEstadoUsuario(EstadoUsuario.ACTIVO);
                em.persist(u);
                us.add(u.getIdUsuario());
              }
              var cat = new Categoria();
              cat.setNombreCategoria("Conc_" + token);
              em.persist(cat);
              var producto = new Producto();
              producto.setCategoria(cat);
              producto.setNombreProducto("Conc_" + token);
              producto.setPrecioActual(BigDecimal.ONE);
              producto.setStock(1);
              producto.setStockMinimo(0);
              producto.setEstadoProducto(EstadoProducto.ACTIVO);
              em.persist(producto);
              var th = new TipoHabitacion();
              th.setNombreTipo("Conc_" + token);
              th.setCapacidadMax(2);
              th.setPrecioBase(BigDecimal.TEN);
              em.persist(th);
              var h = new Habitacion();
              h.setTipoHabitacion(th);
              h.setNumeroCuarto("C" + token);
              h.setCapacidadPersonas(2);
              h.setPlazasCama(2);
              h.setMaxMenoresSinCama(0);
              h.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);
              em.persist(h);
              Long creado = null;
              if (em.createQuery(
                          "select count(t) from TipoMovimientoAlmacen t where t.codigo='VENTA'",
                          Long.class)
                      .getSingleResult()
                  == 0) {
                var t = new TipoMovimientoAlmacen();
                t.setCodigo("VENTA");
                t.setNombreMovimiento("Venta prueba " + token);
                t.setSigno(-1);
                em.persist(t);
                creado = t.getIdTipoMovimientoAlmacen();
              }
              em.flush();
              return new Datos(
                  us,
                  es,
                  ps,
                  cs,
                  doc.getIdTipoDocumento(),
                  cat.getIdCategoria(),
                  producto.getIdProducto(),
                  th.getIdTipoHabitacion(),
                  h.getIdHabitacion(),
                  creado);
            });
  }

  @Test
  void dosConfirmacionesNoPuedenVenderLaMismaUltimaUnidad() throws Exception {
    var d = preparar();
    try {
      abrirTurnos(d);
      List<Long> ids = new ArrayList<>();
      for (int i = 0; i < 2; i++)
        ids.add(
            ventas
                .registrar(
                    new VentaRequestDTO(
                        UUID.randomUUID().toString(),
                        d.usuarios().get(i),
                        null,
                        "Prueba",
                        ModalidadCobro.SEPARADA,
                        null,
                        List.of(new VentaLineaRegistroDTO(d.producto(), 1))))
                .idVenta());
      assertThat(
              enParalelo(
                  i -> {
                    try {
                      ventas.confirmar(ids.get(i), new OperacionUsuarioDTO(d.usuarios().get(i)));
                      return true;
                    } catch (ReglaNegocioException e) {
                      assertThat(e).hasMessageContaining("Stock insuficiente");
                      return false;
                    }
                  }))
          .containsExactlyInAnyOrder(true, false);
      tx().executeWithoutResult(
              s -> {
                assertThat(em.find(Producto.class, d.producto()).getStock()).isZero();
                assertThat(
                        em.createQuery(
                                "select count(m) from MovimientoInventario m where"
                                    + " m.producto.idProducto=:id",
                                Long.class)
                            .setParameter("id", d.producto())
                            .getSingleResult())
                    .isEqualTo(1);
              });
    } finally {
      limpiar(d);
    }
  }

  @Test
  void dosOperadoresNoPuedenReservarLaMismaHabitacionEnElMismoPeriodo() throws Exception {
    var d = preparar();
    try {
      var inicio = LocalDateTime.now().plusDays(1);
      var fin = inicio.plusDays(1);
      assertThat(
              enParalelo(
                  i -> {
                    var r =
                        new ReservaRegistroRequestDTO(
                            UUID.randomUUID().toString(),
                            d.clientes().get(i),
                            d.usuarios().get(i),
                            List.of(
                                new ReservaDetalleRegistroDTO(
                                    d.habitacion(),
                                    inicio,
                                    fin,
                                    BigDecimal.TEN,
                                    List.of(
                                        new ReservaOcupanteRegistroDTO(
                                            d.clientes().get(i), null, null, true)))));
                    try {
                      reservas.registrar(r);
                      return true;
                    } catch (ReglaNegocioException e) {
                      assertThat(e).hasMessageContaining("ya tiene una reserva");
                      return false;
                    }
                  }))
          .containsExactlyInAnyOrder(true, false);
      tx().executeWithoutResult(
              s ->
                  assertThat(
                          em.createQuery(
                                  "select count(r) from DetalleReserva r where"
                                      + " r.habitacion.idHabitacion=:id",
                                  Long.class)
                              .setParameter("id", d.habitacion())
                              .getSingleResult())
                      .isEqualTo(1));
    } finally {
      limpiar(d);
    }
  }

  List<Boolean> enParalelo(IntPredicate accion) throws Exception {
    var listos = new CountDownLatch(2);
    var empezar = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> resultados = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        int index = i;
        resultados.add(
            executor.submit(
                () -> {
                  listos.countDown();
                  if (!empezar.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("No se inició la prueba concurrente");
                  return accion.test(index);
                }));
      }
      assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
      empezar.countDown();
      return List.of(
          resultados.get(0).get(30, TimeUnit.SECONDS), resultados.get(1).get(30, TimeUnit.SECONDS));
    } finally {
      empezar.countDown();
    }
  }

  void limpiar(Datos d) {
    tx().executeWithoutResult(
            s -> {
              // Todos los filtros son las claves devueltas al crear esta prueba, nunca datos
              // generales.
              em.createQuery("delete from FlujoCaja f where f.turnoCaja.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from Pago p where p.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from ComprobanteDetalle l where l.comprobante.venta.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from ComprobanteDetalle l where l.comprobante.reserva.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from Comprobante c where c.venta.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from Comprobante c where c.reserva.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              em.createQuery("delete from MovimientoInventario m where m.producto.idProducto=:id")
                  .setParameter("id", d.producto())
                  .executeUpdate();
              em.createQuery("delete from DetalleVenta l where l.venta.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios())
                  .executeUpdate();
              em.createQuery("delete from Venta v where v.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios())
                  .executeUpdate();
              em.createQuery(
                      "delete from ReservaOcupante o where"
                          + " o.detalleReserva.habitacion.idHabitacion=:id")
                  .setParameter("id", d.habitacion())
                  .executeUpdate();
              em.createQuery("delete from DetalleReserva r where r.habitacion.idHabitacion=:id")
                  .setParameter("id", d.habitacion())
                  .executeUpdate();
              em.createQuery("delete from Reserva r where r.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios())
                  .executeUpdate();
              em.remove(em.find(Habitacion.class, d.habitacion()));
              em.remove(em.find(TipoHabitacion.class, d.tipoHabitacion()));
              em.remove(em.find(Producto.class, d.producto()));
              em.remove(em.find(Categoria.class, d.categoria()));
              em.flush();
              em.createQuery("delete from TurnoCaja t where t.usuario.idUsuario in :ids")
                  .setParameter("ids", d.usuarios()).executeUpdate();
              for (Long id : d.usuarios()) em.remove(em.find(Usuario.class, id));
              em.flush();
              for (Long id : d.empleados()) em.remove(em.find(Empleado.class, id));
              em.flush();
              em.createQuery("delete from Huesped h where h.perfil.idPerfil in :ids")
                  .setParameter("ids", d.perfiles()).executeUpdate();
              em.flush();
              for (Long id : d.perfiles()) em.remove(em.find(PersonaNatural.class, id));
              em.flush();
              for (Long id : d.perfiles()) em.remove(em.find(Perfil.class, id));
              em.flush();
              em.remove(em.find(TipoDocumento.class, d.documento()));
              if (d.tipoVentaCreado() != null)
                em.remove(em.find(TipoMovimientoAlmacen.class, d.tipoVentaCreado()));
            });
  }
}
