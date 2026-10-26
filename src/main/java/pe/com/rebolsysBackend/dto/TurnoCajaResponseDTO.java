package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoCajaResponseDTO {

    private Long idTurno;

    private Long idUsuario;

    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;

    private BigDecimal montoBase;

    private Integer estadoSesion;
}
