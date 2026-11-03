package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioRequestDTO {

  private Long idProducto;

  private Long idItem;

  private Long idDetalleVenta;

  @jakarta.validation.constraints.NotBlank
  @Size(max = 64)
  private String claveOperacion;

  private Long idDetalleHabitacion;

  @NotNull(message = "idTipoMovimientoAlmacen es obligatorio")
  private Long idTipoMovimientoAlmacen;

  @NotNull(message = "cantidad es obligatorio")
  @Digits(
      integer = 9,
      fraction = 0,
      message = "cantidad admite hasta 9 dígitos enteros y 0 decimales")
  @jakarta.validation.constraints.Positive
  private Integer cantidad;

  @NotNull(message = "stockResultante es obligatorio")
  @Digits(
      integer = 9,
      fraction = 0,
      message = "stockResultante admite hasta 9 dígitos enteros y 0 decimales")
  @PositiveOrZero(message = "stockResultante debe ser mayor o igual a 0")
  private Integer stockResultante;

  @NotNull(message = "idUsuario es obligatorio")
  private Long idUsuario;

  @Size(max = 300, message = "observacion no debe superar 300 caracteres")
  private String observacion;

  @JsonIgnore
  @AssertTrue(message = "Indique producto o ítem, de forma excluyente")
  public boolean isReferenciaValida() {
    return (idProducto != null) != (idItem != null);
  }

  @JsonIgnore
  @AssertTrue(message = "Un detalle de venta en inventario requiere un producto")
  public boolean isDetalleVentaValido() {
    return idDetalleVenta == null || idProducto != null;
  }
}
