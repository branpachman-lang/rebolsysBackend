package pe.com.rebolsysBackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "REBOLSYS API",
        version = "v1",
        description = "API de gestión de hospedaje. Base técnica inicial."
    )
)
public class OpenApiConfig {}
