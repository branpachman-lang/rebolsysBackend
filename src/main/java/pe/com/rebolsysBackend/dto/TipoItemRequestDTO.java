package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoItemRequestDTO {

    @NotNull(message = "nombreItem es obligatorio")
    @Size(min = 1, max = 30, message = "nombreItem debe tener entre 1 y 30 caracteres")
    private String nombreItem;
}
