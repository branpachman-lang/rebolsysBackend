package pe.com.rebolsysBackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.com.rebolsysBackend.service.impl.AccesosService;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "REBOLSYS API",
            version = "v1",
            description =
                "Siete casos de uso del hospedaje con cliente unificado: reservas, estados de"
                    + " habitaciones, limpieza, mantenimiento, gastos, ventas y configuración de habitaciones, mobiliario y"
                    + " productos. Reservas asistidas con anticipos, ocupantes al ingreso,"
                    + " consumos, pagos y comprobantes internos con emisión electrónica beta"
                    + " mediante APIsPerú. La integración verifica el ambiente beta; producción bloqueada."))
@SecurityScheme(name = "sesion", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "JSESSIONID")
public class OpenApiConfig {
  @Bean
  OperationCustomizer proteccionOperativa() {
    return (operacion, handler) -> {
      var acceso = Arrays.asList(AccesosService.regla(handler));
      if (!acceso.isEmpty() && !acceso.contains(Acceso.PUBLICO)) {
        operacion.addSecurityItem(new SecurityRequirement().addList("sesion"));
      }
      return operacion;
    };
  }
}
