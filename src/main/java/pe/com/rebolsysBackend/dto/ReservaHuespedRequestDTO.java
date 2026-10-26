package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ParentescoResponsable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaHuespedRequestDTO {

    @NotNull(message = "idDetalleReserva es obligatorio")
    private Long idDetalleReserva;

    @NotNull(message = "idHuesped es obligatorio")
    private Long idHuesped;

    @NotNull(message = "idTipoOcupante es obligatorio")
    private Long idTipoOcupante;

    private Long idHuespedResponsable;
    private ParentescoResponsable parentescoResponsable;

    @NotNull(message = "Indica si el huésped requiere una plaza de cama propia")
    private Boolean ocupaPlazaCama = true;

    @JsonIgnore
    @AssertTrue(
        message = "Responsable y parentesco deben informarse juntos, sin asignar al huésped como su propio responsable"
    )
    public boolean isResponsableValido() {
        return idHuespedResponsable == null
            ? parentescoResponsable == null
            : parentescoResponsable != null && !idHuespedResponsable.equals(idHuesped);
    }
}
