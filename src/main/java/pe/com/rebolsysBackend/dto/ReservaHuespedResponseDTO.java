package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ParentescoResponsable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaHuespedResponseDTO {

    private Boolean ocupaPlazaCama;

    private Long idDetalleReserva;

    private Long idHuesped;

    private Long idTipoOcupante;

    private Long idHuespedResponsable;
    private ParentescoResponsable parentescoResponsable;
}
