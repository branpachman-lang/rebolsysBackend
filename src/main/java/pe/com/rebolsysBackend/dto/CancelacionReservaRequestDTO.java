package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionReservaRequestDTO {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    @Size(max = 800, message = "El motivo no debe superar 800 caracteres")
    private String motivo;

    private boolean confirmarDevolucion;
}
