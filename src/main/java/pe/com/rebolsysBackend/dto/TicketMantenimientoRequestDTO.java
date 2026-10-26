package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoTicket;
import pe.com.rebolsysBackend.enums.TipoAccionMantenimiento;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketMantenimientoRequestDTO {

    @NotNull(message = "idDetalleInforme es obligatorio")
    private Long idDetalleInforme;

    private Long idEmpleado;

    @NotNull(message = "tipoAccion es obligatorio")
    private TipoAccionMantenimiento tipoAccion;

    @NotNull(message = "estadoTicket es obligatorio")
    private EstadoTicket estadoTicket;

    private LocalDateTime fechaResolucion;

    @Digits(integer = 8, fraction = 2, message = "costoReparacion admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "costoReparacion debe ser mayor o igual a 0")
    private BigDecimal costoReparacion;

    @Size(max = 300, message = "evidenciaRuta no debe superar 300 caracteres")
    private String evidenciaRuta;

    @Size(min = 64, max = 64, message = "evidenciaSha256 debe tener entre 64 y 64 caracteres")
    private String evidenciaSha256;

    @Size(max = 1000, message = "observacionTecnica no debe superar 1000 caracteres")
    private String observacionTecnica;
}
