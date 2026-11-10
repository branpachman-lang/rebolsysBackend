package pe.com.rebolsysBackend.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import pe.com.rebolsysBackend.dto.HabitacionRequestDTO;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

class HabitacionTarifaTest {
  private final HabitacionRepository habitaciones = mock(HabitacionRepository.class);
  private final TipoHabitacionRepository tipos = mock(TipoHabitacionRepository.class);
  private final DetalleReservaRepository detalles = mock(DetalleReservaRepository.class);
  private final ReservaOcupanteRepository ocupantes = mock(ReservaOcupanteRepository.class);
  private final HabitacionServiceImpl servicio = new HabitacionServiceImpl(
      mock(EntityManager.class), habitaciones, tipos, detalles, ocupantes,
      mock(ClienteService.class), mock(ReglasPropuesta.class),
      mock(ItemRepository.class), mock(MovimientoInventarioRepository.class), mock(ItemService.class), mock(InformeRevisionRepository.class));
  private TipoHabitacion tipo;
  private Habitacion habitacion;

  @BeforeEach
  void preparar() {
    tipo = new TipoHabitacion();
    tipo.setIdTipoHabitacion(1L); tipo.setNombreTipo("Simple con baño");
    tipo.setCapacidadMax(2); tipo.setPrecioBase(new BigDecimal("80.00"));
    habitacion = crear(1L, "402", new BigDecimal("50.00"));
    when(habitaciones.findById(1L)).thenReturn(Optional.of(habitacion));
    when(habitaciones.bloquear(1L)).thenReturn(Optional.of(habitacion));
    when(tipos.bloquear(1L)).thenReturn(Optional.of(tipo));
    when(detalles.findByHabitacionIdHabitacion(1L)).thenReturn(List.of());
    when(ocupantes.activosHabitacion(1L)).thenReturn(List.of());
    when(habitaciones.saveAndFlush(any(Habitacion.class))).thenAnswer(i -> i.getArgument(0));
  }

  private Habitacion crear(Long id, String numero, BigDecimal precio) {
    var h = new Habitacion(); h.setIdHabitacion(id); h.setNumeroCuarto(numero);
    h.setTipoHabitacion(tipo); h.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);
    h.setCapacidadPersonas(1); h.setPlazasCama(1); h.setMaxMenoresSinCama(0);
    h.setPrecioPorNoche(precio); return h;
  }

  @Test
  void habitacionesDelMismoTipoMuestranPreciosDistintosEnElCalendario() {
    var otra = crear(2L, "506", new BigDecimal("40.00"));
    when(habitaciones.findAll(any(Sort.class))).thenReturn(List.of(habitacion, otra));
    when(detalles.buscarEnPeriodo(any(), any())).thenReturn(List.of());
    var estado = servicio.estado(LocalDate.of(2026,11,3), LocalDate.of(2026,11,4));
    assertThat(estado).extracting(r -> r.precio()).containsExactly(new BigDecimal("50.00"), new BigDecimal("40.00"));
    assertThat(tipo.getPrecioBase()).isEqualByComparingTo("80.00");
  }

  @Test
  void conservaLaTarifaDelTipoSiLaHabitacionNoTienePrecioPropio() {
    habitacion.setPrecioPorNoche(null);
    var respuesta = servicio.read(1L);
    assertThat(respuesta.precioBase()).isEqualByComparingTo("80.00");
    assertThat(respuesta.precioPorNoche()).isNull();
  }

  @Test
  void elCeroExplicitoNoSeSustituyePorElPrecioDelTipo() {
    habitacion.setPrecioPorNoche(BigDecimal.ZERO);
    assertThat(servicio.read(1L).precioBase()).isEqualByComparingTo("0.00");
  }

  @Test
  void unClienteAnteriorPuedeEditarOcupacionSinBorrarLaTarifa() {
    var respuesta = servicio.update(1L, new HabitacionRequestDTO("402",1L,EstadoHabitacion.DISPONIBLE,1,1,0));
    assertThat(respuesta.precioPorNoche()).isEqualByComparingTo("50.00");
    assertThat(respuesta.precioBase()).isEqualByComparingTo("50.00");
  }

  @Test
  void actualizarLaTarifaNoModificaLosImportesDeReservasExistentes() {
    var reserva = new DetalleReserva(); reserva.setEstadoDetalle(EstadoReserva.CONFIRMADA);
    reserva.setCostoEstadia(new BigDecimal("100.00"));
    when(detalles.findByHabitacionIdHabitacion(1L)).thenReturn(List.of(reserva));
    var respuesta = servicio.update(1L, new HabitacionRequestDTO("402",1L,EstadoHabitacion.DISPONIBLE,1,1,0,new BigDecimal("60.00")));
    assertThat(respuesta.precioBase()).isEqualByComparingTo("60.00");
    assertThat(tipo.getPrecioBase()).isEqualByComparingTo("80.00");
    assertThat(reserva.getCostoEstadia()).isEqualByComparingTo("100.00");
  }

  @Test
  void validaLosDecimalesYLosLimitesSinHacerObligatoriaLaTarifaPropia() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      for (String precio : List.of("-1", "50.001", "100000000"))
        assertThat(validator.validate(new HabitacionRequestDTO("402",1L,EstadoHabitacion.DISPONIBLE,1,1,0,new BigDecimal(precio)))).isNotEmpty();
      for (BigDecimal precio : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("50.25")})
        assertThat(validator.validate(new HabitacionRequestDTO("402",1L,EstadoHabitacion.DISPONIBLE,1,1,0,precio))).isEmpty();
    }
  }
}
