package pe.com.rebolsysBackend.config;

import java.lang.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Ejecuta pruebas de reglas con sus propios actores. El flujo HTTP de seguridad se prueba en AccesoHttpTest. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = {AccesosWebConfig.class, SesionPruebaBodyAdvice.class})
public @interface MockMvcReglasNegocio {}
