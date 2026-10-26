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
public class TipoMovimientoAlmacenRequestDTO {

    @NotNull(message = "codigo es obligatorio")
    @Size(min = 1, max = 10, message = "codigo debe tener entre 1 y 10 caracteres")
    private String codigo;

    @NotNull(message = "nombreMovimiento es obligatorio")
    @Size(min = 1, max = 50, message = "nombreMovimiento debe tener entre 1 y 50 caracteres")
    private String nombreMovimiento;

    @NotNull(message = "signo es obligatorio")
    @Digits(integer = 2, fraction = 0, message = "signo admite hasta 2 dígitos enteros y 0 decimales")
    private Integer signo;

    @JsonIgnore
    @AssertTrue(message = "El signo debe ser -1, 0 o 1")
    public boolean isSignoValido() {
        return signo == null || signo == -1 || signo == 0 || signo == 1;
    }
}
