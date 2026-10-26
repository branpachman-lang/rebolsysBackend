package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoReserva;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull(message = "idCliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "estadoReserva es obligatorio")
    private EstadoReserva estadoReserva;

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotEmpty(message = "Agrega al menos una habitación")
    private List<@Valid ReservaDetalleRequestDTO> detalles;
}
