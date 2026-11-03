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
import pe.com.rebolsysBackend.repository.ProductoRepository;
import pe.com.rebolsysBackend.service.service.CategoriaService;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productos;
    private final ReglasPropuesta reglas;

    @Override
    @Transactional
    public CategoriaResponseDTO create(CategoriaRequestDTO r) {
        reglas.validar(r);
        if (categoriaRepository.existsByNombreCategoria(r.getNombreCategoria().trim())) throw new ReglaNegocioException(
            "Ya existe una categoría con ese nombre"
        );
        var c = new Categoria();
        c.setNombreCategoria(r.getNombreCategoria().trim());
        return convertirResponse(categoriaRepository.saveAndFlush(c));
    }

    @Override
    @Transactional
    public CategoriaResponseDTO update(Long id, CategoriaRequestDTO r) {
        reglas.validar(r);
        var c = buscar(id);
        c.setNombreCategoria(r.getNombreCategoria().trim());
        return convertirResponse(categoriaRepository.saveAndFlush(c));
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
        var c = buscar(id);
        ReglasPropuesta.exigir(!productos.existsByCategoriaIdCategoria(id),
            "La categoría tiene productos asociados");
        categoriaRepository.delete(c);
        categoriaRepository.flush();
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
