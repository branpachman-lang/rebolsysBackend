package pe.com.rebolsysBackend.limpieza;

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
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import pe.com.rebolsysBackend.dto.ItemAsignacionDTO;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.service.impl.ReglasPropuesta;
import pe.com.rebolsysBackend.service.service.ItemService;
import pe.com.rebolsysBackend.service.service.ProveedorFacturacionClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:limpieza;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "springdoc.api-docs.version=OPENAPI_3_0",
    "app.sesion-prueba.habilitada=true", "app.sesion-prueba.usuario=prueba_limpieza",
    "app.limpieza.directorio-evidencias=target/evidencias-limpieza-test"
})
@AutoConfigureMockMvc
@ActiveProfiles("limpieza-test")
class FlujoLimpiezaTest {
  private static final String API = "/api/v1/limpieza";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired EntityManager em;
  @Autowired TransactionTemplate tx;
  @Autowired ItemService items;
  @MockitoBean(name = "apisPeruRestClient") RestClient consultaDocumentos;
  @MockitoBean ProveedorFacturacionClient facturacion;
  private Long habitacion;
  private Long item;
  private Long usuarioId;

  @BeforeEach
  void preparar() {
    tx.executeWithoutResult(s -> {
      var tipoDoc = new TipoDocumento(null, "Documento de prueba", null);
      em.persist(tipoDoc);
      var perfil = new Perfil(null, tipoDoc, "10000000", null, null);
      em.persist(perfil);
      var persona = new PersonaNatural(null, perfil, "Personal", "Prueba", null);
      em.persist(persona);
      var empleado = new Empleado(null, persona, LocalDate.of(2025, 1, 1), null);
      em.persist(empleado);
      var usuario = new Usuario(null, empleado, "prueba_limpieza", EstadoUsuario.ACTIVO);
      em.persist(usuario);
      usuarioId = usuario.getIdUsuario();
      var turno = new TurnoCaja();
      turno.setUsuario(usuario);
      turno.setFechaApertura(ReglasPropuesta.ahora().minusHours(1));
      turno.setEstadoSesion(1);
      em.persist(turno);
      var tipo = new TipoHabitacion();
      tipo.setNombreTipo("Simple");
      tipo.setCapacidadMax(2);
      em.persist(tipo);
      var cuarto = new Habitacion();
      cuarto.setTipoHabitacion(tipo);
      cuarto.setNumeroCuarto("301");
      cuarto.setCapacidadPersonas(2);
      cuarto.setPlazasCama(2);
      cuarto.setEstadoHabitacion(EstadoHabitacion.LIMPIEZA);
      em.persist(cuarto);
      registrarSalida(cuarto, perfil, usuario);
      var tipoItem = new TipoItem(null, "Muebles");
      em.persist(tipoItem);
      var mueble = new Item();
      mueble.setTipoItem(tipoItem);
      mueble.setNombreItem("Mesa");
      mueble.setEstadoItem(EstadoItem.ACTIVO);
      em.persist(mueble);
      em.persist(new TipoMovimientoAlmacen(null, "ASIG_ITEM", "Asignación", 0));
      em.persist(new TipoMovimientoAlmacen(null, "EST_ITEM", "Conservación", 0));
      em.persist(new TipoMovimientoAlmacen(null, "RET_ITEM", "Retiro", 0));
      habitacion = cuarto.getIdHabitacion();
      item = mueble.getIdItem();
      items.asignar(item, new ItemAsignacionDTO("asignacion-prueba", usuarioId,
          habitacion, EstadoConservacion.DANADO, "Inventario anterior"));
    });
  }

  @Test
  void protegeLaVistaDeHabitacionYResuelveIncidenciasEnTransaccionesCompletas() throws Exception {
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/limpieza/informes/{id}/aprobar'].post").exists());
    mvc.perform(get(API + "/habitaciones").param("estado", "POR_LIMPIAR").param("numeroHabitacion", "301"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].idHabitacion").value(habitacion));
    mvc.perform(get(API + "/habitaciones").param("fechaSalida", "2025-10-27"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    mvc.perform(get(API + "/habitaciones").param("fechaSalida", "2025-10-28"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].fechaSalida").value("2025-10-28T08:30:00"));
    String cuarto = API + "/habitaciones/" + habitacion;
    String estadoFisico = "/api/v1/habitaciones/" + habitacion + "/estado";
    long id = respuesta(mvc.perform(post(cuarto + "/iniciar"))).path("idInformeRevision").asLong();
    mvc.perform(post(cuarto + "/iniciar"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idInformeRevision").value(id))
        .andExpect(jsonPath("$.items[0].estado").doesNotExist());
    String enviar = API + "/borradores/" + id + "/enviar";
    String informe = API + "/informes/" + id;
    mvc.perform(get(cuarto)).andExpect(status().isOk())
        .andExpect(jsonPath("$.habitacion.estado").value("EN_LIMPIEZA"))
        .andExpect(jsonPath("$.habitacion.responsable.usuario").value("prueba_limpieza"))
        .andExpect(jsonPath("$.items[0].estado").value("SIN_REGISTRAR"))
        .andExpect(jsonPath("$.estadoInforme").value("SIN_INFORME"))
        .andExpect(jsonPath("$.estadoTickets").value("SIN_TICKET"));
    mvc.perform(patch(estadoFisico).contentType(MediaType.APPLICATION_JSON)
        .content("{\"estadoHabitacion\":\"DISPONIBLE\"}")).andExpect(status().isConflict());
    mvc.perform(post(enviar).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
        .andExpect(status().isConflict());
    var faltante = Map.of("idItem", item, "estadoEncontrado", "DANADO");
    mvc.perform(post(enviar).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("items", List.of(faltante)))))
        .andExpect(status().isConflict());

    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
    var foto = respuesta(mvc.perform(multipart(API + "/evidencias")
        .file(new MockMultipartFile("archivo", "mesa.png", "image/png", bytes.toByteArray())))
        .andExpect(status().isCreated()));
    mvc.perform(get(foto.path("evidenciaRuta").asText())).andExpect(status().isOk())
        .andExpect(content().bytes(bytes.toByteArray()));
    var reporte = Map.of("items", List.of(Map.of("idItem", item, "estadoEncontrado", "DANADO",
        "observaciones", "Pata suelta", "evidencia", foto)));
    var enviado = respuesta(mvc.perform(post(enviar).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(reporte))).andExpect(status().isOk())
        .andExpect(jsonPath("$.informe.estado").value("POR_APROBAR"))
        .andExpect(jsonPath("$.informe.puedeAprobar").value(false)));
    mvc.perform(post(informe + "/aprobar")).andExpect(status().isConflict());
    mvc.perform(get(API + "/informes").param("nombre", "inf-" + id).param("estado", "POR_APROBAR")
        .param("fechaEmision", enviado.path("informe").path("fechaEnvio").asText().substring(0, 10)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
    long detalle = enviado.path("items").get(0).path("idDetalleInforme").asLong();
    String nuevoTicket = informe + "/items/" + detalle + "/ticket";
    var reparacion = Map.of("accion", Map.of("tipoAccion", "REPARACION", "costoReparacion", 15), "evidencia", foto);
    long primerTicket = respuesta(mvc.perform(post(nuevoTicket).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(reparacion))).andExpect(status().isCreated()))
        .path("ticket").path("idTicket").asLong();
    assertThat(items.obtener(item).estadoActualItem()).isEqualTo(EstadoConservacion.BUENO);
    mvc.perform(get(cuarto)).andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoTickets").value("CON_TICKETS"))
        .andExpect(jsonPath("$.tickets[0].idTicket").value(primerTicket));
    mvc.perform(get(informe)).andExpect(status().isOk())
        .andExpect(jsonPath("$.informe.tieneIncidencias").value(true))
        .andExpect(jsonPath("$.informe.puedeAprobar").value(true))
        .andExpect(jsonPath("$.items[0].estadoEncontrado").value("DANADO"))
        .andExpect(jsonPath("$.items[0].estadoActualItem").value("BUENO"));
    mvc.perform(delete(API + "/tickets/" + primerTicket)).andExpect(status().isNoContent());
    assertThat(items.obtener(item).estadoActualItem()).isEqualTo(EstadoConservacion.DANADO);
    mvc.perform(post(informe + "/aprobar")).andExpect(status().isConflict());

    long idTicket = respuesta(mvc.perform(post(nuevoTicket).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(reparacion))).andExpect(status().isCreated()))
        .path("ticket").path("idTicket").asLong();
    String ticket = API + "/tickets/" + idTicket;
    var reposicion = "{\"tipoAccion\":\"REPOSICION\",\"observacionTecnica\":\"Mesa repuesta\"}";
    mvc.perform(put(ticket).contentType(MediaType.APPLICATION_JSON).content(reposicion))
        .andExpect(status().isOk()).andExpect(jsonPath("$.costoReparacion").doesNotExist());
    mvc.perform(get(API + "/tickets").param("tipoAccion", "REPOSICION").param("nombre", "TKT-" + idTicket)
        .param("numeroHabitacion", "301")).andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1));
    mvc.perform(post(informe + "/aprobar")).andExpect(status().isOk())
        .andExpect(jsonPath("$.informe.estado").value("APROBADO"));
    mvc.perform(get(cuarto)).andExpect(status().isOk())
        .andExpect(jsonPath("$.habitacion.estado").value("DISPONIBLE"))
        .andExpect(jsonPath("$.items[0].estado").value("BUENO"))
        .andExpect(jsonPath("$.ultimoInforme.informe.idInformeRevision").value(id))
        .andExpect(jsonPath("$.tickets[0].idTicket").value(idTicket));
    mvc.perform(delete(informe)).andExpect(status().isConflict());
    mvc.perform(delete(ticket)).andExpect(status().isConflict());
    mvc.perform(put(ticket).contentType(MediaType.APPLICATION_JSON).content(reposicion))
        .andExpect(status().isConflict());
    mvc.perform(get(API + "/inicio")).andExpect(status().isOk())
        .andExpect(jsonPath("$.informesAprobados").value(1))
        .andExpect(jsonPath("$.ticketsGenerados").value(1));
    var auditoria = tx.execute(s -> em.createQuery("""
        select a.accion from AuditoriaGeneral a where a.nombreTabla = 'TICKET_MANTENIMIENTO'
        and a.idRegistro = :id order by a.idAuditoria
        """, AccionAuditoria.class).setParameter("id", idTicket).getResultList());
    assertThat(auditoria).containsExactly(AccionAuditoria.INSERT, AccionAuditoria.UPDATE);

    verificarEliminacionYAprobacionSinIncidencias(cuarto, estadoFisico, foto);
    verificarHistorialTrasRetiro(informe);
  }

  private void verificarHistorialTrasRetiro(String informe) throws Exception {
    Long destino = tx.execute(s -> {
      var cuarto = new Habitacion();
      cuarto.setTipoHabitacion(em.find(Habitacion.class, habitacion).getTipoHabitacion());
      cuarto.setNumeroCuarto("302");
      cuarto.setCapacidadPersonas(1);
      cuarto.setPlazasCama(1);
      cuarto.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);
      em.persist(cuarto);
      return cuarto.getIdHabitacion();
    });
    items.asignar(item, new ItemAsignacionDTO("traslado-prueba", usuarioId, destino,
        EstadoConservacion.BUENO, "Traslado a otra habitación"));
    items.asignar(item, new ItemAsignacionDTO("retiro-prueba", usuarioId, null,
        EstadoConservacion.DANADO, "Retiro del mueble"));

    var historial = items.historial(item);
    assertThat(historial.get(0).accion()).isEqualTo("RETIRO");
    assertThat(historial.get(0).idHabitacionOrigen()).isEqualTo(destino);
    assertThat(historial.get(1).accion()).isEqualTo("TRASLADO");
    assertThat(historial.get(1).idHabitacionOrigen()).isEqualTo(habitacion);
    assertThat(historial.get(1).idHabitacionDestino()).isEqualTo(destino);
    mvc.perform(get(informe)).andExpect(status().isOk())
        .andExpect(jsonPath("$.informe.idHabitacion").value(habitacion))
        .andExpect(jsonPath("$.items[0].idItem").value(item))
        .andExpect(jsonPath("$.items[0].estadoEncontrado").value("DANADO"));
  }

  private void registrarSalida(Habitacion cuarto, Perfil perfil, Usuario usuario) {
    perfil.setEsCliente(1);
    var reserva = new Reserva();
    reserva.setCliente(perfil);
    reserva.setUsuario(usuario);
    reserva.setClaveRegistro("reserva-limpieza");
    reserva.setFirmaSolicitud("firma-limpieza");
    var estancia = new DetalleReserva();
    estancia.setHabitacion(cuarto);
    estancia.setFechaIngreso(LocalDateTime.of(2025, 10, 27, 12, 0));
    estancia.setFechaSalida(LocalDateTime.of(2025, 10, 28, 10, 0));
    estancia.setFechaIngresoReal(LocalDateTime.of(2025, 10, 27, 12, 0));
    estancia.setFechaSalidaReal(LocalDateTime.of(2025, 10, 28, 8, 30));
    estancia.setEstadoDetalle(EstadoReserva.FINALIZADA);
    estancia.setCostoEstadia(BigDecimal.ZERO);
    reserva.agregarDetalle(estancia);
    em.persist(reserva);
  }

  private void verificarEliminacionYAprobacionSinIncidencias(String cuarto, String estadoFisico, JsonNode foto)
      throws Exception {
    mvc.perform(patch(estadoFisico).contentType(MediaType.APPLICATION_JSON)
        .content("{\"estadoHabitacion\":\"LIMPIEZA\"}")).andExpect(status().isOk());
    var borrador = respuesta(mvc.perform(post(cuarto + "/iniciar")).andExpect(status().isOk()));
    long id = borrador.path("idInformeRevision").asLong();
    long asignacionActual = borrador.path("items").get(0).path("idItem").asLong();
    var itemsConIncidencia = List.of(Map.of("idItem", asignacionActual, "estadoEncontrado", "FALTANTE",
        "evidencia", foto));
    var enviado = respuesta(mvc.perform(post(API + "/borradores/" + id + "/enviar").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("items", itemsConIncidencia)))).andExpect(status().isOk()));
    long detalle = enviado.path("items").get(0).path("idDetalleInforme").asLong();
    String informe = API + "/informes/" + id;
    var datosTicket = Map.of("accion", Map.of("tipoAccion", "REPOSICION"), "evidencia", foto);
    long ticket = respuesta(mvc.perform(post(informe + "/items/" + detalle + "/ticket")
        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(datosTicket)))
        .andExpect(status().isCreated())).path("ticket").path("idTicket").asLong();
    mvc.perform(delete(informe)).andExpect(status().isNoContent());
    mvc.perform(get(API + "/tickets/" + ticket)).andExpect(status().isNotFound());
    mvc.perform(get(cuarto)).andExpect(status().isOk())
        .andExpect(jsonPath("$.habitacion.estado").value("POR_LIMPIAR"));

    var siguiente = respuesta(mvc.perform(post(cuarto + "/iniciar")).andExpect(status().isOk()));
    long nuevoId = siguiente.path("idInformeRevision").asLong();
    var sinIncidencia = List.of(Map.of("idItem", asignacionActual, "estadoEncontrado", "BUENO"));
    mvc.perform(post(API + "/borradores/" + nuevoId + "/enviar").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("items", sinIncidencia))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.informe.puedeAprobar").value(true));
    mvc.perform(post(API + "/informes/" + nuevoId + "/aprobar")).andExpect(status().isOk());
  }

  private JsonNode respuesta(ResultActions respuesta) throws Exception {
    return json.readTree(respuesta.andReturn().getResponse().getContentAsByteArray());
  }
}
