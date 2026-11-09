package pe.com.rebolsysBackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.com.rebolsysBackend.config.MockMvcReglasNegocio;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "apis-peru.url=http://127.0.0.1:4900/apisperu/api/v1",
      "apis-peru.token=token-prueba-local",
      "apis-peru.timeout-millis=500",
    })
@MockMvcReglasNegocio
@EnabledIfEnvironmentVariable(named = "RUN_APISPERU_SIM_TESTS", matches = "true")
class ApisPeruSimulacionTest {

  @Autowired private MockMvc mvc;

  @Test
  void autocompleteDniWithoutReturningTheToken() throws Exception {
    var result =
        mvc.perform(get("/api/v1/documentos/dni/11111111"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombres").value("PERSONA DE PRUEBA"))
            .andExpect(jsonPath("$.apellidos").value("LOCAL DEMO"))
            .andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("token-prueba-local");
  }

  @Test
  void autocompleteRuc() throws Exception {
    mvc.perform(get("/api/v1/documentos/ruc/20123456789"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.razonSocial").value("EMPRESA DE PRUEBA LOCAL"))
        .andExpect(jsonPath("$.direccionFiscal").value("DIRECCION DE PRUEBA"));
  }

  @Test
  void rejectsInvalidDniBeforeCallingTheProvider() throws Exception {
    mvc.perform(get("/api/v1/documentos/dni/123"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.numero")
                .value("El DNI debe contener exactamente 8 dígitos"));
  }

  @Test
  void documentNotFoundRemainsRecoverable() throws Exception {
    mvc.perform(get("/api/v1/documentos/dni/22222222")).andExpect(status().isNotFound());
  }

  @Test
  void timeoutDoesNotHangTheForm() throws Exception {
    mvc.perform(get("/api/v1/documentos/dni/33333333")).andExpect(status().isGatewayTimeout());
  }

  @Test
  void rejectedCredentialDoesNotExposeTheToken() throws Exception {
    var result =
        mvc.perform(get("/api/v1/documentos/dni/44444444"))
            .andExpect(status().isBadGateway())
            .andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("token-prueba-local");
  }

  @Test
  void doesNotAcceptDataForAnotherDocument() throws Exception {
    mvc.perform(get("/api/v1/documentos/dni/55555555")).andExpect(status().isBadGateway());
  }
}
