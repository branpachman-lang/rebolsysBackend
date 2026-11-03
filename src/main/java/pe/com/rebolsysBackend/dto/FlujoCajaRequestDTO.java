package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlujoCajaRequestDTO {

  @NotNull(message = "idTurno es obligatorio")
  private Long idTurno;

  @NotNull(message = "idTipoMovimiento es obligatorio")
  private Long idTipoMovimiento;

  private Long idPago;

  @NotNull(message = "concepto es obligatorio")
  @Size(min = 1, max = 1000, message = "concepto debe tener entre 1 y 1000 caracteres")
  private String concepto;

  @NotNull(message = "monto es obligatorio")
  @Digits(
      integer = 10,
      fraction = 2,
      message = "monto admite hasta 10 dígitos enteros y 2 decimales")
  @Positive(message = "monto debe ser mayor que 0")
  private BigDecimal monto;

  @Size(max = 100, message = "documentoRespaldo no debe superar 100 caracteres")
  private String documentoRespaldo;

  @Size(max = 300, message = "evidenciaRuta no debe superar 300 caracteres")
  private String evidenciaRuta;

  @Size(min = 64, max = 64, message = "evidenciaSha256 debe tener entre 64 y 64 caracteres")
  private String evidenciaSha256;
}
