package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReservaDetalleResponseDTO {

    private Long idDetalleReserva;

    private Long idHabitacion;

    private String numero;

    private String tipo;

    private int capacidad;
    private int plazasCama;
    private int maxMenoresSinCama;

    private BigDecimal precio;

    private LocalDateTime fechaIngreso;

    private LocalDateTime fechaSalida;

    private BigDecimal costo;

    private String estado;

    private List<ReservaHuespedRegistroRequestDTO> huespedes;
}
