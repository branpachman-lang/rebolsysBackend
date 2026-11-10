package pe.com.rebolsysBackend.service.impl;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.repository.*;

class SesionOperacionServiceTest {
  UsuarioRepository usuarios;TurnoCajaRepository turnos;SesionOperacionService servicio;Usuario u;
  @BeforeEach void preparar(){
    usuarios=mock(UsuarioRepository.class);turnos=mock(TurnoCajaRepository.class);var pagos=mock(FormaPagoRepository.class);
    servicio=new SesionOperacionService(usuarios,turnos,pagos,mock(CredencialesService.class),mock(AccesosService.class));ReflectionTestUtils.setField(servicio,"habilitada",true);ReflectionTestUtils.setField(servicio,"username","operacion_local");
    u=new Usuario();u.setIdUsuario(130L);u.setUsername("operacion_local");u.setEstadoUsuario(EstadoUsuario.ACTIVO);when(usuarios.findByUsername("operacion_local")).thenReturn(Optional.of(u));
    var t=new TurnoCaja();t.setIdTurno(101L);when(turnos.findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(130L,1)).thenReturn(List.of(t));
    var p=new FormaPago();p.setIdFormaPago(1L);p.setNombrePago("EFECTIVO");when(pagos.findAll()).thenReturn(List.of(p));
  }
  @Test void devolverElUsuarioConfiguradoConSuTurno(){var s=servicio.actual();assertThat(s.idUsuario()).isEqualTo(130L);assertThat(s.usuario()).isEqualTo("operacion_local");assertThat(s.idTurno()).isEqualTo(101L);assertThat(s.idFormaPagoEfectivo()).isEqualTo(1L);}
  @Test void usuarioInactivoNoPuedeOperar(){u.setEstadoUsuario(EstadoUsuario.INACTIVO);assertThatThrownBy(()->servicio.actual()).hasMessageContaining("activo");}
  @Test void noElegirOtroUsuarioAlFaltarElConfigurado(){when(usuarios.findByUsername("operacion_local")).thenReturn(Optional.empty());assertThatThrownBy(()->servicio.actual()).hasMessageContaining("no existe");}
}
