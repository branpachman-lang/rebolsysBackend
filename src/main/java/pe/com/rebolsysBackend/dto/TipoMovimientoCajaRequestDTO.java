package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimientoCajaRequestDTO {

    @NotNull(message = "nombreMovimiento es obligatorio")
    @Size(min = 1, max = 30, message = "nombreMovimiento debe tener entre 1 y 30 caracteres")
    private String nombreMovimiento;

    @NotNull(message = "signo es obligatorio")
    @Digits(integer = 2, fraction = 0, message = "signo admite hasta 2 dígitos enteros y 0 decimales")
    private Integer signo;

    @JsonIgnore
    @AssertTrue(message = "El signo debe ser -1 o 1")
    public boolean isSignoValido() {
        return signo == null || signo == -1 || signo == 1;
    }
}
