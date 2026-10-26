package pe.com.rebolsysBackend.dto;

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
public class TicketMantenimientoResponseDTO {

    private Long idTicket;

    private Long idDetalleInforme;

    private Long idEmpleado;

    private TipoAccionMantenimiento tipoAccion;

    private EstadoTicket estadoTicket;

    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;

    private BigDecimal costoReparacion;

    private String evidenciaRuta;

    private String evidenciaSha256;

    private String observacionTecnica;
}
