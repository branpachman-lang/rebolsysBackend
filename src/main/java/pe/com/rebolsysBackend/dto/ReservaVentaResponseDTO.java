package pe.com.rebolsysBackend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaVentaResponseDTO {

    private Long idDetalleReserva;
    private Long idReserva;
    private String referencia;
    private String numero;
    private String habitacion;
    private String clienteNombre;
    private Long idCliente;
    private List<String> huespedes;
}
