package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.CategoriaRequestDTO;
import pe.com.rebolsysBackend.dto.CategoriaResponseDTO;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface CategoriaService extends CrudService<CategoriaRequestDTO, CategoriaResponseDTO, Long> {}
