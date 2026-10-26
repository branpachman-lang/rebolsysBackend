package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponseDTO {

    private Long idItem;

    private String nombreItem;

    private EstadoItem estadoItem;

    private Long idTipoItem;
}
