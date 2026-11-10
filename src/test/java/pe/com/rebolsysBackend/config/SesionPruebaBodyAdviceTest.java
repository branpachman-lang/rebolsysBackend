package pe.com.rebolsysBackend.config;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import jakarta.validation.Valid;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.exception.GlobalExceptionHandler;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;
import tools.jackson.databind.json.JsonMapper;

class SesionPruebaBodyAdviceTest {
  SesionOperacionService sesion;MockMvc mvc;
  @RestController static class Operaciones {
    @PostMapping("/api/v1/ventas/confirmadas") public VentaRequestDTO venta(@Valid @RequestBody VentaRequestDTO r){return r;}
    @PostMapping("/api/v1/ventas/1/pagos") public PagoCuentaDTO pago(@Valid @RequestBody PagoCuentaDTO r){return r;}
  }
  @BeforeEach void preparar(){
    sesion=mock(SesionOperacionService.class);when(sesion.actual()).thenReturn(new SesionActualDTO(130L,"operacion_local","PRUEBA",101L,1L));
    mvc=MockMvcBuilders.standaloneSetup(new Operaciones()).setControllerAdvice(new SesionPruebaBodyAdvice(sesion,JsonMapper.builder().build()),new GlobalExceptionHandler()).build();
  }
  @Test void completarUsuarioAntesDeValidarUnaVenta()throws Exception{
    mvc.perform(post("/api/v1/ventas/confirmadas").contentType(MediaType.APPLICATION_JSON).content("{\"claveRegistro\":\"sesion-venta\",\"modalidadCobro\":\"SEPARADA\",\"detalles\":[{\"idProducto\":1,\"cantidad\":2}]}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.idUsuario").value(130)).andExpect(jsonPath("$.modalidadCobro").value("SEPARADA"));
  }
  @Test void rechazarOtroUsuario()throws Exception{
    mvc.perform(post("/api/v1/ventas/confirmadas").contentType(MediaType.APPLICATION_JSON).content("{\"idUsuario\":999,\"claveRegistro\":\"sesion-venta\",\"modalidadCobro\":\"SEPARADA\",\"detalles\":[{\"idProducto\":1,\"cantidad\":2}]}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("La operación debe corresponder al usuario de la sesión actual"));
  }
  @Test void completarUsuarioYTurnoDeUnPago()throws Exception{
    mvc.perform(post("/api/v1/ventas/1/pagos").contentType(MediaType.APPLICATION_JSON).content("{\"claveOperacion\":\"sesion-pago\",\"idFormaPago\":1,\"monto\":5}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.idUsuario").value(130)).andExpect(jsonPath("$.idTurno").value(101));
  }
  @Test void noCobrarSinTurnoAbierto()throws Exception{
    when(sesion.actual()).thenReturn(new SesionActualDTO(130L,"operacion_local","PRUEBA",null,1L));
    mvc.perform(post("/api/v1/ventas/1/pagos").contentType(MediaType.APPLICATION_JSON).content("{\"claveOperacion\":\"sesion-pago\",\"idFormaPago\":1,\"monto\":5}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("El usuario actual necesita un único turno de caja abierto"));
  }
}
