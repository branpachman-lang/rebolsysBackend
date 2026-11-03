package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record GastoFiltroDTO(
    @Positive Long idTurno,
    @Positive Long idUsuario,
    @Positive Long idTipoMovimiento,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
    @Size(max = 200) String buscar,
    @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal montoMinimo,
    @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal montoMaximo,
    @Min(0) Integer pagina,
    @Min(1) @Max(100) Integer tamanio) {
  public GastoFiltroDTO {
    pagina = pagina == null ? 0 : pagina;
    tamanio = tamanio == null ? 20 : tamanio;
    buscar = buscar == null ? "" : buscar.trim();
  }

  @AssertTrue(message = "La fecha inicial no puede ser posterior a la final")
  public boolean isRangoFechasValido() {
    return desde == null || hasta == null || !desde.isAfter(hasta);
  }

  @AssertTrue(message = "El importe mínimo no puede superar al máximo")
  public boolean isRangoMontosValido() {
    return montoMinimo == null || montoMaximo == null || montoMinimo.compareTo(montoMaximo) <= 0;
  }
}
