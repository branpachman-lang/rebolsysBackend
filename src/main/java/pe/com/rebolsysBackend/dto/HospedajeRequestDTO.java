package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HospedajeRequestDTO {

    @NotNull(message = "nombre es obligatorio")
    @Size(min = 1, max = 150, message = "nombre debe tener entre 1 y 150 caracteres")
    private String nombre;

    @NotNull(message = "ruc es obligatorio")
    @Size(min = 1, max = 11, message = "ruc debe tener entre 1 y 11 caracteres")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe contener exactamente 11 dígitos")
    private String ruc;

    @Size(max = 200, message = "direccion no debe superar 200 caracteres")
    private String direccion;

    @Size(max = 300, message = "logoRuta no debe superar 300 caracteres")
    private String logoRuta;
}
