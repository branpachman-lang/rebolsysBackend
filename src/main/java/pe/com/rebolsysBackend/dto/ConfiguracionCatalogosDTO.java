package pe.com.rebolsysBackend.dto;

import java.util.List;
import pe.com.rebolsysBackend.enums.*;

public record ConfiguracionCatalogosDTO(
    List<TipoHabitacionResponseDTO> tiposHabitacion,
    List<TipoItemResponseDTO> tiposItem,
    List<CategoriaResponseDTO> categorias,
    List<EstadoHabitacion> estadosHabitacion,
    List<EstadoItem> estadosItem,
    List<EstadoConservacion> estadosConservacion,
    List<EstadoProducto> estadosProducto) {}
