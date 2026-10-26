package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReservaHuespedRegistroRequestDTO {

    @NotNull(message = "persona es obligatorio")
    @Valid
    private ClienteRegistroRequestDTO persona;

    @NotNull(message = "idTipoOcupante es obligatorio")
    private Long idTipoOcupante;

    @Valid
    private ResponsableHuespedRequestDTO responsable;

    private Long idHuesped;

    @NotNull(message = "Indica si el huésped requiere una plaza de cama propia")
    private Boolean ocupaPlazaCama = true;

    public ReservaHuespedRegistroRequestDTO(
        ClienteRegistroRequestDTO persona,
        Long idTipoOcupante,
        ResponsableHuespedRequestDTO responsable,
        Long idHuesped
    ) {
        this(persona, idTipoOcupante, responsable, idHuesped, true);
    }

    public ReservaHuespedRegistroRequestDTO(
        ClienteRegistroRequestDTO persona,
        Long idTipoOcupante,
        ResponsableHuespedRequestDTO responsable
    ) {
        this(persona, idTipoOcupante, responsable, null);
    }

    public ReservaHuespedRegistroRequestDTO(ClienteRegistroRequestDTO persona, Long idTipoOcupante) {
        this(persona, idTipoOcupante, null);
    }

    @AssertTrue(message = "El huésped debe ser una persona natural")
    @JsonIgnore
    public boolean isNatural() {
        return persona == null || !Boolean.TRUE.equals(persona.getEmpresa());
    }

    @AssertTrue(message = "La fecha de nacimiento de cada huésped es obligatoria")
    @JsonIgnore
    public boolean isFechaNacimientoValida() {
        return persona == null || persona.getFechaNacimiento() != null;
    }
}
