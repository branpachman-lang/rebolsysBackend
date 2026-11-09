package pe.com.rebolsysBackend.propuesta;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.com.rebolsysBackend.config.MockMvcReglasNegocio;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {"spring.jpa.show-sql=false", "logging.level.pe.com.rebolsysBackend=INFO","app.sesion-prueba.habilitada=false"})
@MockMvcReglasNegocio
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
class PropuestaOracleTest {
  @Autowired EntityManager em;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ReservaService reservas;
  @Autowired HabitacionService habitaciones;
  @Autowired VentaService ventas;
  @Autowired CuentaVentaService cuentasVenta;
  @Autowired GastoService gastos;
  @Autowired ItemService items;
  @Autowired ProductoService productos;
  @Autowired ClienteService clientes;
  @Autowired TipoHabitacionService tiposHabitacion;
  @Autowired TipoItemService tiposItem;
  @Autowired CategoriaService categorias;
  @Autowired CuentaReservaService cuentasReserva;
  @Autowired pe.com.rebolsysBackend.service.impl.AuditoriaOperacionService auditoriaOperacion;
  @Autowired MovimientoInventarioRepository movimientos;
  Long adulto,
      menor,
      usuario,
      turno,
      tipoGasto,
      tipoIngreso,
      tipoHabitacion,
      tipoItem,
      categoria,
      habitacion,
      producto;
  Long formaPago;

  @BeforeEach
  void prepararDatosSoloDentroDeLaTransaccionRevertida() {
    assertThat(
            org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive())
        .isTrue();
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    var td = new TipoDocumento();
    td.setNombreDocumento("PRUEBA_" + sufijo);
    td.setCodigoSunat("1");
    em.persist(td);
    adulto = persona(td, "98765432", LocalDate.of(1990, 1, 1));
    menor = persona(td, "12345678", LocalDate.now().minusYears(4));
    var cliente = em.find(Cliente.class, adulto);
    var empleado = new Empleado();
    empleado.setPersonaNatural(em.find(PersonaNatural.class, cliente.getPerfil().getIdPerfil()));
    empleado.setFechaContratacion(LocalDate.now());
    em.persist(empleado);
    var u = new Usuario();
    u.setEmpleado(empleado);
    u.setUsername("prueba_" + sufijo);
    u.setEstadoUsuario(EstadoUsuario.ACTIVO);
    em.persist(u);
    usuario = u.getIdUsuario();
    var t = new TurnoCaja();
    t.setUsuario(u);
    t.setEstadoSesion(1);
    em.persist(t);
    turno = t.getIdTurno();
    var gasto = new TipoMovimientoCaja();
    gasto.setNombreMovimiento("Gasto " + sufijo);
    gasto.setSigno(-1);
    em.persist(gasto);
    tipoGasto = gasto.getIdTipoMovimiento();
    var ingreso = new TipoMovimientoCaja();
    ingreso.setNombreMovimiento("Ingreso " + sufijo);
    ingreso.setSigno(1);
    em.persist(ingreso);
    tipoIngreso = ingreso.getIdTipoMovimiento();
    for (var par :
        Map.of(
                "AJ_ENT",
                1,
                "AJ_SAL",
                -1,
                "VENTA",
                -1,
                "DEV_VENTA",
                1,
                "ASIG_ITEM",
                0,
                "RET_ITEM",
                0,
                "EST_ITEM",
                0)
            .entrySet()) {
      var existentes =
          em.createQuery(
                  "from TipoMovimientoAlmacen where codigo=:codigo", TipoMovimientoAlmacen.class)
              .setParameter("codigo", par.getKey())
              .getResultList();
      if (existentes.isEmpty()) {
        var m = new TipoMovimientoAlmacen();
        m.setCodigo(par.getKey());
        m.setNombreMovimiento(par.getKey() + " " + sufijo);
        m.setSigno(par.getValue());
        em.persist(m);
      }
    }
    var th = new TipoHabitacion();
    th.setNombreTipo("Prueba " + sufijo);
    th.setCapacidadMax(4);
    th.setPrecioBase(new BigDecimal("80.00"));
    em.persist(th);
    tipoHabitacion = th.getIdTipoHabitacion();
    habitacion = habitaciones.guardar(null, habitacionDTO("T" + sufijo, 3, 2, 1)).idHabitacion();
    var ti = new TipoItem();
    ti.setNombreItem("Prueba " + sufijo);
    em.persist(ti);
    tipoItem = ti.getIdTipoItem();
    var c = new Categoria();
    c.setNombreCategoria("Prueba " + sufijo);
    em.persist(c);
    categoria = c.getIdCategoria();
    producto =
        productos
            .create(
                new ProductoRequestDTO(
                    "Agua " + sufijo,
                    new BigDecimal("3.50"),
                    0,
                    1,
                    categoria,
                    EstadoProducto.ACTIVO))
            .getIdProducto();
    prepararCatalogosCuenta();
    em.flush();
  }

  void prepararCatalogosCuenta() {
    for (var par : Map.of("CUENTA", "CTA", "BOLETA", "BINT", "FACTURA", "FINT").entrySet()) {
      var encontrados =
          em.createQuery("from TipoComprobante where codigo=:codigo", TipoComprobante.class)
              .setParameter("codigo", par.getKey())
              .getResultList();
      TipoComprobante tipo;
      if (encontrados.isEmpty()) {
        tipo = new TipoComprobante();
        tipo.setCodigo(par.getKey());
        tipo.setNombreComprobante(par.getKey());
        em.persist(tipo);
      } else tipo = encontrados.getFirst();
      var id =
          new pe.com.rebolsysBackend.entity.id.SerieComprobanteId(
              tipo.getIdTipoComprobante(), par.getValue());
      if (em.find(SerieComprobante.class, id) == null) {
        var s = new SerieComprobante();
        s.setId(id);
        s.setTipoComprobante(tipo);
        em.persist(s);
      }
    }
    var forma = new FormaPago();
    forma.setNombrePago("Prueba " + UUID.randomUUID().toString().substring(0, 8));
    em.persist(forma);
    formaPago = forma.getIdFormaPago();
    if (em.createQuery("from TipoMovimientoCaja where nombreMovimiento='COBRO_RESERVA'")
        .getResultList()
        .isEmpty()) {
      var m = new TipoMovimientoCaja();
      m.setNombreMovimiento("COBRO_RESERVA");
      m.setSigno(1);
      em.persist(m);
    }
  }

  ReservaPagoRegistroDTO pago(String clave, String monto, Long idVenta) {
    return new ReservaPagoRegistroDTO(
        clave, usuario, turno, formaPago, new BigDecimal(monto), null, idVenta);
  }

  VentaRequestDTO consumo(String clave, Long detalle, ModalidadCobro modo, int cantidad) {
    return new VentaRequestDTO(
        clave,
        usuario,
        adulto,
        null,
        modo,
        detalle,
        List.of(new VentaLineaRegistroDTO(producto, cantidad)));
  }

  Long persona(TipoDocumento tipo, String documento, LocalDate nacimiento) {
    var p = new Perfil();
    p.setTipoDocumento(tipo);
    p.setNumeroDocumento(documento);
    em.persist(p);
    var n = new PersonaNatural();
    n.setPerfil(p);
    n.setNombres("Persona");
    n.setApellidos(documento);
    n.setFechaNacimiento(nacimiento);
    em.persist(n);
    var c = new Cliente();
    c.setPerfil(p);
    em.persist(c);
    return c.getIdCliente();
  }

  HabitacionRequestDTO habitacionDTO(String numero, int capacidad, int camas, int sinCama) {
    return new HabitacionRequestDTO(
        numero, tipoHabitacion, EstadoHabitacion.DISPONIBLE, capacidad, camas, sinCama);
  }

  ReservaRegistroRequestDTO reserva(String clave, boolean incluirMenor) {
    var ocupantes = new ArrayList<ReservaOcupanteRegistroDTO>();
    ocupantes.add(new ReservaOcupanteRegistroDTO(adulto, null, null, true));
    if (incluirMenor)
      ocupantes.add(
          new ReservaOcupanteRegistroDTO(menor, adulto, ParentescoResponsable.MADRE, false));
    return new ReservaRegistroRequestDTO(
        clave,
        adulto,
        usuario,
        List.of(
            new ReservaDetalleRegistroDTO(
                habitacion,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1),
                new BigDecimal("80.00"),
                ocupantes)));
  }

  VentaRequestDTO venta(String clave, int cantidad) {
    return new VentaRequestDTO(
        clave,
        usuario,
        null,
        "Comprador de prueba",
        ModalidadCobro.SEPARADA,
        null,
        List.of(new VentaLineaRegistroDTO(producto, cantidad)));
  }

  void stock(int cantidad) {
    productos.ajustarStock(
        producto,
        new AjusteStockDTO(
            UUID.randomUUID().toString(),
            usuario,
            productos.read(producto).getStock(),
            cantidad,
            "Preparación de prueba"));
  }

  @Test
  void registrarReservaConClienteUnificadoYResponsableEnLaMismaHabitacion() throws Exception {
    var r = reserva("reserva-menor", true);
    var respuesta =
        mvc.perform(
                post("/api/v1/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(r)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.detalles[0].ocupantes.length()").value(2))
            .andReturn();
    Long id = json.readTree(respuesta.getResponse().getContentAsString()).get("idReserva").asLong();
    assertThat(reservas.registrar(r).idReserva()).isEqualTo(id);
    var d = reservas.obtener(id).detalles().getFirst();
    var responsable =
        d.ocupantes().stream().filter(o -> o.idCliente().equals(adulto)).findFirst().orElseThrow();
    assertThat(
            d.ocupantes().stream()
                .filter(o -> o.idCliente().equals(menor))
                .findFirst()
                .orElseThrow()
                .idOcupanteResponsable())
        .isEqualTo(responsable.idReservaOcupante());
    assertThat(
            em.createNativeQuery(
                    "select count(*) from reserva where claveRegistro='reserva-menor'", Long.class)
                .getSingleResult())
        .isEqualTo(1L);
  }

  @Test
  void rechazarReutilizacionDeClaveConDatosDiferentes() {
    var r = reserva("misma-clave", false);
    reservas.registrar(r);
    var distinto = new ReservaRegistroRequestDTO(r.claveRegistro(), menor, usuario, r.detalles());
    assertThatThrownBy(() -> reservas.registrar(distinto))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("otra solicitud");
  }

  @Test
  void rechazarSuperposicionDeReserva() {
    reservas.registrar(reserva("primera", false));
    assertThatThrownBy(() -> reservas.registrar(reserva("segunda", false)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("ya tiene una reserva");
  }

  @Test
  void rechazarMenorSinResponsable() {
    var base = reserva("menor-solo", false).detalles().getFirst();
    var d =
        new ReservaDetalleRegistroDTO(
            habitacion,
            base.fechaIngreso(),
            base.fechaSalida(),
            base.costoEstadia(),
            List.of(new ReservaOcupanteRegistroDTO(menor, null, null, false)));
    assertThatThrownBy(
            () ->
                reservas.registrar(
                    new ReservaRegistroRequestDTO("menor-solo", adulto, usuario, List.of(d))))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("responsable");
  }

  @Test
  void estadoDeHabitacionesEsSoloLecturaYDistingueReservaDeOcupacion() {
    long antes =
        em.createQuery("select count(d) from DetalleReserva d", Long.class).getSingleResult();
    var r = reservas.registrar(reserva("estados", true));
    var estados = habitaciones.estado(LocalDate.now(), LocalDate.now().plusDays(2));
    var estado =
        estados.stream().filter(h -> h.idHabitacion().equals(habitacion)).findFirst().orElseThrow();
    assertThat(estado.ocupadaActualmente()).isFalse();
    assertThat(estado.disponible()).isFalse();
    assertThat(em.find(Habitacion.class, habitacion).getEstadoHabitacion())
        .isEqualTo(EstadoHabitacion.DISPONIBLE);
    var id = r.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(id, "ingreso", new OperacionUsuarioDTO(usuario));
    assertThat(
            habitaciones.estado(LocalDate.now(), LocalDate.now().plusDays(2)).stream()
                .filter(h -> h.idHabitacion().equals(habitacion))
                .findFirst()
                .orElseThrow()
                .ocupadaActualmente())
        .isTrue();
    cuentasReserva.pagar(r.idReserva(), pago("liquidar-estados", "80.00", null));
    reservas.cambiarEstado(id, "salida", new OperacionUsuarioDTO(usuario));
    assertThat(em.find(Habitacion.class, habitacion).getEstadoHabitacion())
        .isEqualTo(EstadoHabitacion.LIMPIEZA);
    assertThat(
            em.createQuery("select count(d) from DetalleReserva d", Long.class).getSingleResult())
        .isEqualTo(antes + 1);
  }

  @Test
  void gastosSeRegistranComoSalidaSinPago() throws Exception {
    var r =
        new GastoRequestDTO(
            usuario,
            turno,
            tipoGasto,
            "Material de limpieza",
            new BigDecimal("12.50"),
            "B001-1",
            null,
            null);
    var resultado =
        mvc.perform(
                post("/api/v1/gastos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(r)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.monto").value(12.5))
            .andReturn();
    Long id = json.readTree(resultado.getResponse().getContentAsString()).get("idFlujo").asLong();
    assertThat(em.find(FlujoCaja.class, id).getPago()).isNull();
    assertThat(gastos.listar(turno)).hasSize(1);
  }

  @Test
  void historialDeGastosExcluyeCobrosYDevolucionesDeReservas() {
    var r = reservas.registrar(reserva("gasto-no-cobro", false));
    var cuenta = cuentasReserva.pagar(r.idReserva(), pago("ingreso-no-gasto", "20", null));
    var p = em.createQuery("from Pago where comprobante.idComprobante=:id", Pago.class)
        .setParameter("id", cuenta.cuentas().getFirst().idCuenta()).getSingleResult();
    var devolucion = new FlujoCaja();
    devolucion.setTurnoCaja(em.find(TurnoCaja.class, turno));
    devolucion.setTipoMovimientoCaja(em.find(TipoMovimientoCaja.class, tipoGasto));
    devolucion.setPago(p);
    devolucion.setConcepto("Devolución vinculada al pago; no es gasto operativo");
    devolucion.setMonto(new BigDecimal("5"));
    em.persist(devolucion);
    gastos.registrar(new GastoRequestDTO(usuario, turno, tipoGasto, "Taxi operativo",
        new BigDecimal("10"), null, null, null, UUID.randomUUID().toString()));
    var h = gastos.historial(new GastoFiltroDTO(turno, null, null, null, null, null, null, null, null, null));
    assertThat(h.resumen().cantidadGastos()).isEqualTo(1);
    assertThat(h.resumen().totalGastado()).isEqualByComparingTo("10");
    assertThat(gastos.listar(turno)).hasSize(1);
    assertThatThrownBy(() -> gastos.obtener(devolucion.getIdFlujo()))
        .isInstanceOf(pe.com.rebolsysBackend.exception.RecursoNoEncontradoException.class);
  }

  @Test
  void rechazarGastoConTurnoCerrado() {
    var t = em.find(TurnoCaja.class, turno);
    t.setEstadoSesion(0);
    t.setFechaCierre(LocalDateTime.now());
    em.flush();
    assertThatThrownBy(
            () ->
                gastos.registrar(
                    new GastoRequestDTO(
                        usuario,
                        turno,
                        tipoGasto,
                        "No permitido",
                        BigDecimal.ONE,
                        null,
                        null,
                        null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("abierto");
  }

  @Test
  void rechazarGastoConTipoDeIngreso() {
    assertThatThrownBy(
            () ->
                gastos.registrar(
                    new GastoRequestDTO(
                        usuario,
                        turno,
                        tipoIngreso,
                        "No permitido",
                        BigDecimal.ONE,
                        null,
                        null,
                        null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("salida de caja");
  }

  @Test
  void ventaEnBorradorNoMueveStockYConfirmacionYAnulacionSonIdempotentes() throws Exception {
    stock(10);
    var r = venta("venta-idempotente", 3);
    var v = ventas.registrar(r);
    assertThat(ventas.registrar(r).idVenta()).isEqualTo(v.idVenta());
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    mvc.perform(
            post("/api/v1/ventas/" + v.idVenta() + "/confirmacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new OperacionUsuarioDTO(usuario))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoVenta").value("CONFIRMADA"))
        .andExpect(jsonPath("$.total").value(10.5));
    ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario));
    assertThat(productos.read(producto).getStock()).isEqualTo(7);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto)).hasSize(2);
    em.flush();
    em.clear();
    var linea = em.find(DetalleVenta.class, v.detalles().getFirst().idDetalleVenta());
    assertThat(linea.getTotal()).isEqualByComparingTo("10.50");
    ventas.anular(v.idVenta(), new OperacionUsuarioDTO(usuario));
    ventas.anular(v.idVenta(), new OperacionUsuarioDTO(usuario));
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto)).hasSize(3);
  }

  @Test
  void rechazarVentaSinStockSinGuardarMovimientosParciales() {
    var v = ventas.registrar(venta("sin-stock", 1));
    assertThatThrownBy(() -> ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("Stock insuficiente");
    assertThat(productos.read(producto).getStock()).isZero();
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto)).isEmpty();
  }

  @Test
  void ventaConCargoAReservaRequiereEstanciaEnCurso() {
    var r = reservas.registrar(reserva("venta-reserva", false));
    var d = r.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(d, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(2);
    var v =
        ventas.registrar(
            new VentaRequestDTO(
                "cargo-reserva",
                usuario,
                adulto,
                null,
                ModalidadCobro.RESERVA,
                d,
                List.of(new VentaLineaRegistroDTO(producto, 1))));
    assertThat(ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario)).idDetalleReserva())
        .isEqualTo(d);
  }

  @Test
  void noReducirCapacidadPorDebajoDeReservaActiva() {
    reservas.registrar(reserva("capacidad", true));
    var numero = habitaciones.obtener(habitacion).numeroCuarto();
    assertThatThrownBy(() -> habitaciones.guardar(habitacion, habitacionDTO(numero, 1, 1, 0)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("ocupantes");
  }

  @Test
  void configurarHabitacionPermiteCrearYEditar() throws Exception {
    var h = habitaciones.obtener(habitacion);
    var r = habitacionDTO(h.numeroCuarto(), 4, 3, 1);
    mvc.perform(
            put("/api/v1/habitaciones/" + habitacion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(r)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capacidadPersonas").value(4));
  }

  @Test
  void mobiliarioConservaAsignacionesAnterioresYRegistraTrasladosYRetiros() {
    var i = items.guardar(null, new ItemRequestDTO("Televisor", tipoItem, EstadoItem.ACTIVO));
    var a =
        new ItemAsignacionDTO(
            "asignar", usuario, habitacion, EstadoConservacion.BUENO, "Instalación");
    var asignado = items.asignar(i.idItem(), a);
    items.asignar(i.idItem(), a);
    assertThat(items.movimientos(i.idItem())).hasSize(1);
    assertThat(asignado.idHabitacion()).isEqualTo(habitacion);
    var h2 = habitaciones.guardar(null, habitacionDTO("OTRA", 2, 2, 0));
    var trasladado =
        items.asignar(
            i.idItem(),
            new ItemAsignacionDTO(
                "trasladar", usuario, h2.idHabitacion(), EstadoConservacion.BUENO, "Reubicación"));
    assertThat(trasladado.idDetalleHabitacion()).isNotEqualTo(asignado.idDetalleHabitacion());
    assertThat(
            em.find(DetalleHabitacion.class, asignado.idDetalleHabitacion())
                .getHabitacion()
                .getIdHabitacion())
        .isEqualTo(habitacion);
    var retirado =
        items.asignar(
            i.idItem(),
            new ItemAsignacionDTO(
                "retirar", usuario, null, EstadoConservacion.EN_REPARACION, "Revisión"));
    assertThat(retirado.estadoItem()).isEqualTo(EstadoItem.ACTIVO);
    assertThat(retirado.idHabitacion()).isNull();
    assertThat(items.movimientos(i.idItem())).hasSize(3);
  }

  @Test
  void ajustesDeStockRequierenSaldoActualYSonIdempotentes() {
    var a = new AjusteStockDTO("ajuste", usuario, 0, 5, "Carga inicial");
    productos.ajustarStock(producto, a);
    productos.ajustarStock(producto, a);
    assertThat(productos.read(producto).getStock()).isEqualTo(5);
    assertThat(productos.movimientos(producto)).hasSize(1);
    assertThatThrownBy(
            () ->
                productos.ajustarStock(
                    producto, new AjusteStockDTO("otro", usuario, 0, 2, "Saldo antiguo")))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("stock cambió");
  }

  @Test
  void clienteMenorEsValidoYConservaRestriccionesMedicas() {
    var c = clientes.obtener(menor);
    var guardado =
        clientes.guardar(menor, new ClienteGuardarDTO(c.persona(), "Alergia a penicilina"));
    assertThat(guardado.restriccionesMedicas()).isEqualTo("Alergia a penicilina");
  }

  @Test
  void validarCamposAntesDeEscribirYPermitirPruebasSinAutenticacion() throws Exception {
    mvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.idCliente").exists());
    mvc.perform(
            get("/api/v1/items")
                .with(
                    r -> {
                      r.setRemoteAddr("192.0.2.10");
                      return r;
                    }))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/ventas").param("pagina", "-1")).andExpect(status().isBadRequest());
  }

  @Test
  void crudHabitacionEliminaSoloSiNoTieneHistorial() throws Exception {
    var nueva = habitaciones.create(habitacionDTO("LIBRE", 2, 2, 0));
    mvc.perform(delete("/api/v1/habitaciones/" + nueva.idHabitacion()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/habitaciones/" + nueva.idHabitacion()))
        .andExpect(status().isNotFound());
    reservas.registrar(reserva("con-historial", false));
    mvc.perform(delete("/api/v1/habitaciones/" + habitacion)).andExpect(status().isConflict());
    assertThat(habitaciones.read(habitacion)).isNotNull();
  }

  @Test
  void mobiliarioPorHabitacionMuestraSoloAsignacionActualYLaBajaConservaHistorial()
      throws Exception {
    var i = items.create(new ItemRequestDTO("Silla", tipoItem, EstadoItem.ACTIVO));
    items.asignar(
        i.idItem(),
        new ItemAsignacionDTO(
            "asig", usuario, habitacion, EstadoConservacion.BUENO, "Instalación"));
    mvc.perform(get("/api/v1/habitaciones/" + habitacion + "/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].idItem").value(i.idItem()));
    mvc.perform(delete("/api/v1/items/" + i.idItem())).andExpect(status().isConflict());
    var h2 = habitaciones.create(habitacionDTO("NUEVA", 2, 2, 0));
    items.asignar(
        i.idItem(),
        new ItemAsignacionDTO(
            "mover", usuario, h2.idHabitacion(), EstadoConservacion.BUENO, "Traslado"));
    assertThat(habitaciones.mobiliario(habitacion)).isEmpty();
    assertThat(habitaciones.mobiliario(h2.idHabitacion())).hasSize(1);
    items.asignar(
        i.idItem(),
        new ItemAsignacionDTO("retiro", usuario, null, EstadoConservacion.BUENO, "Almacén"));
    mvc.perform(delete("/api/v1/items/" + i.idItem())).andExpect(status().isNoContent());
    assertThat(items.read(i.idItem()).estadoItem()).isEqualTo(EstadoItem.INACTIVO);
    assertThat(items.movimientos(i.idItem())).hasSize(3);
    assertThat(habitaciones.mobiliario(h2.idHabitacion())).isEmpty();
  }

  @Test
  void configuracionTiposYCategoriasRespetaReferenciasYCapacidades() throws Exception {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    var tipo =
        tiposHabitacion.create(
            new TipoHabitacionRequestDTO("Suite " + sufijo, 4, new BigDecimal("120.00")));
    var h =
        habitaciones.create(
            new HabitacionRequestDTO(
                "SUITE", tipo.idTipoHabitacion(), EstadoHabitacion.DISPONIBLE, 4, 4, 0));
    assertThatThrownBy(
            () ->
                tiposHabitacion.update(
                    tipo.idTipoHabitacion(),
                    new TipoHabitacionRequestDTO("Suite " + sufijo, 2, new BigDecimal("120.00"))))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("capacidad");
    assertThatThrownBy(() -> tiposHabitacion.delete(tipo.idTipoHabitacion()))
        .isInstanceOf(ReglaNegocioException.class);
    habitaciones.delete(h.idHabitacion());
    tiposHabitacion.delete(tipo.idTipoHabitacion());
    var ti = tiposItem.create(new TipoItemRequestDTO("Escritorio " + sufijo));
    tiposItem.update(ti.idTipoItem(), new TipoItemRequestDTO("Mesa " + sufijo));
    mvc.perform(delete("/api/v1/tipos-item/" + ti.idTipoItem())).andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/tipos-item/" + tipoItem)).andExpect(status().isNoContent());
    var cat = categorias.create(new CategoriaRequestDTO("Snacks " + sufijo));
    mvc.perform(
            put("/api/v1/categorias/" + cat.getIdCategoria())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreCategoria\":\"Bocaditos " + sufijo + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreCategoria").value("Bocaditos " + sufijo));
    mvc.perform(delete("/api/v1/categorias/" + categoria)).andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/categorias/" + cat.getIdCategoria()))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/v1/tipos-habitacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreTipo\":\"Incorrecto\",\"capacidadMax\":0,\"precioBase\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reservaSinOcupantesSeCompletaAntesDelIngresoYConsultaNoCreaCuenta() throws Exception {
    var base = reserva("sin-ocupantes", false);
    var d = base.detalles().getFirst();
    var r =
        reservas.registrar(
            new ReservaRegistroRequestDTO(
                base.claveRegistro(),
                adulto,
                usuario,
                List.of(
                    new ReservaDetalleRegistroDTO(
                        habitacion, d.fechaIngreso(), d.fechaSalida(), d.costoEstadia(), null))));
    var id = r.detalles().getFirst().idDetalleReserva();
    assertThat(r.detalles().getFirst().ocupantes()).isEmpty();
    mvc.perform(get("/api/v1/reservas/" + r.idReserva() + "/cuenta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoPendiente").value(80));
    assertThat(
            em.createQuery("from Comprobante where reserva.idReserva=:id")
                .setParameter("id", r.idReserva())
                .getResultList())
        .isEmpty();
    mvc.perform(
            post("/api/v1/reservas/detalles/" + id + "/ingreso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new OperacionUsuarioDTO(usuario))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Completa los ocupantes antes del ingreso"));
    var ocupantes = reserva("datos", true).detalles().getFirst().ocupantes();
    mvc.perform(
            put("/api/v1/reservas/detalles/" + id + "/ocupantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(new ReservaOcupantesGuardarDTO(usuario, ocupantes))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ocupantes.length()").value(2));
    // Reemplazar una lista con responsables no deja referencias a IDs eliminados.
    reservas.guardarOcupantes(id, new ReservaOcupantesGuardarDTO(usuario, ocupantes));
    reservas.cambiarEstado(id, "ingreso", new OperacionUsuarioDTO(usuario));
    assertThat(cuentasReserva.cuenta(r.idReserva()).saldoPendiente()).isEqualByComparingTo("80");
    mvc.perform(
            put("/api/v1/reservas/detalles/" + id + "/ocupantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(new ReservaOcupantesGuardarDTO(usuario, ocupantes))))
        .andExpect(status().isConflict());
  }

  @Test
  void flujoCompletoConAnticipoConsumosMixtosComprobanteSeparadoYPagoAlSalir() {
    var r = reservas.registrar(reserva("flujo-completo", true));
    Long id = r.idReserva(), detalle = r.detalles().getFirst().idDetalleReserva();
    cuentasReserva.pagar(id, pago("anticipo", "20", null));
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(10);
    var cargo = cuentasReserva.consumir(id, consumo("cargo", detalle, ModalidadCobro.RESERVA, 2));
    var separado =
        cuentasReserva.consumir(id, consumo("aparte", detalle, ModalidadCobro.SEPARADA, 1));
    var boleta =
        cuentasReserva.emitir(
            id,
            new ReservaComprobanteRegistroDTO(
                usuario, turno, "BOLETA", adulto, separado.venta().idVenta()));
    assertThat(boleta.estado()).isEqualTo(EstadoComprobante.EMITIDO);
    assertThat(boleta.saldo()).isEqualByComparingTo("3.50");
    cuentasReserva.pagar(id, pago("pago-aparte", "3.50", separado.venta().idVenta()));
    assertThat(cuentasReserva.cuenta(id).saldoPendiente()).isEqualByComparingTo("67");
    assertThatThrownBy(
            () -> reservas.cambiarEstado(detalle, "salida", new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("saldo");
    cuentasReserva.pagar(id, pago("liquidacion", "67", null));
    var cuenta = cuentasReserva.cuenta(id);
    assertThat(cuenta.totalCargos()).isEqualByComparingTo("90.50");
    assertThat(cuenta.totalPagado()).isEqualByComparingTo("90.50");
    assertThat(cuenta.saldoPendiente()).isZero();
    assertThat(cuentasReserva.consumos(id)).hasSize(2);
    assertThat(cargo.idCuenta()).isNotEqualTo(separado.idCuenta());
    assertThat(separado.numeroReserva()).isEqualTo("R" + id);
    reservas.cambiarEstado(detalle, "salida", new OperacionUsuarioDTO(usuario));
    assertThat(em.find(Habitacion.class, habitacion).getEstadoHabitacion())
        .isEqualTo(EstadoHabitacion.LIMPIEZA);
  }

  @Test
  void reservaPagadaExigeConsumoSeparadoYBloqueaSalidaHastaPagarlo() {
    var r = reservas.registrar(reserva("pagada", false));
    Long id = r.idReserva(), detalle = r.detalles().getFirst().idDetalleReserva();
    cuentasReserva.pagar(id, pago("alojamiento", "80", null));
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(3);
    assertThatThrownBy(
            () ->
                cuentasReserva.consumir(
                    id, consumo("rechazado", detalle, ModalidadCobro.RESERVA, 1)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("SEPARADA");
    var c = cuentasReserva.consumir(id, consumo("separado", detalle, ModalidadCobro.SEPARADA, 1));
    assertThat(cuentasReserva.cuenta(id).cuentas()).hasSize(2);
    assertThatThrownBy(
            () -> reservas.cambiarEstado(detalle, "salida", new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class);
    cuentasReserva.pagar(id, pago("pagar-separado", "3.50", c.venta().idVenta()));
    reservas.cambiarEstado(detalle, "salida", new OperacionUsuarioDTO(usuario));
  }

  @Test
  void comprobanteEmitidoFijaContenidoPeroPermiteCobrarSaldo() {
    var r = reservas.registrar(reserva("emitida", false));
    Long id = r.idReserva(), detalle = r.detalles().getFirst().idDetalleReserva();
    var request = new ReservaComprobanteRegistroDTO(usuario, turno, "BOLETA", adulto, null);
    var doc = cuentasReserva.emitir(id, request);
    var repetido = cuentasReserva.emitir(id, request);
    assertThat(repetido.idCuenta()).isEqualTo(doc.idCuenta());
    assertThat(repetido.numero()).isEqualTo(doc.numero());
    em.flush();
    em.clear();
    assertThat(em.find(Comprobante.class, doc.idCuenta()).getFechaEmision()).isNotNull();
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(2);
    assertThatThrownBy(
            () ->
                cuentasReserva.consumir(
                    id, consumo("no-consolidar", detalle, ModalidadCobro.RESERVA, 1)))
        .isInstanceOf(ReglaNegocioException.class);
    assertThatThrownBy(
            () ->
                cuentasReserva.emitir(
                    id, new ReservaComprobanteRegistroDTO(usuario, turno, "FACTURA", adulto, null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("otro tipo");
    cuentasReserva.pagar(id, pago("pago-despues-emision", "80", null));
    assertThat(cuentasReserva.cuenta(id).saldoPendiente()).isZero();
  }

  @Test
  void pagosSonIdempotentesRechazanSobrepagoYRegistranUnaSolaEntradaDeCaja() {
    var r = reservas.registrar(reserva("pagos", false));
    var request = pago("pago-unico", "30.00", null);
    cuentasReserva.pagar(r.idReserva(), request);
    cuentasReserva.pagar(r.idReserva(), pago("pago-unico", "30", null));
    assertThat(cuentasReserva.cuenta(r.idReserva()).totalPagado()).isEqualByComparingTo("30");
    assertThat(
            em.createQuery(
                    "select count(f) from FlujoCaja f where f.pago.claveIdempotencia='pago-unico'",
                    Long.class)
                .getSingleResult())
        .isEqualTo(1);
    assertThatThrownBy(() -> cuentasReserva.pagar(r.idReserva(), pago("pago-unico", "40", null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("otra solicitud");
    assertThatThrownBy(() -> cuentasReserva.pagar(r.idReserva(), pago("exceso", "51", null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("supera");
    assertThat(cuentasReserva.cuenta(r.idReserva()).saldoPendiente()).isEqualByComparingTo("50");
  }

  @Test
  void consumoRepetidoNoDuplicaStockCuentaNiImporteYNoSeAnulaPorVentas() {
    var r = reservas.registrar(reserva("consumo-repetido", false));
    var detalle = r.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(5);
    var req = consumo("una-vez", detalle, ModalidadCobro.RESERVA, 2);
    var primero = cuentasReserva.consumir(r.idReserva(), req);
    var segundo = cuentasReserva.consumir(r.idReserva(), req);
    assertThat(segundo.venta().idVenta()).isEqualTo(primero.venta().idVenta());
    assertThat(productos.read(producto).getStock()).isEqualTo(3);
    assertThat(cuentasReserva.cuenta(r.idReserva()).totalCargos()).isEqualByComparingTo("87");
    assertThat(cuentasReserva.cuenta(r.idReserva()).cuentas().getFirst().lineas()).hasSize(2);
    assertThatThrownBy(
            () -> ventas.anular(primero.venta().idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void titularJuridicoPuedeReservarSinSerOcupanteYRecibirFacturaInterna() {
    var td = new TipoDocumento();
    td.setNombreDocumento("RUC_PRUEBA_" + UUID.randomUUID().toString().substring(0, 6));
    td.setCodigoSunat("6");
    em.persist(td);
    var perfil = new Perfil();
    perfil.setTipoDocumento(td);
    perfil.setNumeroDocumento("20123456789");
    em.persist(perfil);
    var pj = new PersonaJuridica();
    pj.setPerfil(perfil);
    pj.setRazonSocial("Empresa de pruebas");
    em.persist(pj);
    var empresa = new Cliente();
    empresa.setPerfil(perfil);
    em.persist(empresa);
    var b = reserva("empresa", false);
    var r =
        reservas.registrar(
            new ReservaRegistroRequestDTO(
                b.claveRegistro(), empresa.getIdCliente(), usuario, b.detalles()));
    var doc =
        cuentasReserva.emitir(
            r.idReserva(),
            new ReservaComprobanteRegistroDTO(
                usuario, turno, "FACTURA", empresa.getIdCliente(), null));
    assertThat(doc.tipo()).isEqualTo("FACTURA");
    assertThat(doc.serie()).isEqualTo("FINT");
    assertThat(doc.idClienteReceptor()).isEqualTo(empresa.getIdCliente());
    assertThat(r.detalles().getFirst().ocupantes().getFirst().idCliente()).isEqualTo(adulto);
  }

  @Test
  void facturaInternaRequiereReceptorConRuc() {
    var r = reservas.registrar(reserva("factura-sin-ruc", false));
    assertThatThrownBy(
            () ->
                cuentasReserva.emitir(
                    r.idReserva(),
                    new ReservaComprobanteRegistroDTO(usuario, turno, "FACTURA", adulto, null)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("RUC");
  }

  @Test
  void anularSinPagosLiberaHabitacionYAnticipoExigeFlujoDeDevolucion() {
    var r = reservas.registrar(reserva("sin-pagos", false));
    reservas.cambiarEstado(
        r.detalles().getFirst().idDetalleReserva(),
        "cancelacion",
        new OperacionUsuarioDTO(usuario));
    assertThat(cuentasReserva.cuenta(r.idReserva()).totalAlojamiento()).isZero();
    var r2 = reservas.registrar(reserva("con-anticipo", false));
    cuentasReserva.pagar(r2.idReserva(), pago("anticipo-anulacion", "10", null));
    assertThatThrownBy(
            () ->
                reservas.cambiarEstado(
                    r2.detalles().getFirst().idDetalleReserva(),
                    "cancelacion",
                    new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("devolución");
    assertThat(cuentasReserva.cuenta(r2.idReserva()).totalPagado()).isEqualByComparingTo("10");
  }

  @Test
  void pagoConTurnoCerradoEsRechazado() {
    var r = reservas.registrar(reserva("cuenta-segura", false));
    var req = pago("turno-cerrado", "10", null);
    em.find(TurnoCaja.class, turno).setEstadoSesion(0);
    em.flush();
    assertThatThrownBy(() -> cuentasReserva.pagar(r.idReserva(), req))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("abierto");
  }

  @Test
  void validarResponsableDelMenorAlCompletarAntesDelIngreso() {
    var r = reservas.registrar(reserva("validar-menor", false));
    var id = r.detalles().getFirst().idDetalleReserva();
    assertThatThrownBy(
            () ->
                reservas.guardarOcupantes(
                    id,
                    new ReservaOcupantesGuardarDTO(
                        usuario,
                        List.of(new ReservaOcupanteRegistroDTO(menor, null, null, false)))))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("responsable");
  }

  @Test
  void desdeSieteAniosSeNecesitaPlazaDeCama() {
    var c = em.find(Cliente.class, menor);
    em.find(PersonaNatural.class, c.getPerfil().getIdPerfil())
        .setFechaNacimiento(LocalDate.now().minusYears(7));
    em.flush();
    assertThatThrownBy(() -> reservas.registrar(reserva("menor-con-cama", true)))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("menor de siete años");
  }

  @Test
  void dosAdultosYUnMenorCompartenDosPlazasSinAumentarCamas() {
    var tipo=em.find(Cliente.class,adulto).getPerfil().getTipoDocumento();
    var segundo=persona(tipo,"87654321",LocalDate.of(1992,1,1));
    var base=reserva("familia-dos-plazas",false).detalles().getFirst();
    var r=reservas.registrar(new ReservaRegistroRequestDTO("familia-dos-plazas",adulto,usuario,List.of(new ReservaDetalleRegistroDTO(habitacion,base.fechaIngreso(),base.fechaSalida(),base.costoEstadia(),List.of(
      new ReservaOcupanteRegistroDTO(adulto,null,null,true),
      new ReservaOcupanteRegistroDTO(segundo,null,null,true),
      new ReservaOcupanteRegistroDTO(menor,adulto,ParentescoResponsable.MADRE,false))))));
    assertThat(r.detalles().getFirst().ocupantes()).hasSize(3);
    assertThat(r.detalles().getFirst().ocupantes().stream().filter(ReservaOcupanteRespuestaDTO::ocupaPlazaCama).count()).isEqualTo(2);
    assertThat(em.find(Habitacion.class,habitacion).getPlazasCama()).isEqualTo(2);
  }

  @Test
  void dosMenoresSinCamaSuperanElCupoAdicional() {
    var tipo=em.find(Cliente.class,adulto).getPerfil().getTipoDocumento();
    var segundoMenor=persona(tipo,"87654320",LocalDate.now().minusYears(3));
    var base=reserva("dos-menores-sin-cama",false).detalles().getFirst();
    var solicitud=new ReservaRegistroRequestDTO("dos-menores-sin-cama",adulto,usuario,List.of(new ReservaDetalleRegistroDTO(habitacion,base.fechaIngreso(),base.fechaSalida(),base.costoEstadia(),List.of(
      new ReservaOcupanteRegistroDTO(adulto,null,null,true),
      new ReservaOcupanteRegistroDTO(menor,adulto,ParentescoResponsable.MADRE,false),
      new ReservaOcupanteRegistroDTO(segundoMenor,adulto,ParentescoResponsable.MADRE,false)))));
    assertThatThrownBy(()->reservas.registrar(solicitud)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("límite de menores");
  }

  @Test
  void configuracionNoPermiteMasDeUnMenorAdicional() {
    assertThatThrownBy(()->habitaciones.update(habitacion,habitacionDTO(em.find(Habitacion.class,habitacion).getNumeroCuarto(),4,2,2)))
      .isInstanceOf(jakarta.validation.ConstraintViolationException.class).hasMessageContaining("un menor adicional");
  }

  @Test
  void ocuparVariasHabitacionesNoDuplicaPersonasNiPermiteResponsableEnOtraHabitacion() {
    var h2 = habitaciones.create(habitacionDTO("OTRA_HAB", 3, 2, 1));
    var base = reserva("varias", false).detalles().getFirst();
    var r =
        reservas.registrar(
            new ReservaRegistroRequestDTO(
                "varias",
                adulto,
                usuario,
                List.of(
                    base,
                    new ReservaDetalleRegistroDTO(
                        h2.idHabitacion(),
                        base.fechaIngreso(),
                        base.fechaSalida(),
                        base.costoEstadia(),
                        List.of()))));
    var segunda = r.detalles().get(1).idDetalleReserva();
    assertThatThrownBy(
            () ->
                reservas.guardarOcupantes(
                    segunda,
                    new ReservaOcupantesGuardarDTO(
                        usuario,
                        List.of(new ReservaOcupanteRegistroDTO(adulto, null, null, true)))))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("otra estancia");
    assertThatThrownBy(
            () ->
                reservas.guardarOcupantes(
                    segunda,
                    new ReservaOcupantesGuardarDTO(
                        usuario,
                        List.of(
                            new ReservaOcupanteRegistroDTO(
                                menor, adulto, ParentescoResponsable.PADRE, false)))))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("misma habitación");
  }

  @Test
  void unaReservaNoPuedePagarNiFacturarElConsumoDeOtra() {
    var r = reservas.registrar(reserva("origen", false));
    var d = r.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(d, "ingreso", new OperacionUsuarioDTO(usuario));
    stock(2);
    var c =
        cuentasReserva.consumir(r.idReserva(), consumo("de-otra", d, ModalidadCobro.SEPARADA, 1));
    var h2 = habitaciones.create(habitacionDTO("DISTINTA", 3, 2, 1));
    var origen = reserva("destino", false).detalles().getFirst();
    var otra =
        reservas.registrar(
            new ReservaRegistroRequestDTO(
                "destino",
                adulto,
                usuario,
                List.of(
                    new ReservaDetalleRegistroDTO(
                        h2.idHabitacion(),
                        origen.fechaIngreso(),
                        origen.fechaSalida(),
                        origen.costoEstadia(),
                        List.of()))));
    assertThatThrownBy(
            () ->
                cuentasReserva.pagar(
                    otra.idReserva(), pago("cruzado", "3.50", c.venta().idVenta())))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("no pertenece");
    assertThatThrownBy(
            () ->
                cuentasReserva.emitir(
                    otra.idReserva(),
                    new ReservaComprobanteRegistroDTO(
                        usuario, turno, "BOLETA", adulto, c.venta().idVenta())))
        .isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("no pertenece");
  }
  Long productoAdicional(String precio, int existencias) {
    var p = productos.create(new ProductoRequestDTO("Producto " + UUID.randomUUID().toString().substring(0, 8),
        new BigDecimal(precio), 0, 0, categoria, EstadoProducto.ACTIVO));
    if (existencias > 0) productos.ajustarStock(p.getIdProducto(), new AjusteStockDTO(
        UUID.randomUUID().toString(), usuario, 0, existencias, "Preparación de prueba"));
    return p.getIdProducto();
  }

  @Test
  void auditoriaDeCambiosConservaElUsuarioYRegistroAfectado() {
    auditoriaOperacion.registrar(new SesionActualDTO(usuario,"operador de prueba","PRUEBA",turno,formaPago),"HABITACION",habitacion,"PUT","/api/v1/habitaciones/"+habitacion,"127.0.0.1");
    var a=em.createQuery("select a from AuditoriaGeneral a where a.usuario.idUsuario=:usuario and a.nombreTabla='HABITACION'",AuditoriaGeneral.class).setParameter("usuario",usuario).getSingleResult();
    assertThat(a.getIdRegistro()).isEqualTo(habitacion);assertThat(a.getUsuario().getIdUsuario()).isEqualTo(usuario);assertThat(a.getAccion()).isEqualTo(AccionAuditoria.UPDATE);assertThat(a.getDatosNuevos()).contains("PUT /api/v1/habitaciones/");assertThat(a.getFechaHora()).isNotNull();
  }

  @Test
  void ventaPagadaConUsuarioYReintentoNoDuplicaCobroNiStock() {
    stock(10);
    var req=new VentaRequestDTO("venta-sesion-"+UUID.randomUUID(),usuario,null,"Comprador de prueba",ModalidadCobro.SEPARADA,null,List.of(new VentaLineaRegistroDTO(producto,2,new BigDecimal("3.50"))));
    var v=ventas.registrarConfirmada(req);var pago=new PagoCuentaDTO("pago-sesion-"+UUID.randomUUID(),usuario,turno,formaPago,new BigDecimal("7.00"),null);
    var c=cuentasVenta.pagar(v.idVenta(),pago);assertThat(c.saldo()).isEqualByComparingTo("0");assertThat(v.idUsuario()).isEqualTo(usuario);
    assertThat(ventas.registrarConfirmada(req).idVenta()).isEqualTo(v.idVenta());
    assertThat(cuentasVenta.pagar(v.idVenta(),pago).pagos()).hasSize(1);assertThat(productos.read(producto).getStock()).isEqualTo(8);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto).getFirst().getUsuario().getIdUsuario()).isEqualTo(usuario);
  }

  @Test
  void precioModificadoNoConfirmaUnaVentaConOtroImporte() {
    stock(10);var req=new VentaRequestDTO("precio-sesion-"+UUID.randomUUID(),usuario,null,null,ModalidadCobro.SEPARADA,null,List.of(new VentaLineaRegistroDTO(producto,1,BigDecimal.ONE)));
    assertThatThrownBy(()->ventas.registrarConfirmada(req)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cambió");assertThat(productos.read(producto).getStock()).isEqualTo(10);
  }

  @Test
  void altaDeProductoGuardaDescripcionYExistenciasConMovimientoInicial() {
    var req=new ProductoRequestDTO("Alta inicial "+UUID.randomUUID().toString().substring(0,8),new BigDecimal("3.50"),50,2,categoria,EstadoProducto.ACTIVO);
    req.setIdUsuario(usuario);req.setDescripcion("  Agua sin gas de 500 ml  ");
    var p=productos.create(req);
    assertThat(p.getStock()).isEqualTo(50);assertThat(p.getDescripcion()).isEqualTo("Agua sin gas de 500 ml");
    var historial=productos.movimientos(p.getIdProducto());assertThat(historial).hasSize(1);
    assertThat(historial.getFirst().cantidad()).isEqualTo(50);assertThat(historial.getFirst().stockResultante()).isEqualTo(50);
    assertThat(historial.getFirst().idUsuario()).isEqualTo(usuario);assertThat(historial.getFirst().observacion()).isEqualTo("Existencias iniciales al registrar el producto");
    assertThat(productos.read(p.getIdProducto()).getDescripcion()).isEqualTo(p.getDescripcion());
  }

  @Test
  void altaConExistenciasExigeResponsableYDescripcionAcotada() {
    var req=new ProductoRequestDTO("Alta inválida",BigDecimal.ONE,50,0,categoria,EstadoProducto.ACTIVO);
    assertThatThrownBy(()->productos.create(req)).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("responsable");
    req.setIdUsuario(usuario);req.setDescripcion("x".repeat(501));
    assertThatThrownBy(()->productos.create(req)).isInstanceOf(jakarta.validation.ConstraintViolationException.class).hasMessageContaining("descripción");
  }

  @Test
  void editarDescripcionConservaStockYElMotivoDeAjustes() {
    var req=new ProductoRequestDTO("Editar descripción "+UUID.randomUUID().toString().substring(0,8),BigDecimal.ONE,10,0,categoria,EstadoProducto.ACTIVO);
    req.setIdUsuario(usuario);var p=productos.create(req);req.setDescripcion("Producto con descripción");
    productos.update(p.getIdProducto(),req);assertThat(productos.read(p.getIdProducto()).getStock()).isEqualTo(10);assertThat(productos.movimientos(p.getIdProducto())).hasSize(1);
    productos.ajustarStock(p.getIdProducto(),new AjusteStockDTO(UUID.randomUUID().toString(),usuario,10,8,"Dos unidades dañadas"));
    var ultimo=productos.movimientos(p.getIdProducto()).getFirst();assertThat(ultimo.observacion()).isEqualTo("Dos unidades dañadas");assertThat(ultimo.stockResultante()).isEqualTo(8);
  }

  VentaRequestDTO carrito(String clave, Long segundo, int primeraCantidad, int segundaCantidad) {
    return new VentaRequestDTO(clave, usuario, adulto, null, ModalidadCobro.SEPARADA, null,
        List.of(new VentaLineaRegistroDTO(producto, primeraCantidad),
            new VentaLineaRegistroDTO(segundo, segundaCantidad)));
  }

  PagoCuentaDTO pagoVenta(String clave, String monto) {
    return new PagoCuentaDTO(clave, usuario, turno, formaPago, new BigDecimal(monto), null);
  }

  @Test
  void ventaDeVariosProductosCalculaTotalYDescuentaCadaStockUnaSolaVez() {
    stock(10);
    var segundo = productoAdicional("2.00", 10);
    var req = carrito("varios-productos", segundo, 2, 3);
    var v = ventas.registrar(req);
    assertThat(v.total()).isEqualByComparingTo("13");
    assertThat(v.detalles()).hasSize(2);
    assertThat(ventas.registrar(req).idVenta()).isEqualTo(v.idVenta());
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    assertThat(productos.read(segundo).getStock()).isEqualTo(10);
    ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario));
    ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario));
    assertThat(productos.read(producto).getStock()).isEqualTo(8);
    assertThat(productos.read(segundo).getStock()).isEqualTo(7);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto)).hasSize(2);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(segundo)).hasSize(2);
  }

  @Test
  void editarBorradorReemplazaCarritoYConfirmaConPrecioGuardado() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var req = carrito("editar-carrito", segundo, 2, 3);
    var v = ventas.registrar(req);
    var antiguos = v.detalles().stream().map(VentaLineaRespuestaDTO::idDetalleVenta).toList();
    em.find(Producto.class, producto).setPrecioActual(new BigDecimal("4"));
    em.flush();
    var editada = ventas.editar(v.idVenta(), new VentaEditarDTO(usuario, null, "Comprador directo",
        List.of(new VentaLineaRegistroDTO(producto, 3), new VentaLineaRegistroDTO(segundo, 2))));
    assertThat(editada.total()).isEqualByComparingTo("16");
    assertThat(editada.compradorNombreLibre()).isEqualTo("Comprador directo");
    assertThat(editada.idClienteComprador()).isNull();
    assertThat(editada.idUsuario()).isEqualTo(v.idUsuario());
    assertThat(ventas.registrar(req).detalles()).hasSize(2);
    assertThatThrownBy(() -> ventas.registrarConfirmada(req)).isInstanceOf(ReglaNegocioException.class)
        .hasMessageContaining("editado");
    for (var id : antiguos) assertThat(em.find(DetalleVenta.class, id)).isNull();
    em.find(Producto.class, producto).setPrecioActual(new BigDecimal("5"));
    em.flush();
    assertThat(ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario)).total()).isEqualByComparingTo("16");
    assertThatThrownBy(() -> ventas.editar(v.idVenta(), new VentaEditarDTO(usuario, adulto, null, req.detalles())))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("borrador");
  }

  @Test
  void faltaDeStockEnSegundaLineaNoDescuentaPrimeraNiConfirmaVenta() {
    stock(10);
    var segundo = productoAdicional("2", 0);
    var v = ventas.registrar(carrito("segunda-sin-stock", segundo, 2, 1));
    assertThatThrownBy(() -> ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Stock insuficiente");
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    assertThat(productos.read(segundo).getStock()).isZero();
    assertThat(ventas.obtener(v.idVenta()).estadoVenta()).isEqualTo(EstadoVenta.BORRADOR);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(producto)).hasSize(1);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(segundo)).isEmpty();
  }

  @Test
  void productoInactivoAlConfirmarRechazaTodoElCarrito() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var v = ventas.registrar(carrito("inactivo-al-confirmar", segundo, 2, 1));
    productos.delete(segundo);
    assertThatThrownBy(() -> ventas.confirmar(v.idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("inactivo");
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    assertThat(productos.read(segundo).getStock()).isEqualTo(10);
  }

  @Test
  void carritoRechazaProductosRepetidosYTotalGlobalFueraDelModelo() {
    assertThatThrownBy(() -> ventas.registrar(new VentaRequestDTO("repetidos", usuario, adulto, null,
        ModalidadCobro.SEPARADA, null, List.of(new VentaLineaRegistroDTO(producto, 1), new VentaLineaRegistroDTO(producto, 2)))))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Agrupa");
    em.find(Producto.class, producto).setPrecioActual(new BigDecimal("99999999.99"));
    em.flush();
    var segundo = productoAdicional("99999999.99", 0);
    assertThatThrownBy(() -> ventas.registrar(carrito("total-excesivo", segundo, 100, 100)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("total de la venta");
  }

  @Test
  void cuentaDirectaEsSoloLecturaYPagoParcialYBoletaNoDuplicanCaja() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var v = ventas.registrarConfirmada(carrito("venta-cobro-directo", segundo, 2, 3));
    assertThat(cuentasVenta.cuenta(v.idVenta()).idCuenta()).isNull();
    assertThat(em.createQuery("from Comprobante where venta.idVenta=:id")
        .setParameter("id", v.idVenta()).getResultList()).isEmpty();
    var abono = pagoVenta("pago-directo", "5");
    var pagada = cuentasVenta.pagar(v.idVenta(), abono);
    cuentasVenta.pagar(v.idVenta(), abono);
    assertThat(pagada.saldo()).isEqualByComparingTo("8");
    assertThat(pagada.idReserva()).isNull();
    assertThat(pagada.numeroReserva()).isNull();
    assertThatThrownBy(() -> cuentasVenta.pagar(v.idVenta(), pagoVenta("sobrepago", "9")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("supera");
    cuentasVenta.pagar(v.idVenta(), pagoVenta("saldo-directo", "8"));
    var req = new ComprobanteCuentaDTO(usuario, turno, "BOLETA", adulto);
    var doc = cuentasVenta.emitir(v.idVenta(), req);
    assertThat(cuentasVenta.emitir(v.idVenta(), req).numero()).isEqualTo(doc.numero());
    assertThat(doc.saldo()).isZero();
    assertThat(doc.total()).isEqualByComparingTo("13");
    assertThat(doc.lineas()).hasSize(2);
    assertThat(doc.pagos()).hasSize(2);
    assertThat(doc.idReserva()).isNull();
    assertThat(doc.idVenta()).isEqualTo(v.idVenta());
    assertThat(em.createQuery("select count(f) from FlujoCaja f where f.pago.comprobante.idComprobante=:id "
        + "and f.tipoMovimientoCaja.nombreMovimiento='COBRO_VENTA'", Long.class)
        .setParameter("id", doc.idCuenta()).getSingleResult()).isEqualTo(2);
    assertThat(gastos.listar(turno)).isEmpty();
    assertThatThrownBy(() -> ventas.anular(v.idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void consumoSeparadoDeVariosProductosSePagaPorAmbasRutasSinDuplicar() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var reserva = reservas.registrar(reserva("venta-huesped", false));
    var detalle = reserva.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    var base = carrito("consumo-separado-multiple", segundo, 2, 3);
    var consumo = cuentasReserva.consumir(reserva.idReserva(), new VentaRequestDTO(base.claveRegistro(), usuario,
        adulto, null, ModalidadCobro.SEPARADA, detalle, base.detalles()));
    var doc = cuentasVenta.pagar(consumo.venta().idVenta(), pagoVenta("pago-compartido", "13"));
    cuentasReserva.pagar(reserva.idReserva(), pago("pago-compartido", "13", consumo.venta().idVenta()));
    assertThat(cuentasVenta.cuenta(consumo.venta().idVenta()).pagos()).hasSize(1);
    assertThat(doc.idReserva()).isEqualTo(reserva.idReserva());
    assertThat(doc.numeroReserva()).isEqualTo("R" + reserva.idReserva());
    assertThat(cuentasReserva.cuenta(reserva.idReserva()).saldoPendiente()).isEqualByComparingTo("80");
    var boleta = cuentasVenta.emitir(consumo.venta().idVenta(), new ComprobanteCuentaDTO(usuario, turno, "BOLETA", adulto));
    assertThat(boleta.numeroReserva()).isEqualTo(doc.numeroReserva());
  }

  @Test
  void consumoConsolidadoDeVariosProductosNoSePuedeCobrarDosVecesDesdeVentas() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var reserva = reservas.registrar(reserva("consolidado-multiple", false));
    var detalle = reserva.detalles().getFirst().idDetalleReserva();
    reservas.cambiarEstado(detalle, "ingreso", new OperacionUsuarioDTO(usuario));
    var base = carrito("consumo-principal", segundo, 2, 3);
    var c = cuentasReserva.consumir(reserva.idReserva(), new VentaRequestDTO(base.claveRegistro(), usuario,
        adulto, null, ModalidadCobro.RESERVA, detalle, base.detalles()));
    assertThat(cuentasReserva.cuenta(reserva.idReserva()).totalCargos()).isEqualByComparingTo("93");
    assertThat(cuentasReserva.cuenta(reserva.idReserva()).cuentas().getFirst().lineas()).hasSize(3);
    assertThatThrownBy(() -> cuentasVenta.pagar(c.venta().idVenta(), pagoVenta("no-doble", "13")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("principal");
  }

  @Test
  void anularVentaMultipleSinCuentaReponeStockUnaSolaVez() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var v = ventas.registrarConfirmada(carrito("anular-multiple", segundo, 2, 3));
    ventas.anular(v.idVenta(), new OperacionUsuarioDTO(usuario));
    ventas.anular(v.idVenta(), new OperacionUsuarioDTO(usuario));
    assertThat(productos.read(producto).getStock()).isEqualTo(10);
    assertThat(productos.read(segundo).getStock()).isEqualTo(10);
    assertThat(movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(segundo)).hasSize(3);
    assertThat(ventas.obtener(v.idVenta()).estadoVenta()).isEqualTo(EstadoVenta.ANULADA);
  }

  @Test
  void cerrarTurnoImpideConfirmacionNuevaPeroPermiteReintentarPagoRegistrado() {
    stock(10);
    var segundo = productoAdicional("2", 10);
    var v = ventas.registrarConfirmada(carrito("venta-turno", segundo, 2, 3));
    var req = pagoVenta("pago-antes-cierre", "5");
    cuentasVenta.pagar(v.idVenta(), req);
    var borrador = ventas.registrar(carrito("otro-borrador", segundo, 1, 1));
    var t = em.find(TurnoCaja.class, turno);
    t.setEstadoSesion(0);
    t.setFechaCierre(pe.com.rebolsysBackend.service.impl.ReglasPropuesta.ahora());
    em.flush();
    assertThat(cuentasVenta.pagar(v.idVenta(), req).pagado()).isEqualByComparingTo("5");
    assertThatThrownBy(() -> ventas.confirmar(borrador.idVenta(), new OperacionUsuarioDTO(usuario)))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("turno abierto");
    var nuevoTurno = new TurnoCaja();
    nuevoTurno.setUsuario(em.find(Usuario.class, usuario));
    nuevoTurno.setEstadoSesion(1);
    em.persist(nuevoTurno);
    var emitido = cuentasVenta.emitir(v.idVenta(),
        new ComprobanteCuentaDTO(usuario, nuevoTurno.getIdTurno(), "BOLETA", adulto));
    assertThat(em.find(Comprobante.class, emitido.idCuenta()).getTurnoCaja().getIdTurno())
        .isEqualTo(nuevoTurno.getIdTurno());
  }

  @Test
  void ventaDirectaNoSeCobraEnBorradorYValidaCantidadesDesdeHttp() throws Exception {
    var segundo = productoAdicional("2", 10);
    var v = ventas.registrar(carrito("borrador-sin-cobro", segundo, 1, 1));
    assertThatThrownBy(() -> cuentasVenta.pagar(v.idVenta(), pagoVenta("prematuro", "1")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Confirma");
    var invalida = carrito("cantidad-invalida", segundo, 0, 1);
    mvc.perform(post("/api/v1/ventas/confirmadas").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(invalida))).andExpect(status().isBadRequest());
  }


  @Test
  void catalogosConfiguracionIncluyenReferenciasYEstados() throws Exception {
    mvc.perform(get("/api/v1/configuraciones/catalogos")).andExpect(status().isOk())
        .andExpect(jsonPath("$.tiposHabitacion[?(@.idTipoHabitacion == " + tipoHabitacion + ")].precioBase").value(80))
        .andExpect(jsonPath("$.tiposItem[?(@.idTipoItem == " + tipoItem + ")]").isNotEmpty())
        .andExpect(jsonPath("$.categorias[?(@.idCategoria == " + categoria + ")]").isNotEmpty())
        .andExpect(jsonPath("$.estadosItem[2]").value("En habitacion"))
        .andExpect(jsonPath("$.estadosConservacion[0]").value("BUENO"));
  }

  @Test
  void paginacionHabitacionesCombinaFiltrosYConservaOrden() throws Exception {
    var h2 = habitaciones.create(habitacionDTO("Z%_!", 2, 2, 0));
    habitaciones.cambiarEstado(h2.idHabitacion(), new HabitacionEstadoGuardarDTO(EstadoHabitacion.MANTENIMIENTO));
    mvc.perform(get("/api/v1/habitaciones/configuracion/pagina")
        .param("idTipoHabitacion", tipoHabitacion.toString()).param("tamanio", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(2)).andExpect(jsonPath("$.ultima").value(false))
        .andExpect(jsonPath("$.contenido[0].idHabitacion").value(habitacion));
    mvc.perform(get("/api/v1/habitaciones/configuracion/pagina")
        .param("idTipoHabitacion", tipoHabitacion.toString()).param("buscar", "%_!")
        .param("estado", "MANTENIMIENTO").param("capacidadMinima", "2"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
        .andExpect(jsonPath("$.contenido[0].idHabitacion").value(h2.idHabitacion()));
    mvc.perform(get("/api/v1/habitaciones/configuracion/pagina")
        .param("idTipoHabitacion", tipoHabitacion.toString()).param("capacidadMinima", "4"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
  }

  @Test
  void cambiarEstadoNoSobrescribeConfiguracionNiPermiteOcuparSinIngreso() throws Exception {
    var anterior = habitaciones.read(habitacion);
    mvc.perform(patch("/api/v1/habitaciones/" + habitacion + "/estado")
        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoHabitacion\":\"LIMPIEZA\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.estadoHabitacion").value("LIMPIEZA"));
    var actual = habitaciones.read(habitacion);
    assertThat(actual.numeroCuarto()).isEqualTo(anterior.numeroCuarto());
    assertThat(actual.capacidadPersonas()).isEqualTo(anterior.capacidadPersonas());
    assertThat(actual.precioBase()).isEqualByComparingTo(anterior.precioBase());
    mvc.perform(patch("/api/v1/habitaciones/" + habitacion + "/estado")
        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoHabitacion\":\"OCUPADO\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void configuracionNoPuedeLiberarHabitacionConEstanciaEnCurso() throws Exception {
    var r = reservas.registrar(reserva("estado-config", false));
    reservas.cambiarEstado(r.detalles().getFirst().idDetalleReserva(), "ingreso", new OperacionUsuarioDTO(usuario));
    mvc.perform(patch("/api/v1/habitaciones/" + habitacion + "/estado")
        .contentType(MediaType.APPLICATION_JSON).content("{\"estadoHabitacion\":\"DISPONIBLE\"}"))
        .andExpect(status().isConflict());
    assertThat(habitaciones.read(habitacion).estadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADO);
  }

  @Test
  void conservacionRetiroYTrasladoMantienenTodasLasFichasAnteriores() {
    var i = items.create(new ItemRequestDTO("TV", tipoItem, EstadoItem.ACTIVO));
    var primero = items.asignar(i.idItem(), new ItemAsignacionDTO("asignar", usuario, habitacion, EstadoConservacion.BUENO, "Instalar"));
    var cambio = new ItemConservacionDTO("conservar", usuario, EstadoConservacion.DANADO, "Pantalla dañada");
    items.conservacion(i.idItem(), cambio);
    items.conservacion(i.idItem(), cambio);
    assertThat(items.movimientos(i.idItem())).hasSize(2);
    assertThat(em.find(DetalleHabitacion.class, primero.idDetalleHabitacion()).getEstadoActualItem())
        .isEqualTo(EstadoConservacion.BUENO);
    var h2 = habitaciones.create(habitacionDTO("NUEVOTV", 2, 2, 0));
    items.asignar(i.idItem(), new ItemAsignacionDTO("trasladar", usuario, h2.idHabitacion(), EstadoConservacion.DANADO, "Taller"));
    var retirada = items.asignar(i.idItem(), new ItemAsignacionDTO("retirar", usuario, null, EstadoConservacion.EN_REPARACION, "Enviar reparación"));
    assertThat(retirada.idHabitacion()).isNull();
    assertThat(retirada.estadoActualItem()).isEqualTo(EstadoConservacion.EN_REPARACION);
    assertThat(habitaciones.mobiliario(habitacion)).isEmpty();
    assertThat(habitaciones.mobiliario(h2.idHabitacion())).isEmpty();
    var historial = items.historial(i.idItem());
    assertThat(historial).extracting(ItemHistorialDTO::accion).containsExactly("RETIRO", "TRASLADO", "CONSERVACION", "ASIGNACION");
    assertThat(historial.get(0).idHabitacionOrigen()).isEqualTo(h2.idHabitacion());
    assertThat(historial.get(0).idHabitacionDestino()).isNull();
    assertThat(historial.get(1).idHabitacionOrigen()).isEqualTo(habitacion);
    assertThat(historial.get(1).idHabitacionDestino()).isEqualTo(h2.idHabitacion());
    assertThat(historial).allSatisfy(h -> {
      assertThat(h.usuario()).isNotBlank();
      assertThat(h.observacion()).doesNotContainPattern("\\[[0-9a-f]{64}\\]");
    });
    items.delete(i.idItem());
    assertThat(items.historial(i.idItem())).hasSize(4);
  }

  @Test
  void filtrosMobiliarioUsanUbicacionActualYUltimaConservacion() throws Exception {
    var i = items.create(new ItemRequestDTO("Silla %_!", tipoItem, EstadoItem.ACTIVO));
    items.asignar(i.idItem(), new ItemAsignacionDTO("ubicacion", usuario, habitacion, EstadoConservacion.BUENO, "Instalar"));
    items.conservacion(i.idItem(), new ItemConservacionDTO("dano", usuario, EstadoConservacion.DANADO, "Revisar"));
    for (String estado : List.of("En habitacion", "EN_HABITACION")) {
      mvc.perform(get("/api/v1/items/configuracion").param("idTipoItem", tipoItem.toString())
          .param("estado", estado).param("idHabitacion", habitacion.toString())
          .param("estadoConservacion", "DANADO").param("buscar", "%_!").param("sinAsignar", "false"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
          .andExpect(jsonPath("$.contenido[0].idItem").value(i.idItem()));
    }
    items.asignar(i.idItem(), new ItemAsignacionDTO("retira", usuario, null, EstadoConservacion.EN_REPARACION, "Taller"));
    mvc.perform(get("/api/v1/items/configuracion").param("idHabitacion", habitacion.toString()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    mvc.perform(get("/api/v1/items/configuracion").param("idTipoItem", tipoItem.toString())
        .param("sinAsignar", "true").param("estadoConservacion", "EN_REPARACION"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
  }

  @Test
  void conservacionExigeAsignacionYRechazaClaveConDatosDistintos() throws Exception {
    var i = items.create(new ItemRequestDTO("Mesa", tipoItem, EstadoItem.ACTIVO));
    var r = new ItemConservacionDTO("misma-clave", usuario, EstadoConservacion.BUENO, "Revisar");
    mvc.perform(put("/api/v1/items/" + i.idItem() + "/conservacion")
        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(r)))
        .andExpect(status().isConflict());
    items.asignar(i.idItem(), new ItemAsignacionDTO("inicio", usuario, habitacion, EstadoConservacion.BUENO, "Instalar"));
    items.conservacion(i.idItem(), r);
    assertThatThrownBy(() -> items.conservacion(i.idItem(), new ItemConservacionDTO("misma-clave", usuario, EstadoConservacion.DANADO, "Distinto")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("clave");
    assertThat(items.movimientos(i.idItem())).hasSize(2);
  }

  @Test
  void productosStockBajoIncluyeElMinimoYCombinaEstadoYCategoria() throws Exception {
    stock(1);
    var otro = productoAdicional("5", 2);
    mvc.perform(get("/api/v1/productos").param("idCategoria", categoria.toString()).param("stockBajo", "true"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
        .andExpect(jsonPath("$.contenido[0].idProducto").value(producto))
        .andExpect(jsonPath("$.contenido[0].stockBajo").value(true));
    mvc.perform(get("/api/v1/productos").param("idCategoria", categoria.toString()).param("stockBajo", "false"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].idProducto").value(otro))
        .andExpect(jsonPath("$.contenido[0].stockBajo").value(false));
    productos.delete(producto);
    mvc.perform(get("/api/v1/productos").param("idCategoria", categoria.toString()).param("stockBajo", "true").param("estado", "ACTIVO"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
  }

  @Test
  void busquedaProductosTrataComodinesComoTextoYBuscaCategoria() {
    for (String caracter : List.of("%", "_", "!", "\\")) {
      var p = productos.create(new ProductoRequestDTO("Especial " + caracter, BigDecimal.ONE, 0, 0, categoria, EstadoProducto.ACTIVO));
      assertThat(productos.listar(0, 10, caracter, categoria, null, null).getContenido())
          .extracting(ProductoResponseDTO::getIdProducto).containsExactly(p.getIdProducto());
    }
    assertThat(productos.listar(0, 10, categorias.read(categoria).getNombreCategoria(), categoria, null, null).getTotalElementos())
        .isEqualTo(5);
  }

  @Test
  void configuracionValidaPaginacionEnumsYReferenciasInexistentes() throws Exception {
    for (String ruta : List.of("/habitaciones/configuracion/pagina", "/items/configuracion", "/productos")) {
      mvc.perform(get("/api/v1" + ruta).param("pagina", "-1")).andExpect(status().isBadRequest());
      mvc.perform(get("/api/v1" + ruta).param("tamanio", "101")).andExpect(status().isBadRequest());
      mvc.perform(get("/api/v1" + ruta).param("estado", "DESCONOCIDO")).andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/v1/habitaciones/configuracion/pagina").param("idTipoHabitacion", "999999999"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/items/configuracion").param("idTipoItem", "999999999"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/items/configuracion").param("idHabitacion", "999999999"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/productos").param("idCategoria", "999999999"))
        .andExpect(status().isNotFound());
  }

  @Test
  void habitacionDuplicadaYEstadoCanonicoItemSeValidanPorHttp() throws Exception {
    mvc.perform(post("/api/v1/habitaciones").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(habitacionDTO(habitaciones.read(habitacion).numeroCuarto(), 3, 2, 1))))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON)
        .content("{\"nombreItem\":\"Cama\",\"idTipoItem\":" + tipoItem + ",\"estadoItem\":\"ACTIVO\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.estadoItem").value("Activo"));
  }

  @Test
  void tarifaPorHabitacionPersisteYSeConservaConClientesAnteriores() throws Exception {
    var original = habitaciones.read(habitacion);
    var propia = new HabitacionRequestDTO(original.numeroCuarto(), original.idTipoHabitacion(),
        original.estadoHabitacion(), original.capacidadPersonas(), original.plazasCama(),
        original.maxMenoresSinCama(), new BigDecimal("40.00"));
    mvc.perform(put("/api/v1/habitaciones/" + habitacion).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(propia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precioPorNoche").value(40))
        .andExpect(jsonPath("$.precioBase").value(40));
    em.flush(); em.clear();
    assertThat(em.find(Habitacion.class, habitacion).getPrecioPorNoche()).isEqualByComparingTo("40.00");
    assertThat(tiposHabitacion.read(tipoHabitacion).precioBase()).isEqualByComparingTo(original.precioBase());
    var anterior = new java.util.LinkedHashMap<String, Object>();
    anterior.put("numeroCuarto", original.numeroCuarto()); anterior.put("idTipoHabitacion", original.idTipoHabitacion());
    anterior.put("estadoHabitacion", original.estadoHabitacion()); anterior.put("capacidadPersonas", original.capacidadPersonas());
    anterior.put("plazasCama", original.plazasCama()); anterior.put("maxMenoresSinCama", original.maxMenoresSinCama());
    mvc.perform(put("/api/v1/habitaciones/" + habitacion).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(anterior)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.precioPorNoche").value(40));
    em.flush(); em.clear();
    assertThat(habitaciones.estado(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
        .filter(h -> h.idHabitacion().equals(habitacion)).findFirst().orElseThrow().precio())
        .isEqualByComparingTo("40.00");
  }

}
