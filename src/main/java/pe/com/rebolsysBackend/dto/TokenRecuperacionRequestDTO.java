package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacionRequestDTO {

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "tokenHash es obligatorio")
    @Size(min = 1, max = 255, message = "tokenHash debe tener entre 1 y 255 caracteres")
    @Pattern(regexp = "^[0-9A-Fa-f]{64}$", message = "tokenHash no tiene el formato permitido por su restricción")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String tokenHash;

    @NotNull(message = "fechaExpiracion es obligatorio")
    private LocalDateTime fechaExpiracion;

    @NotNull(message = "usado es obligatorio")
    @Digits(integer = 1, fraction = 0, message = "usado admite hasta 1 dígitos enteros y 0 decimales")
    @Min(value = 0, message = "usado debe ser mayor o igual a 0")
    @Max(value = 1, message = "usado no debe superar 1")
    private Integer usado = 0;
}
