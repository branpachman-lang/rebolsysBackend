package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HospedajeResponseDTO {

    private Long idHospedaje;

    private String nombre;

    private String ruc;

    private String direccion;

    private String logoRuta;
}
