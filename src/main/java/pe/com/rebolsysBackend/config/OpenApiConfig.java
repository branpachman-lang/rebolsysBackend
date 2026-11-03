package pe.com.rebolsysBackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "REBOLSYS API",
            version = "v1",
            description =
                "Siete casos de uso del hospedaje con cliente unificado: reservas, estados de"
                    + " habitaciones, gastos, ventas y configuración de habitaciones, mobiliario y"
                    + " productos. Reservas asistidas con anticipos, ocupantes al ingreso,"
                    + " consumos, pagos y comprobantes internos con emisión electrónica beta"
                    + " mediante APIsPerú. La integración verifica el ambiente beta; producción bloqueada."))
public class OpenApiConfig {}
