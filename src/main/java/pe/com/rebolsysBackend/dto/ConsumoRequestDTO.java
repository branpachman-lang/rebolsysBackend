package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.TipoConsumo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoRequestDTO {

    private Long idCliente;

    private Long idDetalleReserva;

    private Long idProducto;

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "tipoConsumo es obligatorio")
    private TipoConsumo tipoConsumo;

    @NotNull(message = "descripcion es obligatorio")
    @Size(min = 1, max = 300, message = "descripcion debe tener entre 1 y 300 caracteres")
    private String descripcion;

    @NotNull(message = "cantidad es obligatorio")
    @Digits(integer = 9, fraction = 3, message = "cantidad admite hasta 9 dígitos enteros y 3 decimales")
    @Positive(message = "cantidad debe ser mayor que 0")
    private BigDecimal cantidad = new BigDecimal("1");

    @NotNull(message = "valorUnitario es obligatorio")
    @Digits(integer = 8, fraction = 4, message = "valorUnitario admite hasta 8 dígitos enteros y 4 decimales")
    @PositiveOrZero(message = "valorUnitario debe ser mayor o igual a 0")
    private BigDecimal valorUnitario;

    @NotNull(message = "subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "subtotal admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "subtotal debe ser mayor o igual a 0")
    private BigDecimal subtotal;

    @NotNull(message = "igv es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "igv admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "igv debe ser mayor o igual a 0")
    private BigDecimal igv;

    @NotNull(message = "total es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "total admite hasta 10 dígitos enteros y 2 decimales")
    private BigDecimal total;

    @NotNull(message = "estadoCargo es obligatorio")
    private EstadoCargo estadoCargo;

    @NotNull(message = "estadoCobro es obligatorio")
    private EstadoCobro estadoCobro;

    @JsonIgnore
    @AssertTrue(message = "El tipo de consumo no coincide con producto o reserva")
    public boolean isRelacionValida() {
        return (
            tipoConsumo == null ||
            switch (tipoConsumo) {
                case PRODUCTO -> idProducto != null;
                case HOSPEDAJE -> idDetalleReserva != null && idProducto == null;
                case OTRO -> idProducto == null;
            }
        );
    }

    @JsonIgnore
    @AssertTrue(message = "La cantidad de producto debe ser entera")
    public boolean isUnidadesValidas() {
        return tipoConsumo != TipoConsumo.PRODUCTO || cantidad == null || cantidad.stripTrailingZeros().scale() <= 0;
    }

    @JsonIgnore
    @AssertTrue(message = "El total debe ser igual a subtotal más IGV")
    public boolean isTotalValido() {
        return subtotal == null || igv == null || total == null || total.compareTo(subtotal.add(igv)) == 0;
    }

    private Long idVenta;
}
