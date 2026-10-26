package pe.com.rebolsysBackend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CatalogosResponseDTO {

    private List<CatalogoItemResponseDTO> documentos;

    private List<CatalogoItemResponseDTO> ocupantes;

    private List<CatalogoItemResponseDTO> pagos;
}
