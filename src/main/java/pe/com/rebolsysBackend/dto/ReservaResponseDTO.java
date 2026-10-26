package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
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
public class ReservaResponseDTO {

    private Long idReserva;

    private Long idCliente;

    private LocalDateTime fechaRegistro;

    private EstadoReserva estadoReserva;

    private Long idUsuario;

    private List<ReservaDetalleResponseDTO> detalles;
}
