package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoRequestDTO {

    @NotNull(message = "idPerfil es obligatorio")
    private Long idPerfil;

    @NotNull(message = "fechaContratacion es obligatorio")
    private LocalDate fechaContratacion;

    private LocalDate fechaCese;

    @JsonIgnore
    @AssertTrue(message = "La fecha de cese no puede ser anterior a la contratación")
    public boolean isFechasValidas() {
        return fechaCese == null || fechaContratacion == null || !fechaCese.isBefore(fechaContratacion);
    }
}
