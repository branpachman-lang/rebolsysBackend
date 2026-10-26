package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InformeRevisionResponseDTO {

    private Long idInformeRevision;

    private Long idDetalleReserva;

    private Long idHabitacion;

    private Long idUsuario;

    private LocalDateTime fechaHora;

    private String observaciones;
}
