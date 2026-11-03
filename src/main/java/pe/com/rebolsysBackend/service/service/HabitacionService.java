package pe.com.rebolsysBackend.service.service;

import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface HabitacionService extends CrudService<HabitacionRequestDTO, HabitacionResponseDTO, Long> {
  @Override
  List<HabitacionResponseDTO> readAll();

  List<HabitacionEstadoResponseDTO> estado(LocalDate desde, LocalDate hasta);

  List<HabitacionResponseDTO> listar();

  PaginaResponseDTO<HabitacionResponseDTO> configuracion(HabitacionFiltroDTO filtro);

  HabitacionResponseDTO cambiarEstado(Long id, HabitacionEstadoGuardarDTO r);

  HabitacionResponseDTO obtener(Long id);

  HabitacionResponseDTO guardar(Long id, HabitacionRequestDTO r);

  List<ItemResponseDTO> mobiliario(Long id);
}
