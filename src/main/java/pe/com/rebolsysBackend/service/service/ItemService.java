package pe.com.rebolsysBackend.service.service;

import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface ItemService extends CrudService<ItemRequestDTO, ItemResponseDTO, Long> {
  @Override
  List<ItemResponseDTO> readAll();

  List<ItemResponseDTO> listar();

  PaginaResponseDTO<ItemResponseDTO> configuracion(ItemFiltroDTO filtro);

  List<ItemResponseDTO> enHabitacion(Long id);

  ItemResponseDTO conservacion(Long id, ItemConservacionDTO r);

  List<ItemHistorialDTO> historial(Long id);

  ItemResponseDTO obtener(Long id);

  ItemResponseDTO guardar(Long id, ItemRequestDTO r);

  ItemResponseDTO asignar(Long id, ItemAsignacionDTO r);

  List<MovimientoRespuestaDTO> movimientos(Long id);
}
