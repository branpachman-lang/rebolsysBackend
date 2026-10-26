package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SerieComprobanteResponseDTO {

    private Long idTipoComprobante;

    private String serie;

    private Long siguienteNumero;
}
