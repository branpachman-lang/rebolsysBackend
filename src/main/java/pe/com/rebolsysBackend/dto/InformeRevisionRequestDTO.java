package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InformeRevisionRequestDTO {

    private Long idDetalleReserva;

    @NotNull(message = "idHabitacion es obligatorio")
    private Long idHabitacion;

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "fechaHora es obligatorio")
    private LocalDateTime fechaHora;

    @NotNull(message = "observaciones es obligatorio")
    @Size(min = 1, max = 1000, message = "observaciones debe tener entre 1 y 1000 caracteres")
    private String observaciones;
}
