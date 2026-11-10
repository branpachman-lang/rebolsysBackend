package pe.com.rebolsysBackend.config;

import java.util.Map;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.service.impl.*;

@Configuration
@RequiredArgsConstructor
public class SesionPruebaWebConfig implements WebMvcConfigurer {
  private final AuditoriaOperacionService auditoria;
  private static final Map<String,String> TABLAS=Map.ofEntries(Map.entry("habitaciones","HABITACION"),Map.entry("reservas","RESERVA"),Map.entry("ventas","VENTA"),Map.entry("gastos","FLUJO_CAJA"),Map.entry("productos","PRODUCTO"),Map.entry("items","ITEM"),Map.entry("categorias","CATEGORIA"),Map.entry("tipos-habitacion","TIPO_HABITACION"),Map.entry("tipos-item","TIPO_ITEM"),Map.entry("clientes","CLIENTE"),Map.entry("comprobantes","COMPROBANTE"));
  @Override public void addInterceptors(InterceptorRegistry registro){
    registro.addInterceptor(new HandlerInterceptor(){
      @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object handler,Exception ex){
        if(ex!=null||res.getStatus()<200||res.getStatus()>=300||!java.util.Set.of("POST","PUT","PATCH","DELETE").contains(req.getMethod()))return;
        if(!(req.getAttribute(SesionOperacionService.ATRIBUTO) instanceof SesionActualDTO actual))return;
        var partes=req.getRequestURI().substring("/api/v1/".length()).split("/");
        var tabla=TABLAS.get(partes[0]);
        Long id=null;
        if (partes[0].equals("seguridad") && partes.length >= 2) {
          tabla = switch (partes[1]) {
            case "empleados" -> "EMPLEADO";
            case "usuarios" -> "USUARIO";
            case "roles" -> "ROL";
            default -> null;
          };
          if (partes.length > 2 && partes[2].matches("\\d+")) id = Long.valueOf(partes[2]);
        } else if (partes[0].equals("limpieza") && partes.length >= 3) {
          boolean ticketNuevo = partes[1].equals("informes") && partes.length == 6 && partes[5].equals("ticket");
          tabla = ticketNuevo ? "TICKET_MANTENIMIENTO" : switch (partes[1]) {
            case "habitaciones", "borradores", "informes" -> "INFORME_REVISION";
            case "tickets" -> "TICKET_MANTENIMIENTO";
            default -> null;
          };
          if (!ticketNuevo && !partes[1].equals("habitaciones") && partes[2].matches("\\d+")) {
            id = Long.valueOf(partes[2]);
          }
        } else {
          if(partes[0].equals("reservas")&&partes.length>1&&partes[1].equals("detalles"))tabla="DETALLE_RESERVA";
          for(var parte:partes)if(parte.matches("\\d+")){id=Long.valueOf(parte);break;}
        }
        if(tabla==null)return;
        if(id==null&&req.getAttribute("rebolsys.registro") instanceof Long valor)id=valor;
        if(id==null)return;
        try{auditoria.registrar(actual,tabla,id,req.getMethod(),req.getRequestURI(),req.getRemoteAddr());}
        catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger(SesionPruebaWebConfig.class).error("No se pudo registrar la auditoría de {} {}",req.getMethod(),req.getRequestURI(),error);}
      }
    }).addPathPatterns("/api/v1/**");
  }
}
