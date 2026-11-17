package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportesDTO {
  private ReportesDTO() {}

  public record Pagina<T>(List<T> filas, int pagina, boolean haySiguiente) {}

  public record Incidencia(Long idInforme, String habitacion, LocalDateTime fecha, String estadoInforme,
      String mueble, String condicion, String observacion, Long idTicket, String estadoTicket) {}

  public record Importe(String concepto, String medioPago, BigDecimal monto) {}

  public record Financiero(List<Importe> filas, BigDecimal cobrosReservas, BigDecimal cobrosVentas,
      BigDecimal ingresosManuales, BigDecimal egresos, BigDecimal neto) {}

  public record Cierre(Long idTurno, String usuario, LocalDateTime apertura, LocalDateTime cierre,
      String medioPago, BigDecimal esperado, BigDecimal declarado, BigDecimal diferencia) {}

  public record Existencia(Long id, String producto, String categoria, String estado, int stock, int minimo) {}

  public record Mueble(Long id, String mueble, String tipo, String habitacion, String estado, String condicion) {}
}
