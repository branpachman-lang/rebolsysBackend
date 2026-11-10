package pe.com.rebolsysBackend.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;
import pe.com.rebolsysBackend.enums.EstadoComprobante;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.service.impl.AccesosService;
import pe.com.rebolsysBackend.service.impl.CredencialesService;
import pe.com.rebolsysBackend.service.service.ProveedorFacturacionClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seguridad;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
    "springdoc.api-docs.version=OPENAPI_3_0",
    "app.sesion-prueba.habilitada=false", "app.caja.directorio-evidencias=target/evidencias-caja-test"
})
@ActiveProfiles("seguridad-test")
@AutoConfigureMockMvc
class AccesoHttpTest {
  private static final String API = "/api/v1";
  private static final String CLAVE_GERENTE = "Gerente temporal de prueba";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired EntityManager em;
  @Autowired TransactionTemplate tx;
  @Autowired CredencialesService credenciales;
  @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;
  @MockitoBean(name = "apisPeruRestClient") RestClient consultaDocumentos;
  @MockitoBean ProveedorFacturacionClient facturacion;

  @Test
  void cadaRutaOperativaTieneReglaExplicita() throws Exception {
    var sinRegla = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
        .filter(e -> e.getKey().getPatternValues().stream().anyMatch(p -> p.startsWith(API + "/")))
        .filter(e -> AccesosService.regla(e.getValue()).length == 0).map(Map.Entry::getKey).toList();
    assertThat(sinRegla).isEmpty();
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/seguridad/usuarios'].post.security[0].sesion").isArray());
  }

  @Test
  void autenticaConTemporalLimitaFuncionesYRevocaAccesos() throws Exception {
    var tipo = prepararGerente();
    mvc.perform(get(API + "/reservas")).andExpect(status().isUnauthorized());
    mvc.perform(post(API + "/sesion/iniciar").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("usuario", "gerencia", "contrasena", CLAVE_GERENTE))))
        .andExpect(status().isForbidden());
    var temporalGerente = sesion(peticion(post(API + "/sesion/iniciar"), null,
        Map.of("usuario", "gerencia", "contrasena", CLAVE_GERENTE))
        .andExpect(status().isOk()).andExpect(jsonPath("$.requiereCambio").value(true))
        .andExpect(jsonPath("$.idTurno").isEmpty()));
    mvc.perform(get(API + "/seguridad/usuarios").session(temporalGerente)).andExpect(status().isForbidden());
    var activacionGerente = peticion(post(API + "/sesion/contrasena"), temporalGerente,
        Map.of("actual", CLAVE_GERENTE, "nueva", "Gerente permanente de prueba"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.gerente").value(true))
        .andExpect(jsonPath("$.idTurno").isNumber());
    int turnoGerente = cuerpo(activacionGerente).path("idTurno").asInt();
    var gerente = sesion(peticion(post(API + "/sesion/iniciar"), null,
        Map.of("usuario", "gerencia", "contrasena", "Gerente permanente de prueba"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idTurno").value(turnoGerente)));
    var empleadoGerente = cuerpo(mvc.perform(get(API + "/seguridad/empleados").session(gerente))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].protegido").value(true))).get(0).path("idEmpleado").asLong();
    peticion(put(API + "/seguridad/empleados/" + empleadoGerente + "/estado"), gerente, Map.of("activo", false))
        .andExpect(status().isConflict());
    mvc.perform(get(API + "/recepcion/catalogos").session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.turnosAbiertos[0].idTurno").value(turnoGerente));
    mvc.perform(get(API + "/limpieza/inicio").session(gerente))
        .andExpect(status().isOk()).andExpect(jsonPath("$.alcance").value("GENERAL"))
        .andExpect(jsonPath("$.idTurno").isEmpty());
    mvc.perform(get(API + "/limpieza/informes").session(gerente)).andExpect(status().isOk());
    mvc.perform(get(API + "/limpieza/tickets").session(gerente)).andExpect(status().isOk());

    long habitacion = tx.execute(s -> {
      var tipoHabitacion = new TipoHabitacion();
      tipoHabitacion.setNombreTipo("Simple");
      tipoHabitacion.setCapacidadMax(1);
      em.persist(tipoHabitacion);
      var cuarto = new Habitacion();
      cuarto.setNumeroCuarto("101");
      cuarto.setTipoHabitacion(tipoHabitacion);
      cuarto.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);
      cuarto.setCapacidadPersonas(1);
      cuarto.setPlazasCama(1);
      em.persist(cuarto);
      return cuarto.getIdHabitacion();
    });
    String cuarto = API + "/limpieza/habitaciones/" + habitacion;
    mvc.perform(get(cuarto).session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.habitacion.puedeIniciarLimpieza").value(true));
    long informe = cuerpo(peticion(post(cuarto + "/iniciar"), gerente, null)
        .andExpect(status().isOk()).andExpect(jsonPath("$.habitacion.estado").value("EN_LIMPIEZA")))
        .path("idInformeRevision").asLong();
    peticion(post(API + "/limpieza/borradores/" + informe + "/enviar"), gerente, Map.of("items", List.of()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.informe.estado").value("POR_APROBAR"));
    mvc.perform(get(cuarto).session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoInforme").value("POR_APROBAR"));
    peticion(post(API + "/limpieza/informes/" + informe + "/aprobar"), gerente, null).andExpect(status().isOk());

    var rol = cuerpo(peticion(post(API + "/seguridad/roles"), gerente,
        Map.of("nombre", "recepcionista", "permisos", List.of("RESERVAS:VER", "LIMPIEZA:INICIAR", "CAJA:VER", "GASTOS:CREAR")))
        .andExpect(status().isCreated())).path("idRol").asLong();
    var empleado = cuerpo(peticion(post(API + "/seguridad/empleados"), gerente, Map.of(
        "idTipoDocumento", tipo, "numeroDocumento", "00000002", "nombres", "Operadora",
        "apellidos", "Prueba", "correo", "operadora@example.test", "fechaContratacion", "2025-01-01"))
        .andExpect(status().isCreated())).path("idEmpleado").asLong();
    var nuevo = cuerpo(peticion(post(API + "/seguridad/usuarios"), gerente, Map.of(
        "idEmpleado", empleado, "usuario", "operadora@example.test",
        "accesos", Map.of("roles", List.of(rol), "permisos", List.of("HABITACIONES:VER"))))
        .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store")));
    long id = nuevo.path("cuenta").path("idUsuario").asLong();
    String clave = nuevo.path("credencial").path("contrasenaTemporal").asText();
    assertThat(clave).hasSize(24);
    var guardado = tx.execute(s -> em.createQuery("""
        select c.valorHash from UsuarioCredencial c where c.usuario.idUsuario=:id
        """, String.class).setParameter("id", id).getSingleResult());
    assertThat(guardado).startsWith("$2").doesNotContain(clave);
    String rutaEmpleado = API + "/seguridad/empleados/" + empleado;
    String rutaUsuario = API + "/seguridad/usuarios/" + id;
    String rutaRol = API + "/seguridad/roles/" + rol;
    var datosEmpleado = Map.of("nombres", "Operadora", "apellidos", "Actualizada", "fechaContratacion", "2025-01-02");
    peticion(put(rutaEmpleado), gerente, datosEmpleado).andExpect(status().isOk())
        .andExpect(jsonPath("$.apellidos").value("Actualizada"))
        .andExpect(jsonPath("$.numeroDocumento").value("00000002"));
    peticion(delete(rutaRol), gerente, null).andExpect(status().isConflict());

    var primera = sesion(peticion(post(API + "/sesion/iniciar"), null, Map.of("usuario", "OPERADORA@EXAMPLE.TEST", "contrasena", clave))
        .andExpect(status().isOk()).andExpect(jsonPath("$.permisos").isEmpty()));
    mvc.perform(get(API + "/reservas").session(primera)).andExpect(status().isForbidden());
    var activacionRecepcion = peticion(post(API + "/sesion/contrasena"), primera,
        Map.of("actual", clave, "nueva", "Clave elegida por operadora"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.requiereCambio").value(false))
        .andExpect(jsonPath("$.permisos").isArray()).andExpect(jsonPath("$.idTurno").isNumber());
    int turnoRecepcion = cuerpo(activacionRecepcion).path("idTurno").asInt();
    assertThat(turnoRecepcion).isNotEqualTo(turnoGerente);
    var operadora = sesion(peticion(post(API + "/sesion/iniciar"), null,
        Map.of("usuario", "operadora@example.test", "contrasena", "Clave elegida por operadora"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idTurno").value(turnoRecepcion)));
    peticion(post(cuarto + "/iniciar"), operadora, null).andExpect(status().isConflict());
    mvc.perform(get(API + "/reservas").session(operadora)).andExpect(status().isOk());
    mvc.perform(get(API + "/habitaciones/configuracion").session(operadora)).andExpect(status().isOk());
    mvc.perform(get(API + "/productos").session(operadora)).andExpect(status().isForbidden());
    mvc.perform(get(API + "/seguridad/usuarios").session(operadora)).andExpect(status().isForbidden());
    peticion(post(API + "/reservas"), operadora, Map.of()).andExpect(status().isForbidden());
    peticion(put(API + "/habitaciones/1"), operadora, Map.of()).andExpect(status().isForbidden());
    peticion(delete(API + "/habitaciones/1"), operadora, null).andExpect(status().isForbidden());
    peticion(put(API + "/seguridad/usuarios/" + id + "/accesos"), operadora,
        Map.of("roles", List.of(), "permisos", List.of("RESERVAS:CREAR"))).andExpect(status().isForbidden());
    probarCaja(gerente, operadora, turnoGerente);
    var accesos = Map.of("roles", List.of(), "permisos", List.of("HABITACIONES:VER"));
    peticion(put(rutaUsuario), gerente, Map.of("usuario", "GERENCIA", "accesos", accesos))
        .andExpect(status().isConflict());
    peticion(put(rutaUsuario), gerente, Map.of("usuario", "recepcion.nueva", "accesos", accesos))
        .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value("recepcion.nueva"));
    peticion(delete(rutaRol), gerente, null).andExpect(status().isNoContent());
    mvc.perform(get(API + "/seguridad/roles").session(gerente)).andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get(API + "/reservas").session(operadora)).andExpect(status().isForbidden());
    var temporalNueva = cuerpo(peticion(post(rutaUsuario + "/credencial-temporal"), gerente, null)
        .andExpect(status().isOk())).path("contrasenaTemporal").asText();
    mvc.perform(get(API + "/habitaciones/configuracion").session(operadora)).andExpect(status().isUnauthorized());
    var sesionRenombrada = sesion(peticion(post(API + "/sesion/iniciar"), null,
        Map.of("usuario", "recepcion.nueva", "contrasena", temporalNueva)).andExpect(status().isOk()));
    peticion(put(rutaEmpleado + "/estado"), gerente, Map.of("activo", false)).andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaCese").isNotEmpty()).andExpect(jsonPath("$.tieneUsuario").value(true));
    mvc.perform(get(API + "/sesion/actual").session(sesionRenombrada)).andExpect(status().isUnauthorized());
    peticion(put(rutaUsuario + "/estado"), gerente, Map.of("estado", "ACTIVO")).andExpect(status().isConflict());
    peticion(put(rutaEmpleado + "/estado"), gerente, Map.of("activo", true)).andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaCese").isEmpty());
    peticion(put(rutaUsuario + "/estado"), gerente, Map.of("estado", "ACTIVO")).andExpect(status().isOk());
    peticion(post(API + "/sesion/cerrar"), gerente, null).andExpect(status().isNoContent());
  }

  private void probarCaja(MockHttpSession gerente, MockHttpSession operadora, long idTurno) throws Exception {
    var catalogo = tx.execute(s -> prepararMovimientosCaja(idTurno));
    String ruta = API + "/caja/turnos/" + idTurno;
    mvc.perform(get(ruta).session(operadora)).andExpect(status().isNotFound());
    peticion(post(ruta + "/cierre"), operadora, Map.of()).andExpect(status().isForbidden());
    peticion(post(API + "/gastos"), operadora, Map.of("idUsuario", catalogo.get("usuario"), "idTurno", idTurno,
        "idTipoMovimiento", catalogo.get("gasto"), "concepto", "Gasto ajeno", "monto", 10))
        .andExpect(status().isConflict());
    var inicial = cuerpo(mvc.perform(get(API + "/caja/actual").session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.arqueos[0].montoCalculado").value(140.50))
        .andExpect(jsonPath("$.arqueos[1].montoCalculado").value(25)));

    var foto = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", foto);
    var archivo = new MockMultipartFile("archivo", "comprobante.png", "image/png", foto.toByteArray());
    var datosIngreso = datosMovimiento(catalogo.get("ingreso"), "Ingreso de prueba", "10.00");
    var ingreso = cuerpo(peticion(multipart(ruta + "/movimientos").file(datosIngreso).file(archivo), gerente, null)
        .andExpect(status().isCreated()).andExpect(jsonPath("$.signo").value(1)));
    var evidencia = ingreso.path("evidencia").asText();
    mvc.perform(get(evidencia).session(gerente)).andExpect(status().isOk()).andExpect(content().bytes(foto.toByteArray()));
    mvc.perform(get(evidencia).session(operadora)).andExpect(status().isNotFound());
    peticion(multipart(ruta + "/movimientos").file(datosIngreso).file(archivo), gerente, null)
        .andExpect(status().isCreated()).andExpect(jsonPath("$.idFlujo").value(ingreso.path("idFlujo").asInt()));
    peticion(multipart(ruta + "/movimientos").file(datosMovimiento(catalogo.get("gasto"), "Gasto de prueba", "20.00")), gerente, null)
        .andExpect(status().isCreated()).andExpect(jsonPath("$.signo").value(-1));

    mvc.perform(get(ruta + "/movimientos").session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(3));
    var arqueos = List.of(Map.of("idFormaPago", catalogo.get("efectivo"), "montoDeclarado", new BigDecimal("129.50")),
        Map.of("idFormaPago", catalogo.get("yape"), "montoDeclarado", 25));
    peticion(post(ruta + "/cierre"), gerente, Map.of("revision", inicial.path("revision").asText(), "arqueos", arqueos))
        .andExpect(status().isConflict());
    var actual = cuerpo(mvc.perform(get(ruta).session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.abierto").value(true)).andExpect(jsonPath("$.arqueos[0].montoCalculado").value(130.50)));
    String revision = actual.path("revision").asText();
    var cierre = Map.of("revision", revision, "arqueos", arqueos);
    peticion(post(ruta + "/cierre"), gerente, Map.of("revision", revision, "arqueos", List.of(arqueos.getFirst())))
        .andExpect(status().isConflict());
    peticion(post(ruta + "/cierre"), gerente, cierre).andExpect(status().isOk())
        .andExpect(jsonPath("$.abierto").value(false)).andExpect(jsonPath("$.arqueos[0].diferencia").value(-1));
    peticion(post(ruta + "/cierre"), gerente, cierre).andExpect(status().isConflict());
    peticion(multipart(ruta + "/movimientos").file(datosMovimiento(catalogo.get("ingreso"), "Otro ingreso", "10.00")), gerente, null)
        .andExpect(status().isConflict());
    mvc.perform(get(API + "/caja/actual").session(gerente)).andExpect(status().isOk())
        .andExpect(jsonPath("$.idTurno").value(idTurno)).andExpect(jsonPath("$.arqueos.length()").value(2));
  }

  private MockMultipartFile datosMovimiento(Long tipo, String concepto, String monto) {
    return new MockMultipartFile("datos", "", "application/json", json.writeValueAsBytes(Map.of(
        "idTipoMovimiento", tipo, "concepto", concepto, "monto", new BigDecimal(monto), "claveRegistro", UUID.randomUUID())));
  }

  private Map<String, Long> prepararMovimientosCaja(long idTurno) {
    var turno = em.find(TurnoCaja.class, idTurno);
    turno.setMontoBase(new BigDecimal("50.00"));
    var efectivo = new FormaPago(null, "EFECTIVO"); em.persist(efectivo);
    var yape = new FormaPago(null, "YAPE"); em.persist(yape);
    var ingreso = new TipoMovimientoCaja(null, "INGRESO_MANUAL", 1); em.persist(ingreso);
    var gasto = new TipoMovimientoCaja(null, "GASTO", -1); em.persist(gasto);
    var tipoCobro = new TipoMovimientoCaja(null, "COBRO", 1); em.persist(tipoCobro);
    var tipoComprobante = new TipoComprobante(null, "BOLETA", "Boleta", "03"); em.persist(tipoComprobante);
    var serie = new SerieComprobante(new SerieComprobanteId(tipoComprobante.getIdTipoComprobante(), "TEST"), 1L, tipoComprobante);
    em.persist(serie);
    var comprobante = new Comprobante();
    comprobante.setTipoComprobante(tipoComprobante);
    comprobante.setSerieComprobante(serie);
    comprobante.setSerie("TEST");
    comprobante.setTurnoCaja(turno);
    comprobante.setSubtotal(BigDecimal.ZERO);
    comprobante.setIgv(BigDecimal.ZERO);
    comprobante.setTotalPagar(new BigDecimal("127.50"));
    comprobante.setEstadoComprobante(EstadoComprobante.BORRADOR);
    em.persist(comprobante);
    var pago = pagoCaja(comprobante, turno, efectivo, "90.50", 1);
    pagoCaja(comprobante, turno, yape, "25.00", 1);
    pagoCaja(comprobante, turno, yape, "12.00", 0);
    var flujo = new FlujoCaja();
    flujo.setTurnoCaja(turno);
    flujo.setTipoMovimientoCaja(tipoCobro);
    flujo.setPago(pago);
    flujo.setConcepto("Cobro de prueba");
    flujo.setMonto(pago.getMonto());
    flujo.setFechaHora(LocalDateTime.now());
    em.persist(flujo);
    return Map.of("ingreso", ingreso.getIdTipoMovimiento(), "gasto", gasto.getIdTipoMovimiento(),
        "efectivo", efectivo.getIdFormaPago(), "yape", yape.getIdFormaPago(), "usuario", turno.getUsuario().getIdUsuario());
  }

  private Pago pagoCaja(Comprobante comprobante, TurnoCaja turno, FormaPago forma, String monto, int verificado) {
    var pago = new Pago();
    pago.setComprobante(comprobante);
    pago.setTurnoCaja(turno);
    pago.setUsuario(turno.getUsuario());
    pago.setFormaPago(forma);
    pago.setMonto(new BigDecimal(monto));
    pago.setFechaPago(LocalDateTime.now());
    pago.setVerificado(verificado);
    em.persist(pago);
    return pago;
  }

  private Long prepararGerente() {
    Long id = tx.execute(s -> {
      var tipo = new TipoDocumento(null, "DNI", "1"); em.persist(tipo);
      var perfil = new Perfil(null, tipo, "00000001", "gerencia@example.test", null); em.persist(perfil);
      var persona = new PersonaNatural(null, perfil, "Gerente", "Prueba", null); em.persist(persona);
      var empleado = new Empleado(null, persona, LocalDate.of(2025, 1, 1), null); em.persist(empleado);
      em.persist(new Usuario(null, empleado, "gerencia", EstadoUsuario.ACTIVO));
      return tipo.getIdTipoDocumento();
    });
    credenciales.prepararGerente("gerencia", CLAVE_GERENTE);
    return id;
  }

  private ResultActions peticion(AbstractMockHttpServletRequestBuilder<?> req, MockHttpSession sesion, Object datos) throws Exception {
    var token = get(API + "/sesion/csrf");
    if (sesion != null) token.session(sesion);
    var respuesta = mvc.perform(token).andExpect(status().isOk());
    var proteccion = cuerpo(respuesta);
    req.session(sesion(respuesta)).header(proteccion.path("cabecera").asText(), proteccion.path("token").asText());
    if (datos != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(datos));
    return mvc.perform(req);
  }

  private MockHttpSession sesion(ResultActions resultado) {
    return (MockHttpSession) resultado.andReturn().getRequest().getSession(false);
  }

  private JsonNode cuerpo(ResultActions resultado) {
    return json.readTree(resultado.andReturn().getResponse().getContentAsByteArray());
  }
}
