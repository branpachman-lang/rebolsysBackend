package pe.com.rebolsysBackend.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ErrorExamples())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void validationErrorsIdentifyFields() throws Exception {
    mvc.perform(
            post("/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.nombre").value("El nombre es obligatorio"))
        .andExpect(jsonPath("$.path").value("/validation"));
  }

  @Test
  void invalidJsonUsesTheSameErrorFormat() throws Exception {
    mvc.perform(post("/validation").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La solicitud no tiene un formato válido"))
        .andExpect(jsonPath("$.path").value("/validation"));
  }

  @Test
  void methodErrorsPreserveTheAllowHeader() throws Exception {
    mvc.perform(post("/unexpected"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  void unexpectedFailuresDoNotExposeInternalDetails() throws Exception {
    var result =
        mvc.perform(get("/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Ocurrió un error interno"))
            .andReturn();
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("INTERNAL_DATABASE_DETAIL");
  }

  @Test
  void databaseErrorsKeepTheNameOfTheActualConstraint() throws Exception {
    mvc.perform(get("/constraint"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("ORA-02290: check constraint (HOTELRB.RESERVA_FECHAS_CHK) violated"));
  }

  @Test
  void invalidParameterTypesReturnBadRequest() throws Exception {
    mvc.perform(get("/parameter").param("numero", "texto"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.validationErrors.numero")
                .value("El parámetro numero no tiene un formato válido"));
  }

  record ExampleRequest(@NotBlank(message = "El nombre es obligatorio") String nombre) {}

  @RestController
  static class ErrorExamples {

    @GetMapping("/parameter")
    public int parameter(@RequestParam int numero) {
      return numero;
    }

    @GetMapping("/constraint")
    public void constraint() {
      throw new org.springframework.dao.DataIntegrityViolationException(
          "Restricción rechazada",
          new java.sql.SQLException(
              "ORA-02290: check constraint (HOTELRB.RESERVA_FECHAS_CHK) violated\n"
                  + "INTERNAL_SQL_DETAIL"));
    }

    @PostMapping("/validation")
    public void validation(@Valid @RequestBody ExampleRequest request) {}

    @GetMapping("/unexpected")
    public void unexpected() {
      throw new IllegalStateException("INTERNAL_DATABASE_DETAIL");
    }
  }
}
