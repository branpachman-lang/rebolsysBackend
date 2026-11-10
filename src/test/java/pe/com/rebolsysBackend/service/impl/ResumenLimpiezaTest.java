package pe.com.rebolsysBackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.EstadoInforme;
import pe.com.rebolsysBackend.repository.*;

class ResumenLimpiezaTest {
  final SesionOperacionService sesion = mock(SesionOperacionService.class);
  final InformeRevisionRepository informes = mock(InformeRevisionRepository.class);
  final TicketMantenimientoRepository tickets = mock(TicketMantenimientoRepository.class);
  final TurnoCajaRepository turnos = mock(TurnoCajaRepository.class);
  final ReglasPropuesta reglas = mock(ReglasPropuesta.class);
  LimpiezaServiceImpl servicio;

  @BeforeEach void preparar() {
    servicio = new LimpiezaServiceImpl(mock(HabitacionRepository.class), mock(DetalleReservaRepository.class),
        mock(ItemRepository.class), informes, mock(DetalleInformeRepository.class), tickets, turnos,
        sesion, reglas, mock(ItemServiceImpl.class), mock(EvidenciaLimpiezaService.class));
  }

  @Test void gerenteSinTurnoVeTotalesDeTodoElPersonal() {
    when(sesion.actual()).thenReturn(cuenta(true, null, List.of("GERENTE", "RECEPCIONISTA")));
    when(informes.countByEstadoInforme(EstadoInforme.POR_APROBAR)).thenReturn(4L);
    when(informes.countByEstadoInforme(EstadoInforme.APROBADO)).thenReturn(9L);
    when(tickets.count()).thenReturn(6L);
    var resumen = servicio.inicio();
    assertThat(resumen.alcance()).isEqualTo("GENERAL");
    assertThat(resumen.informesEnviados()).isEqualTo(13);
    assertThat(resumen.informesPorAprobar()).isEqualTo(4);
    assertThat(resumen.ticketsGenerados()).isEqualTo(6);
    assertThat(resumen.idTurno()).isNull();
    verifyNoInteractions(turnos, reglas);
  }

  @Test void limpiezaSinTurnoConsultaSuActividadDeHoy() {
    when(sesion.actual()).thenReturn(cuenta(false, null, List.of("LIMPIEZA")));
    var usuario = new Usuario();
    var empleado = new Empleado(); empleado.setIdEmpleado(7L); usuario.setEmpleado(empleado);
    when(reglas.usuario(3L)).thenReturn(usuario);
    when(informes.countByUsuarioIdUsuarioAndEstadoInformeAndFechaHoraBetween(eq(3L), eq(EstadoInforme.POR_APROBAR), any(), any())).thenReturn(2L);
    when(tickets.countByEmpleadoIdEmpleadoAndFechaGeneracionBetween(eq(7L), any(), any())).thenReturn(1L);
    var resumen = servicio.inicio();
    assertThat(resumen.alcance()).isEqualTo("DIA");
    assertThat(resumen.informesEnviados()).isEqualTo(2);
    assertThat(resumen.ticketsGenerados()).isEqualTo(1);
    assertThat(resumen.fechaApertura().toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
    verifyNoInteractions(turnos);
  }

  @Test void recepcionistaSinTurnoRecibeSuResumenDeTurnoVacio() {
    when(sesion.actual()).thenReturn(cuenta(false, null, List.of("RECEPCIONISTA")));
    var resumen = servicio.inicio();
    assertThat(resumen.alcance()).isEqualTo("TURNO");
    assertThat(resumen.informesEnviados()).isZero();
    verifyNoInteractions(informes, tickets, turnos);
  }

  @Test void recepcionistaConTurnoUsaSuHoraDeApertura() {
    when(sesion.actual()).thenReturn(cuenta(false, 12L, List.of("RECEPCIONISTA")));
    var turno = new TurnoCaja(); turno.setIdTurno(12L);
    turno.setFechaApertura(ReglasPropuesta.ahora().minusHours(2));
    when(turnos.findById(12L)).thenReturn(Optional.of(turno));
    var usuario = new Usuario(); var empleado = new Empleado(); empleado.setIdEmpleado(7L); usuario.setEmpleado(empleado);
    when(reglas.usuario(3L)).thenReturn(usuario);
    var resumen = servicio.inicio();
    assertThat(resumen.alcance()).isEqualTo("TURNO");
    assertThat(resumen.fechaApertura()).isEqualTo(turno.getFechaApertura());
    assertThat(resumen.idTurno()).isEqualTo(12L);
    verify(informes).countByUsuarioIdUsuarioAndEstadoInformeAndFechaHoraBetween(eq(3L), eq(EstadoInforme.POR_APROBAR), eq(turno.getFechaApertura()), any());
  }

  private SesionActualDTO cuenta(boolean gerente, Long turno, List<String> roles) {
    return new SesionActualDTO(3L, "prueba", "ACTIVA", turno, null, false, gerente, roles, List.of());
  }
}
