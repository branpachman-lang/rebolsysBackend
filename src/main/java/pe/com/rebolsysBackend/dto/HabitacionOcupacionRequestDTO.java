package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabitacionOcupacionRequestDTO {

    @NotNull(message = "La capacidad máxima de personas es obligatoria")
    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    @Max(value = 99, message = "La capacidad máxima no puede superar 99")
    private Integer capacidadPersonas;

    @NotNull(message = "Las plazas de cama son obligatorias")
    @Min(value = 1, message = "Debe existir al menos una plaza de cama")
    @Max(value = 99, message = "Las plazas de cama no pueden superar 99")
    private Integer plazasCama;

    @NotNull(message = "El máximo de menores sin plaza propia es obligatorio")
    @Min(value = 0, message = "El máximo de menores sin plaza propia no puede ser negativo")
    @Max(value = 1, message = "Solo se permite un menor adicional sin cama por habitación")
    private Integer maxMenoresSinCama;

    @JsonIgnore
    @AssertTrue(message = "Las plazas de cama no pueden superar la capacidad máxima de personas")
    public boolean isPlazasValidas() {
        return capacidadPersonas == null || plazasCama == null || plazasCama <= capacidadPersonas;
    }

    @JsonIgnore
    @AssertTrue(message = "El límite de menores sin plaza propia debe dejar espacio para al menos un adulto")
    public boolean isLimiteMenoresValido() {
        return capacidadPersonas == null || maxMenoresSinCama == null || maxMenoresSinCama < capacidadPersonas;
    }
}
