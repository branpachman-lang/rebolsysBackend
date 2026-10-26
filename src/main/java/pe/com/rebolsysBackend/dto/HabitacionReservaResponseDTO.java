package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HabitacionReservaResponseDTO {

    private String referencia;

    private String cliente;

    private LocalDateTime ingreso;

    private LocalDateTime salida;

    private String estado;
}
