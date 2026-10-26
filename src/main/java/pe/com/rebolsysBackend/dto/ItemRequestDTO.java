package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDTO {

    @NotNull(message = "nombreItem es obligatorio")
    @Size(min = 1, max = 100, message = "nombreItem debe tener entre 1 y 100 caracteres")
    private String nombreItem;

    @NotNull(message = "estadoItem es obligatorio")
    private EstadoItem estadoItem = EstadoItem.ACTIVO;

    @NotNull(message = "idTipoItem es obligatorio")
    private Long idTipoItem;
}
