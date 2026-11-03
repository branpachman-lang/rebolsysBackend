package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.TipoItemRequestDTO;
import pe.com.rebolsysBackend.dto.TipoItemResponseDTO;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface TipoItemService extends CrudService<TipoItemRequestDTO, TipoItemResponseDTO, Long> {}
