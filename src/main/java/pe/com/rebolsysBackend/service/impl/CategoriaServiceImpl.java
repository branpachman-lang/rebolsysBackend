package pe.com.rebolsysBackend.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.CategoriaRequestDTO;
import pe.com.rebolsysBackend.dto.CategoriaResponseDTO;
import pe.com.rebolsysBackend.entity.Categoria;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.CategoriaRepository;
import pe.com.rebolsysBackend.service.service.CategoriaService;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public CategoriaResponseDTO create(CategoriaRequestDTO r) {
        if (categoriaRepository.existsByNombreCategoria(r.getNombreCategoria().trim())) throw new ReglaNegocioException(
            "Ya existe una categoría con ese nombre"
        );
        var c = new Categoria();
        c.setNombreCategoria(r.getNombreCategoria().trim());
        return convertirResponse(categoriaRepository.save(c));
    }

    @Override
    @Transactional
    public CategoriaResponseDTO update(Long id, CategoriaRequestDTO r) {
        var c = buscar(id);
        c.setNombreCategoria(r.getNombreCategoria().trim());
        return convertirResponse(categoriaRepository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO read(Long id) {
        return convertirResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> readAll() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc().stream().map(this::convertirResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        categoriaRepository.delete(buscar(id));
    }

    private Categoria buscar(Long id) {
        return categoriaRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe"));
    }

    private CategoriaResponseDTO convertirResponse(Categoria c) {
        return new CategoriaResponseDTO(c.getIdCategoria(), c.getNombreCategoria());
    }
}
