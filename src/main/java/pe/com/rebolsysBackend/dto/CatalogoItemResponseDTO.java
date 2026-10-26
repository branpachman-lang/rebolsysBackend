package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CatalogoItemResponseDTO {

    private Long id;

    private String nombre;

    private String codigoSunat;

    public CatalogoItemResponseDTO(Long id, String nombre) {
        this(id, nombre, null);
    }
}
