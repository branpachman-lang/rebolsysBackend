package pe.com.rebolsysBackend.service.service;

import java.util.*;
import pe.com.rebolsysBackend.dto.*;

public interface GastoService {
  GastoResponseDTO registrar(GastoRequestDTO r);

  GastoResponseDTO obtener(Long id);

  List<GastoResponseDTO> listar(Long idTurno);

  GastoHistorialDTO historial(GastoFiltroDTO filtros);

  List<CatalogoItemResponseDTO> tipos();
}
