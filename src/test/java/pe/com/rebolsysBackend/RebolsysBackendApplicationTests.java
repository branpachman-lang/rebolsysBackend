package pe.com.rebolsysBackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.sesion-prueba.habilitada=false")
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
@AutoConfigureMockMvc
class RebolsysBackendApplicationTests {

  @Autowired private MockMvc mvc;

  @Autowired private ApplicationContext context;

  @Test
  void startsWithOracleWithoutAutomaticallyInsertingRecords() {
    assertThat(context.containsBean("dataSource")).isTrue();
    assertThat(context.containsBean("flyway")).isFalse();
    assertThat(context.containsBean("datosIteracion")).isFalse();
    assertThat(
            context.getBeansOfType(
                org.springframework.security.core.userdetails.UserDetailsService.class))
        .isEmpty();
  }

  @Test
  void healthIsPublicAndDoesNotCreateASession() throws Exception {
    var result =
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.application").value("rebolsysBackend"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andReturn();
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @Test
  void businessRoutesRequireLogin() throws Exception {
    mvc.perform(get("/api/v1/items"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void remoteBusinessRequestsRequireLogin() throws Exception {
    var result = mvc.perform(get("/api/v1/items").with(r -> {
          r.setRemoteAddr("192.0.2.10");
          return r;
        }))
        .andExpect(status().isUnauthorized()).andReturn();
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @Test
  void allowedOriginCanSendAnAuthenticatedRequestPreflight() throws Exception {
    mvc.perform(
            options("/api/v1/items")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
  }

  @Test
  void unconfiguredOriginsAreRejected() throws Exception {
    mvc.perform(
            options("/api/v1/items")
                .header("Origin", "https://unconfigured.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void allowedFrontendOriginReceivesAuthenticationFailure() throws Exception {
    mvc.perform(get("/api/v1/items").header("Origin", "http://localhost:4200"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
  }

  @Test
  void openApiIncludesTheRestResources() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("REBOLSYS API"))
        .andExpect(jsonPath("$.paths['/api/health'].get").exists())
        .andExpect(jsonPath("$.paths['/api/v1/reservas'].post").exists())
        .andExpect(jsonPath("$.paths['/api/v1/habitaciones'].get").exists());
  }
}
