package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VentaActualizarRequestDTO {
    @Size(max = 200, message = "El nombre del comprador no debe superar 200 caracteres")
    private String compradorNombre;

    @NotEmpty(message = "Agrega al menos un producto")
    @Size(max = 100, message = "El carrito admite hasta 100 productos distintos")
    private List<@NotNull @Valid VentaDetalleRequestDTO> detalles;
}
