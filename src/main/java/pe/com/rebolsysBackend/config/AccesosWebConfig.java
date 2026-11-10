package pe.com.rebolsysBackend.config;

import jakarta.servlet.http.*;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import pe.com.rebolsysBackend.service.impl.AccesosService;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;
import pe.com.rebolsysBackend.controller.GestionAccesosController;

@Configuration
@RequiredArgsConstructor
public class AccesosWebConfig implements WebMvcConfigurer {
  private final SesionOperacionService sesion;

  @Override
  public void addInterceptors(InterceptorRegistry registro) {
    registro.addInterceptor(new HandlerInterceptor() {
      @Override
      public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) return true;
        var reglas = Arrays.asList(AccesosService.regla(metodo));
        if (reglas.contains(Acceso.PUBLICO) || reglas.contains(Acceso.CIERRE)) return true;
        var actual = sesion.actual();
        if (reglas.contains(Acceso.SESION)) return true;
        if (actual.requiereCambio()) throw new AccessDeniedException("Cambia la contraseña temporal para continuar");
        boolean pruebaOperativa = "PRUEBA".equals(actual.modo()) && metodo.getBeanType() != GestionAccesosController.class;
        if (!reglas.isEmpty() && (reglas.contains(Acceso.CATALOGOS) || actual.gerente()
            || pruebaOperativa
            || reglas.stream().anyMatch(actual.permisos()::contains))) return true;
        throw new AccessDeniedException("Tu usuario no tiene acceso a esta función");
      }
    }).addPathPatterns("/api/v1/**").order(-100);
  }
}
