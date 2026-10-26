package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoReservaResponseDTO {

    private Long idConsumo;
    private Long idVenta;
    private Long idDetalleReserva;
    private String habitacion;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal total;
    private String estadoCobro;
    private LocalDateTime fecha;
}
