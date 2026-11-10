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
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.service.impl.ApisPeruFacturacionClient;
import pe.com.rebolsysBackend.service.service.EmisionElectronicaService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties={"spring.jpa.show-sql=false","logging.level.pe.com.rebolsysBackend=INFO",
    "facturacion.apisperu.habilitada=true","facturacion.apisperu.url=http://127.0.0.1:4920/facturacion/api/v1",
    "facturacion.apisperu.token=",
    "facturacion.apisperu.usuario=prueba","facturacion.apisperu.password=solo-simulacion",
    "facturacion.apisperu.id-empresa=1","facturacion.apisperu.ruc=20123456789",
    "facturacion.apisperu.razon-social=HOSPEDAJE FICTICIO","facturacion.apisperu.direccion=DIRECCION FICTICIA",
    "facturacion.apisperu.ubigeo=150101","facturacion.apisperu.departamento=LIMA",
    "facturacion.apisperu.provincia=LIMA","facturacion.apisperu.distrito=LIMA","facturacion.apisperu.timeout-millis=1000"})
@MockMvcReglasNegocio @Transactional
@EnabledIfEnvironmentVariable(named="RUN_FACTURACION_SIM_TESTS",matches="true")
class FacturacionElectronicaOracleTest {
  @Autowired EntityManager em;
  @Autowired EmisionElectronicaService servicio;
  @Autowired FacturacionApisPeruProperties config;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  Comprobante cuenta;
  Long usuario;

  @BeforeEach void preparar() {
    String sufijo=UUID.randomUUID().toString().substring(0,8);
    var td=new TipoDocumento();td.setNombreDocumento("CPE_"+sufijo);td.setCodigoSunat("1");em.persist(td);
    var p=new Perfil();p.setTipoDocumento(td);p.setNumeroDocumento("11111111");em.persist(p);
    var n=new PersonaNatural();n.setPerfil(p);n.setNombres("PERSONA FICTICIA");n.setApellidos("PRUEBA");n.setFechaNacimiento(LocalDate.of(1990,1,1));em.persist(n);
    p.setEsCliente(1);
    var empleado=new Empleado();empleado.setPersonaNatural(n);empleado.setFechaContratacion(LocalDate.now());em.persist(empleado);
    var u=new Usuario();u.setEmpleado(empleado);u.setUsername("cpe_"+sufijo);u.setEstadoUsuario(EstadoUsuario.ACTIVO);em.persist(u);usuario=u.getIdUsuario();
    var t=new TurnoCaja();t.setUsuario(u);t.setEstadoSesion(1);em.persist(t);
    var reserva=new Reserva();reserva.setCliente(p);reserva.setUsuario(u);reserva.setClaveRegistro(UUID.randomUUID().toString());reserva.setFirmaSolicitud("0".repeat(64));em.persist(reserva);
    var tipo=em.createQuery("from TipoComprobante where codigo='BOLETA'",TipoComprobante.class).getSingleResult();
    var id=new SerieComprobanteId(tipo.getIdTipoComprobante(),"BTST");
    if (em.find(SerieComprobante.class,id)==null) {var serie=new SerieComprobante();serie.setId(id);serie.setTipoComprobante(tipo);em.persist(serie);}
    cuenta=new Comprobante();cuenta.setReserva(reserva);cuenta.setCliente(p);cuenta.setTipoComprobante(tipo);cuenta.setSerie("BTST");cuenta.setTurnoCaja(t);
    cuenta.setSubtotal(new BigDecimal("118"));cuenta.setIgv(BigDecimal.ZERO);cuenta.setTotalPagar(new BigDecimal("118"));cuenta.setEstadoComprobante(EstadoComprobante.BORRADOR);em.persist(cuenta);
    cuenta.setNumeroComprobante(cuenta.getIdComprobante().toString());cuenta.setFechaEmision(LocalDateTime.now());cuenta.setEstadoComprobante(EstadoComprobante.EMITIDO);
    cuenta.setReceptorNombre("PERSONA FICTICIA");cuenta.setReceptorTipoDoc("1");cuenta.setReceptorNumeroDoc("11111111");
    var th=new TipoHabitacion();th.setNombreTipo("CPE "+sufijo);th.setCapacidadMax(2);th.setPrecioBase(new BigDecimal("118"));em.persist(th);
    var h=new Habitacion();h.setTipoHabitacion(th);h.setNumeroCuarto("E"+sufijo);h.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);h.setCapacidadPersonas(2);h.setPlazasCama(2);h.setMaxMenoresSinCama(0);em.persist(h);
    var dr=new DetalleReserva();dr.setReserva(reserva);dr.setHabitacion(h);dr.setFechaIngreso(LocalDateTime.now());dr.setFechaSalida(LocalDateTime.now().plusDays(1));dr.setCostoEstadia(new BigDecimal("118"));dr.setEstadoDetalle(EstadoReserva.CONFIRMADA);em.persist(dr);
    var l=new ComprobanteDetalle();l.setComprobante(cuenta);l.setDetalleReserva(dr);l.setNumeroLinea(1);l.setDescripcion("ALOJAMIENTO FICTICIO");l.setCantidad(BigDecimal.ONE);l.setValorUnitario(new BigDecimal("118"));l.setIgv(BigDecimal.ZERO);em.persist(l);em.flush();
  }
  EmisionElectronicaRequestDTO solicitud() {
    return new EmisionElectronicaRequestDTO("cpe-"+cuenta.getIdComprobante(),usuario,new BigDecimal("18"),null,null,
        List.of(new EmisionElectronicaRequestDTO.Cuota(LocalDate.now().plusDays(1),new BigDecimal("118"))));
  }
  void receptor(String dni) {cuenta.setReceptorNumeroDoc(dni);em.flush();}
  OperacionUsuarioDTO operador() {return new OperacionUsuarioDTO(usuario);}

  @Test void preparaEmiteYReintentaSinDuplicarNiCrearCobros() throws Exception {
    var preparado=servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.preparar(cuenta.getIdComprobante(),solicitud()).idEmision()).isEqualTo(preparado.idEmision());
    var enviada=servicio.enviar(cuenta.getIdComprobante(),operador());
    assertThat(enviada.estado()).isEqualTo(EstadoEmisionElectronica.ACEPTADO);
    assertThat(enviada.subtotal()).isEqualByComparingTo("100");assertThat(enviada.igv()).isEqualByComparingTo("18");
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).intentos()).isEqualTo(1);
    assertThat(em.createQuery("select count(p) from Pago p where p.comprobante.idComprobante=:id",Long.class).setParameter("id",cuenta.getIdComprobante()).getSingleResult()).isZero();
    mvc.perform(get("/api/v1/comprobantes/"+cuenta.getIdComprobante()+"/emision-electronica/xml"))
        .andExpect(status().isOk()).andExpect(header().string("X-Comprobante-Ambiente","BETA"));
    assertThat(servicio.archivo(cuenta.getIdComprobante(),"cdr")).startsWith((byte)'P',(byte)'K');
    assertThat(new String(servicio.archivo(cuenta.getIdComprobante(),"pdf"),java.nio.charset.StandardCharsets.UTF_8)).startsWith("%PDF-");
  }
  @Test void unRechazoCdrNoSePresentaComoAceptacion() {
    receptor("22222222");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.RECHAZADO);
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).intentos()).isEqualTo(1);
  }
  @Test void timeoutDespuesDelEnvioSeResuelveConsultandoSinReenviar() {
    receptor("33333333");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.INCIERTO);
    assertThatThrownBy(()->servicio.enviar(cuenta.getIdComprobante(),operador())).hasMessageContaining("Consulta");
    assertThat(servicio.consultar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.ACEPTADO);
    assertThat(servicio.obtener(cuenta.getIdComprobante()).intentos()).isEqualTo(1);
  }
  @Test void credencialRechazadaNoExponeSecretos() {
    receptor("44444444");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    var e=servicio.enviar(cuenta.getIdComprobante(),operador());
    assertThat(e.estado()).isEqualTo(EstadoEmisionElectronica.ERROR_ENVIO);
    assertThat(e.mensajeRespuesta()).doesNotContain("solo-simulacion","token-facturacion-local");
  }
  @Test void http200SinCdrNoEsUnaAceptacion() {
    receptor("55555555");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.INCIERTO);
    assertThat(servicio.consultar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.ACEPTADO);
  }
  @Test void respuestaMalformadaNoPermiteReenvioCiego() {
    receptor("66666666");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.INCIERTO);
    assertThatThrownBy(()->servicio.enviar(cuenta.getIdComprobante(),operador())).hasMessageContaining("Consulta");
  }
  @Test void cdrDeOtroDocumentoRequiereRevision() {
    receptor("88888888");servicio.preparar(cuenta.getIdComprobante(),solicitud());
    assertThat(servicio.enviar(cuenta.getIdComprobante(),operador()).estado()).isEqualTo(EstadoEmisionElectronica.INCIERTO);
  }
  @Test void noPermiteEmpresaEnProduccion() {
    var p=new FacturacionApisPeruProperties();p.setUrl("http://127.0.0.1:4920/facturacion/api/v1");p.setIdEmpresa(2L);p.setHabilitada(true);p.setToken("token-facturacion-local");
    var cliente=new ApisPeruFacturacionClient(p,json);
    assertThatThrownBy(()->cliente.enviar("{}","20123456789","03","BINT",1)).hasMessageContaining("beta");
  }
  @Test void validacionHttpYArchivoNoDisponibleSeRespetan() throws Exception {
    mvc.perform(post("/api/v1/comprobantes/"+cuenta.getIdComprobante()+"/emision-electronica")
        .contentType(MediaType.APPLICATION_JSON).content("{\"claveOperacion\":\"prueba\",\"idUsuario\":"+usuario+",\"tasaIgv\":-1}"))
        .andExpect(status().isBadRequest());
    servicio.preparar(cuenta.getIdComprobante(),solicitud());
    mvc.perform(get("/api/v1/comprobantes/"+cuenta.getIdComprobante()+"/emision-electronica/xml")).andExpect(status().isNotFound());
    var cambiado=new EmisionElectronicaRequestDTO(solicitud().claveOperacion(),usuario,new BigDecimal("10.5"),null,null,solicitud().cuotas());
    assertThatThrownBy(()->servicio.preparar(cuenta.getIdComprobante(),cambiado)).hasMessageContaining("clave");
  }
}
