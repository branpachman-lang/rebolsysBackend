package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GastoRequestDTO(
    @NotNull @Positive Long idUsuario,
    @NotNull @Positive Long idTurno,
    @NotNull @Positive Long idTipoMovimiento,
    @NotBlank @Size(max = 1000) String concepto,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal monto,
    @Size(max = 100) String documentoRespaldo,
    @Size(max = 300) String evidenciaRuta,
    @Pattern(regexp = "[a-fA-F0-9]{64}") String evidenciaSha256,
    @Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String claveRegistro) {
  public GastoRequestDTO(Long idUsuario, Long idTurno, Long idTipoMovimiento,
      String concepto, BigDecimal monto, String documentoRespaldo,
      String evidenciaRuta, String evidenciaSha256) {
    this(idUsuario, idTurno, idTipoMovimiento, concepto, monto, documentoRespaldo,
        evidenciaRuta, evidenciaSha256, null);
  }
}
