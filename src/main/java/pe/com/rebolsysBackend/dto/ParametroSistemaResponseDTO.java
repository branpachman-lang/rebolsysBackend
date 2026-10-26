package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistemaResponseDTO {

    private Long idParametro;

    private String clave;

    private String valor;

    private String descripcion;
}
