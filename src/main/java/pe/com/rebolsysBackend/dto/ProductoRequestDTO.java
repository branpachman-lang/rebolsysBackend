package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class ProductoRequestDTO {

    @NotBlank(message = "nombreProducto es obligatorio")
    @Size(min = 1, max = 100, message = "nombreProducto debe tener entre 1 y 100 caracteres")
    private String nombreProducto;

    @NotNull(message = "precioActual es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "precioActual admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "precioActual debe ser mayor o igual a 0")
    private BigDecimal precioActual;

    @NotNull(message = "stock es obligatorio")
    @io.swagger.v3.oas.annotations.media.Schema(description="Existencias iniciales al crear; si son mayores a cero se requiere idUsuario y se registra una entrada de inventario. Al editar se conserva el stock observado.")
    @Digits(integer = 9, fraction = 0, message = "stock admite hasta 9 dígitos enteros y 0 decimales")
    @PositiveOrZero(message = "stock debe ser mayor o igual a 0")
    private Integer stock = 0;

    @NotNull(message = "stockMinimo es obligatorio")
    @Digits(integer = 9, fraction = 0, message = "stockMinimo admite hasta 9 dígitos enteros y 0 decimales")
    @PositiveOrZero(message = "stockMinimo debe ser mayor o igual a 0")
    private Integer stockMinimo = 0;

    @NotNull(message = "idCategoria es obligatorio")
    @jakarta.validation.constraints.Positive
    private Long idCategoria;

    @NotNull(message = "estadoProducto es obligatorio")
    private EstadoProducto estadoProducto;

    @Size(max = 300, message = "La ruta de imagen no debe superar 300 caracteres")
    @Pattern(
        regexp = "^$|^/(?!/)[^\\s\\\\<>]*\\.(?i:png|jpe?g|webp|avif|gif)$",
        message = "Usa una ruta del sitio a una imagen PNG, JPG, WEBP, AVIF o GIF"
    )
    private String imagenRuta;

    @PositiveOrZero(message = "El stock observado no puede ser negativo")
    private Integer stockAnterior;

    @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
    private String descripcion;

    @jakarta.validation.constraints.Positive
    @io.swagger.v3.oas.annotations.media.Schema(description="Responsable obligatorio cuando las existencias iniciales son mayores a cero.")
    private Long idUsuario;

    public ProductoRequestDTO(String nombreProducto, BigDecimal precioActual, Integer stock,
        Integer stockMinimo, Long idCategoria, EstadoProducto estadoProducto,
        String imagenRuta, Integer stockAnterior) {
        this(nombreProducto,precioActual,stock,stockMinimo,idCategoria,estadoProducto,imagenRuta,stockAnterior,null,null);
    }

    public ProductoRequestDTO(
        String nombreProducto,
        BigDecimal precioActual,
        Integer stock,
        Integer stockMinimo,
        Long idCategoria,
        EstadoProducto estadoProducto
    ) {
        this(nombreProducto, precioActual, stock, stockMinimo, idCategoria, estadoProducto, null, null);
    }
}
