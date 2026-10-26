package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
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
public class ReservaDetalleRequestDTO {

    @NotNull(message = "idHabitacion es obligatorio")
    private Long idHabitacion;

    @NotNull(message = "fechaIngreso es obligatorio")
    private LocalDateTime fechaIngreso;

    @NotNull(message = "fechaSalida es obligatorio")
    private LocalDateTime fechaSalida;

    @NotNull(message = "huespedes es obligatorio")
    private List<@Valid ReservaHuespedRegistroRequestDTO> huespedes;

    @AssertTrue(message = "La salida debe ser posterior al ingreso")
    @JsonIgnore
    public boolean isFechasValidas() {
        return fechaIngreso == null || fechaSalida == null || fechaSalida.isAfter(fechaIngreso);
    }
}
