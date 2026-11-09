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
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.service.service.GastoService;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "logging.level.pe.com.rebolsysBackend=INFO"})
@MockMvcReglasNegocio
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
class GastosOracleTest {
  @Autowired EntityManager em;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired GastoService gastos;
  Usuario recepcionista, otro;
  TurnoCaja turno, otroTurno;
  TipoMovimientoCaja taxi, repuestos, ingreso;

  @BeforeEach
  void preparar() {
    var td = new TipoDocumento();
    td.setNombreDocumento("GASTO" + token());
    td.setCodigoSunat("1");
    em.persist(td);
    recepcionista = usuario(td);
    otro = usuario(td);
    turno = turno(recepcionista);
    otroTurno = turno(otro);
    taxi = tipo("Taxi " + token(), -1);
    repuestos = tipo("Repuestos " + token(), -1);
    ingreso = tipo("Ingreso " + token(), 1);
    em.flush();
  }

  String token() { return UUID.randomUUID().toString().substring(0, 8); }

  Usuario usuario(TipoDocumento td) {
    var p = new Perfil();
    p.setTipoDocumento(td);
    p.setNumeroDocumento(token());
    em.persist(p);
    var persona = new PersonaNatural();
    persona.setPerfil(p);
    persona.setNombres("Recepcionista");
    persona.setApellidos("De prueba");
    persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
    em.persist(persona);
    var e = new Empleado();
    e.setPersonaNatural(persona);
    e.setFechaContratacion(LocalDate.now());
    em.persist(e);
    var u = new Usuario();
    u.setEmpleado(e);
    u.setUsername("gastos" + token());
    u.setEstadoUsuario(EstadoUsuario.ACTIVO);
    em.persist(u);
    return u;
  }

  TurnoCaja turno(Usuario u) {
    var t = new TurnoCaja();
    t.setUsuario(u);
    t.setEstadoSesion(1);
    em.persist(t);
    return t;
  }

  TipoMovimientoCaja tipo(String nombre, int signo) {
    var t = new TipoMovimientoCaja();
    t.setNombreMovimiento(nombre);
    t.setSigno(signo);
    em.persist(t);
    return t;
  }

  FlujoCaja movimiento(TurnoCaja turno, TipoMovimientoCaja tipo, String concepto, String monto, String fecha) {
    var f = new FlujoCaja();
    f.setTurnoCaja(turno);
    f.setTipoMovimientoCaja(tipo);
    f.setConcepto(concepto);
    f.setMonto(new BigDecimal(monto));
    f.setFechaHora(LocalDateTime.parse(fecha));
    em.persist(f);
    em.flush();
    return f;
  }

  GastoFiltroDTO filtro(Long t, Long usuario, Long tipo, LocalDate desde, LocalDate hasta,
      String buscar, String minimo, String maximo, Integer pagina, Integer tamanio) {
    return new GastoFiltroDTO(t, usuario, tipo, desde, hasta, buscar,
        minimo == null ? null : new BigDecimal(minimo), maximo == null ? null : new BigDecimal(maximo), pagina, tamanio);
  }

  GastoFiltroDTO delTurno() {
    return filtro(turno.getIdTurno(), null, null, null, null, null, null, null, null, null);
  }

  GastoRequestDTO solicitud(String clave, String monto) {
    return new GastoRequestDTO(recepcionista.getIdUsuario(), turno.getIdTurno(), taxi.getIdTipoMovimiento(),
        "Taxi para conseguir repuestos", new BigDecimal(monto), "B001-9", null, null, clave);
  }

  @Test
  void historialPaginadoOrdenaPorFechaEIdYResumenIncluyeTodasLasPaginas() throws Exception {
    var primero = movimiento(turno, taxi, "Primero", "10", "2026-11-02T09:00:00");
    var segundo = movimiento(turno, taxi, "Segundo", "20", "2026-11-02T10:00:00");
    var tercero = movimiento(turno, repuestos, "Tercero", "30", "2026-11-02T10:00:00");
    mvc.perform(get("/api/v1/gastos/historial").param("idTurno", turno.getIdTurno().toString()).param("tamanio", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.gastos.contenido[0].idFlujo").value(tercero.getIdFlujo()))
        .andExpect(jsonPath("$.gastos.totalElementos").value(3)).andExpect(jsonPath("$.gastos.totalPaginas").value(3))
        .andExpect(jsonPath("$.resumen.cantidadGastos").value(3)).andExpect(jsonPath("$.resumen.totalGastado").value(60))
        .andExpect(jsonPath("$.gastos.contenido[0].usuario").value(recepcionista.getUsername()))
        .andExpect(jsonPath("$.gastos.contenido[0].tipoGasto").value(repuestos.getNombreMovimiento()));
    var pagina = gastos.historial(filtro(turno.getIdTurno(), null, null, null, null, null, null, null, 1, 1));
    assertThat(pagina.gastos().getContenido().getFirst().idFlujo()).isEqualTo(segundo.getIdFlujo());
    assertThat(pagina.resumen().totalGastado()).isEqualByComparingTo("60");
    assertThat(primero.getIdFlujo()).isLessThan(tercero.getIdFlujo());
  }

  @Test
  void fechasIncluyenElDiaCompletoYAdmitenSoloUnExtremo() {
    movimiento(turno, taxi, "Antes", "1", "2026-11-01T23:59:59.999999");
    movimiento(turno, taxi, "Inicio", "10", "2026-11-02T00:00:00");
    movimiento(turno, taxi, "Final", "20", "2026-11-02T23:59:59.999999");
    movimiento(turno, taxi, "Después", "2", "2026-11-03T00:00:00");
    var dia = LocalDate.of(2026, 11, 2);
    var h = gastos.historial(filtro(turno.getIdTurno(), null, null, dia, dia, null, null, null, null, null));
    assertThat(h.resumen().cantidadGastos()).isEqualTo(2);
    assertThat(h.resumen().totalGastado()).isEqualByComparingTo("30");
    assertThat(gastos.historial(filtro(turno.getIdTurno(), null, null, dia, null, null, null, null, null, null))
        .resumen().totalGastado()).isEqualByComparingTo("32");
    assertThat(gastos.historial(filtro(turno.getIdTurno(), null, null, null, dia, null, null, null, null, null))
        .resumen().totalGastado()).isEqualByComparingTo("31");
  }

  @Test
  void filtrosSeCombinanPorResponsableCategoriaTextoEImportesInclusivos() {
    var esperado = movimiento(turno, taxi, "Taxi a ferretería", "10", "2026-11-02T12:00:00");
    esperado.setDocumentoRespaldo("B001-900");
    movimiento(turno, repuestos, "Compra a ferretería", "10", "2026-11-02T12:00:00");
    movimiento(turno, taxi, "Otro taxi", "20", "2026-11-02T12:00:00");
    movimiento(otroTurno, taxi, "Taxi a ferretería", "10", "2026-11-02T12:00:00");
    em.flush();
    var h = gastos.historial(filtro(turno.getIdTurno(), recepcionista.getIdUsuario(), taxi.getIdTipoMovimiento(),
        null, null, "  b001-900  ", "10", "10", null, null));
    assertThat(h.gastos().getContenido()).extracting(GastoResponseDTO::idFlujo).containsExactly(esperado.getIdFlujo());
    assertThat(h.resumen().totalGastado()).isEqualByComparingTo("10");
    assertThat(gastos.historial(filtro(null, otro.getIdUsuario(), null, null, null, null, null, null, null, null))
        .resumen().cantidadGastos()).isEqualTo(1);
  }

  @Test
  void buscarPorcentajeGuionBajoYBarraLosTrataComoTextoLiteral() {
    var especial = movimiento(turno, taxi, "Oferta 10%_local\\Taxi", "10", "2026-11-02T12:00:00");
    movimiento(turno, taxi, "Oferta 100Xlocal", "20", "2026-11-02T12:00:00");
    for (String buscar : List.of("%", "_", "\\")) {
      var h = gastos.historial(filtro(turno.getIdTurno(), null, null, null, null, buscar, null, null, null, null));
      assertThat(h.gastos().getContenido()).extracting(GastoResponseDTO::idFlujo).containsExactly(especial.getIdFlujo());
    }
  }

  @Test
  void noSeMezclanIngresosYElDetalleSoloAceptaGastos() {
    movimiento(turno, taxi, "Salida", "5", "2026-11-02T12:00:00");
    var cobro = movimiento(turno, ingreso, "Ingreso", "100", "2026-11-02T12:00:00");
    var h = gastos.historial(delTurno());
    assertThat(h.resumen().cantidadGastos()).isEqualTo(1);
    assertThat(h.resumen().totalGastado()).isEqualByComparingTo("5");
    assertThatThrownBy(() -> gastos.obtener(cobro.getIdFlujo())).isInstanceOf(RecursoNoEncontradoException.class);
    assertThat(gastos.tipos()).extracting(CatalogoItemResponseDTO::getId).contains(taxi.getIdTipoMovimiento())
        .doesNotContain(ingreso.getIdTipoMovimiento());
  }

  @Test
  void resultadoVacioYPaginaFueraDeRangoConservanResumenCorrecto() throws Exception {
    var vacio = gastos.historial(delTurno());
    assertThat(vacio.gastos().getContenido()).isEmpty();
    assertThat(vacio.resumen().cantidadGastos()).isZero();
    assertThat(vacio.resumen().totalGastado()).isZero();
    gastos.registrar(solicitud(UUID.randomUUID().toString(), "12.50"));
    var fuera = gastos.historial(filtro(turno.getIdTurno(), null, null, null, null, null, null, null, 10, 1));
    assertThat(fuera.gastos().getContenido()).isEmpty();
    assertThat(fuera.resumen().totalGastado()).isEqualByComparingTo("12.50");
    mvc.perform(get("/api/v1/gastos/historial")).andExpect(status().isOk())
        .andExpect(jsonPath("$.gastos.tamanio").value(20));
  }

  @Test
  void registroEsIdempotenteYSePuedeReconsultarDespuesDelCierre() throws Exception {
    var req = solicitud(UUID.randomUUID().toString(), "15.50");
    var creado = gastos.registrar(req);
    var repetido = gastos.registrar(req);
    assertThat(repetido.idFlujo()).isEqualTo(creado.idFlujo());
    assertThat(gastos.historial(delTurno()).resumen().cantidadGastos()).isEqualTo(1);
    turno.setEstadoSesion(0);
    turno.setFechaCierre(LocalDateTime.now(ZoneId.of("America/Lima")));
    em.flush();
    assertThat(gastos.registrar(req).turnoAbierto()).isFalse();
    recepcionista.setEstadoUsuario(EstadoUsuario.INACTIVO);
    em.flush();
    var h = gastos.historial(filtro(null, recepcionista.getIdUsuario(), null, null, null, null, null, null, null, null));
    assertThat(h.resumen().cantidadGastos()).isEqualTo(1);
    assertThat(h.gastos().getContenido().getFirst().turnoAbierto()).isFalse();
  }

  @Test
  void mismaClaveConOtroContenidoEsRechazadaSinNuevoGasto() {
    String clave = UUID.randomUUID().toString();
    gastos.registrar(solicitud(clave, "10.00"));
    assertThatThrownBy(() -> gastos.registrar(solicitud(clave, "20")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("otro gasto");
    assertThat(gastos.historial(delTurno()).resumen().totalGastado()).isEqualByComparingTo("10");
  }

  @Test
  void guardarConTurnoAjenoOCerradoSeRechaza() {
    assertThatThrownBy(() -> gastos.registrar(new GastoRequestDTO(recepcionista.getIdUsuario(),
        otroTurno.getIdTurno(), taxi.getIdTipoMovimiento(), "Taxi", BigDecimal.ONE, null, null, null, UUID.randomUUID().toString())))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("otro usuario");
    turno.setEstadoSesion(0);
    turno.setFechaCierre(LocalDateTime.now(ZoneId.of("America/Lima")));
    em.flush();
    assertThatThrownBy(() -> gastos.registrar(solicitud(UUID.randomUUID().toString(), "10")))
        .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("abierto");
    assertThat(gastos.historial(delTurno()).resumen().cantidadGastos()).isZero();
  }

  @Test
  void evidenciaNormalizaHuellaYFechaSeFijaAlRegistrar() {
    String clave = UUID.randomUUID().toString();
    var antes = LocalDateTime.now(ZoneId.of("America/Lima")).minusSeconds(1);
    var req = new GastoRequestDTO(recepcionista.getIdUsuario(), turno.getIdTurno(), taxi.getIdTipoMovimiento(),
        " Taxi ", new BigDecimal("10"), " B001-1 ", " evidencia/taxi.jpg ", "A".repeat(64), clave);
    var gasto = gastos.registrar(req);
    assertThat(gasto.fechaHora()).isAfterOrEqualTo(antes);
    assertThat(gasto.fechaHora()).isBeforeOrEqualTo(LocalDateTime.now(ZoneId.of("America/Lima")));
    assertThat(gasto.concepto()).isEqualTo("Taxi");
    assertThat(gasto.evidenciaSha256()).isEqualTo("a".repeat(64));
    var repetido = gastos.registrar(new GastoRequestDTO(recepcionista.getIdUsuario(), turno.getIdTurno(),
        taxi.getIdTipoMovimiento(), "Taxi", new BigDecimal("10.00"), "B001-1", "evidencia/taxi.jpg", "a".repeat(64), clave));
    assertThat(repetido.idFlujo()).isEqualTo(gasto.idFlujo());
  }

  @Test
  void validarFiltrosYReferenciasDesdeHttp() throws Exception {
    for (var parametros : List.of(
        Map.of("pagina", "-1"), Map.of("tamanio", "101"), Map.of("idTurno", "0"),
        Map.of("desde", "2026-11-03", "hasta", "2026-11-02"), Map.of("desde", "no-es-fecha"),
        Map.of("montoMinimo", "-1"), Map.of("montoMinimo", "20", "montoMaximo", "10"),
        Map.of("tamanio", "texto"))) {
      var request = get("/api/v1/gastos/historial");
      parametros.forEach(request::param);
      mvc.perform(request).andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/v1/gastos/historial").param("idTurno", "9223372036854775807"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/gastos/historial").param("idTipoMovimiento", ingreso.getIdTipoMovimiento().toString()))
        .andExpect(status().isConflict());
  }

  @Test
  void registrarDesdeHttpRechazaMontoCeroYEvidenciaIncompletaYConservaCompatibilidad() throws Exception {
    var valido = solicitud(null, "3.00");
    var primero = gastos.registrar(valido);
    var segundo = gastos.registrar(valido);
    assertThat(primero.idFlujo()).isNotEqualTo(segundo.idFlujo());
    for (var req : List.of(solicitud("cero", "0"),
        new GastoRequestDTO(recepcionista.getIdUsuario(), turno.getIdTurno(), taxi.getIdTipoMovimiento(),
            "Sin huella", BigDecimal.ONE, null, "foto.jpg", null, "sin-huella"))) {
      mvc.perform(post("/api/v1/gastos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
          .andExpect(req.monto().signum() == 0 ? status().isBadRequest() : status().isConflict());
    }
  }
}
