package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabitacionRequestDTO {

    @NotNull(message = "numeroCuarto es obligatorio")
    @Size(min = 1, max = 10, message = "numeroCuarto debe tener entre 1 y 10 caracteres")
    private String numeroCuarto;

    @NotNull(message = "idTipo es obligatorio")
    private Long idTipo;

    @NotNull(message = "estadoHabitacion es obligatorio")
    private EstadoHabitacion estadoHabitacion;

    @NotNull(message = "La capacidad máxima de personas es obligatoria")
    @jakarta.validation.constraints.Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    @jakarta.validation.constraints.Max(value = 99, message = "La capacidad máxima no puede superar 99")
    private Integer capacidadPersonas;

    @NotNull(message = "Las plazas de cama son obligatorias")
    @jakarta.validation.constraints.Min(value = 1, message = "Debe existir al menos una plaza de cama")
    @jakarta.validation.constraints.Max(value = 99, message = "Las plazas de cama no pueden superar 99")
    private Integer plazasCama;

    @NotNull(message = "El máximo de menores sin plaza propia es obligatorio")
    @jakarta.validation.constraints.Min(
        value = 0,
        message = "El máximo de menores sin plaza propia no puede ser negativo"
    )
    @jakarta.validation.constraints.Max(
        value = 98,
        message = "El máximo de menores sin plaza propia no puede superar 98"
    )
    private Integer maxMenoresSinCama = 0;
}
