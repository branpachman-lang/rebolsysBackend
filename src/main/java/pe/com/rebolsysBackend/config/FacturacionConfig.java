package pe.com.rebolsysBackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FacturacionApisPeruProperties.class)
public class FacturacionConfig {}
