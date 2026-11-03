package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;

public record HabitacionFiltroDTO(
    @Size(max = 200) String buscar,
    @Positive Long idTipoHabitacion,
    EstadoHabitacion estado,
    @Min(1) @Max(99) Integer capacidadMinima,
    @Min(0) Integer pagina,
    @Min(1) @Max(100) Integer tamanio) {
  public HabitacionFiltroDTO {
    buscar = buscar == null ? "" : buscar.trim();
    pagina = pagina == null ? 0 : pagina;
    tamanio = tamanio == null ? 20 : tamanio;
  }
}
