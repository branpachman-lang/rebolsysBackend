package pe.com.rebolsysBackend.config;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.*;
import org.springframework.web.context.request.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.Borrador;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.TicketDetalle;
import pe.com.rebolsysBackend.dto.SeguridadDTO.*;
import pe.com.rebolsysBackend.service.impl.CredencialesService.Temporal;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

@ControllerAdvice
@RequiredArgsConstructor
public class SesionPruebaBodyAdvice extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Object> {
  private final SesionOperacionService sesion;
  private final ObjectMapper mapper;
  private static final Set<String> ACTOR=Set.of("AjusteStockDTO","ComprobanteCuentaDTO","EmisionElectronicaRequestDTO","GastoRequestDTO","ItemAsignacionDTO","ItemConservacionDTO","OperacionUsuarioDTO","PagoCuentaDTO","ProductoRequestDTO","ReservaComprobanteRegistroDTO","ReservaOcupantesGuardarDTO","ReservaPagoRegistroDTO","ReservaRegistroRequestDTO","VentaEditarDTO","VentaRequestDTO");
  @Override public boolean supports(MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c){return ACTOR.contains(p.getParameterType().getSimpleName());}
  @Override public HttpInputMessage beforeBodyRead(HttpInputMessage entrada,MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c)throws IOException{
    var nodo=mapper.readTree(entrada.getBody());if(!(nodo instanceof ObjectNode datos))return entrada;
    var actual=sesion.actual();
    var enviado=datos.get("idUsuario");exigir(enviado==null||enviado.isNull()||enviado.asLong()==actual.idUsuario(),"La operación debe corresponder al usuario de la sesión actual");
    datos.put("idUsuario",actual.idUsuario());
    boolean usaTurno=java.util.Arrays.stream(p.getParameterType().getDeclaredFields()).anyMatch(f->f.getName().equals("idTurno"));
    if(usaTurno){exigir(actual.idTurno()!=null,"El usuario actual necesita un único turno de caja abierto");var turno=datos.get("idTurno");exigir(turno==null||turno.isNull()||turno.asLong()==actual.idTurno(),"El turno debe pertenecer a la sesión actual");datos.put("idTurno",actual.idTurno());}
    byte[] json=mapper.writeValueAsBytes(datos);var headers=new HttpHeaders();headers.putAll(entrada.getHeaders());headers.setContentLength(json.length);
    return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(json);}public HttpHeaders getHeaders(){return headers;}};
  }
  @Override public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c){return true;}
  @Override public Object beforeBodyWrite(Object body,MethodParameter p,MediaType m,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest req,ServerHttpResponse res){
    if(body==null||body instanceof org.springframework.core.io.Resource)return body;var atributos=RequestContextHolder.getRequestAttributes();
    if(atributos instanceof ServletRequestAttributes a&&a.getRequest().getAttribute(SesionOperacionService.ATRIBUTO)!=null){
      Long registro = switch (body) {
        case UsuarioCreado creado -> creado.cuenta().idUsuario();
        case UsuarioVista usuario -> usuario.idUsuario();
        case EmpleadoVista empleado -> empleado.idEmpleado();
        case RolVista rol -> rol.idRol();
        default -> null;
      };
      if (registro != null || body instanceof Temporal) {
        if (registro != null) a.getRequest().setAttribute("rebolsys.registro", registro);
        return body;
      }
      if (body instanceof Borrador borrador) {
        a.getRequest().setAttribute("rebolsys.registro", borrador.idInformeRevision());
        return body;
      }
      if (body instanceof TicketDetalle detalle) {
        a.getRequest().setAttribute("rebolsys.registro", detalle.ticket().idTicket());
        return body;
      }
      var nodo=mapper.valueToTree(body);for(String campo:new String[]{"idHabitacion","idReserva","idVenta","idProducto","idItem","idCategoria","idTipoHabitacion","idTipoItem","idCliente","idCuenta","idMovimiento","idFlujo"}){
        var id=nodo.get(campo);if(id!=null&&id.isIntegralNumber()){a.getRequest().setAttribute("rebolsys.registro",id.asLong());break;}
      }
    }
    return body;
  }
}
