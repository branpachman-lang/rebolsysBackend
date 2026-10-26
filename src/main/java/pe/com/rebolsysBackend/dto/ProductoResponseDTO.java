package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoProducto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponseDTO {

    private Long idProducto;

    private String nombreProducto;

    private BigDecimal precioActual;

    private Integer stock;

    private Integer stockMinimo;

    private Long idCategoria;

    private EstadoProducto estadoProducto;

    private String imagenRuta;
    private String categoriaNombre;
}
