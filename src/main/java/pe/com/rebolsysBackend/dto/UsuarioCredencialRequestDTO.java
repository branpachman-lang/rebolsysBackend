package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class UsuarioCredencialRequestDTO {

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "idTipoCredencial es obligatorio")
    private Long idTipoCredencial;

    @Size(max = 255, message = "valorHash no debe superar 255 caracteres")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String valorHash;

    @Digits(integer = 1, fraction = 0, message = "credencialTemporal admite hasta 1 dígitos enteros y 0 decimales")
    @Min(value = 0, message = "credencialTemporal debe ser mayor o igual a 0")
    @Max(value = 1, message = "credencialTemporal no debe superar 1")
    private Integer credencialTemporal = 1;

    @NotNull(message = "intentosFallidos es obligatorio")
    @Min(value = 0, message = "intentosFallidos debe ser mayor o igual a 0")
    @Max(value = 5, message = "intentosFallidos no debe superar 5")
    private Integer intentosFallidos = 0;

    private LocalDateTime fechaBloqueo;
}
