package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import pe.com.rebolsysBackend.enums.EstadoItem;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

public record ItemFiltroDTO(
    @Size(max = 200) String buscar,
    @Positive Long idTipoItem,
    EstadoItem estado,
    @Positive Long idHabitacion,
    Boolean sinAsignar,
    EstadoConservacion estadoConservacion,
    @Min(0) Integer pagina,
    @Min(1) @Max(100) Integer tamanio) {
  public ItemFiltroDTO {
    buscar = buscar == null ? "" : buscar.trim();
    pagina = pagina == null ? 0 : pagina;
    tamanio = tamanio == null ? 20 : tamanio;
  }
}
