package pe.com.rebolsysBackend.service.impl;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.ProductoRequestDTO;
import pe.com.rebolsysBackend.dto.ProductoResponseDTO;
import pe.com.rebolsysBackend.entity.MovimientoInventario;
import pe.com.rebolsysBackend.entity.Producto;
import pe.com.rebolsysBackend.enums.EstadoProducto;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.CategoriaRepository;
import pe.com.rebolsysBackend.repository.MovimientoInventarioRepository;
import pe.com.rebolsysBackend.repository.ProductoRepository;
import pe.com.rebolsysBackend.repository.TipoMovimientoAlmacenRepository;
import pe.com.rebolsysBackend.repository.UsuarioRepository;
import pe.com.rebolsysBackend.service.service.ProductoService;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TipoMovimientoAlmacenRepository tiposMovimiento;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioRepository usuarios;
    private final Validator validator;

    @Override
    @Transactional
    public ProductoResponseDTO create(ProductoRequestDTO request) {
        validar(request);
        var p = new Producto();
        completar(p, request);
        p.setStock(request.getStock());
        productoRepository.saveAndFlush(p);
        if (p.getStock() > 0) registrarAjuste(p, p.getStock(), "Existencias iniciales del producto");
        return convertirResponse(p);
    }

    @Override
    @Transactional
    public ProductoResponseDTO update(Long id, ProductoRequestDTO request) {
        validar(request);
        var p = productoRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
        int actual = p.getStock();
        int nuevo = actual;
        if (request.getStockAnterior() != null && !request.getStock().equals(request.getStockAnterior())) {
            if (actual != request.getStockAnterior()) throw new ReglaNegocioException(
                "El stock cambió mientras editabas. Recarga el producto antes de ajustar sus existencias"
            );
            nuevo = request.getStock();
        } else if (request.getStockAnterior() == null && request.getStock() != actual) throw new ReglaNegocioException(
            "Recarga el producto para confirmar el stock actual antes de modificarlo"
        );
        completar(p, request);
        p.setStock(nuevo);
        productoRepository.save(p);
        if (nuevo != actual) registrarAjuste(p, nuevo - actual, "Ajuste desde el mantenimiento de productos");
        return convertirResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO read(Long id) {
        return convertirResponse(
            productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"))
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var p = productoRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
        p.setEstadoProducto(EstadoProducto.INACTIVO);
        productoRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> readAll() {
        return productoRepository.findAll(Sort.by("nombreProducto")).stream().map(this::convertirResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> catalogo() {
        return productoRepository
            .findByEstadoProductoOrderByNombreProductoAsc(EstadoProducto.ACTIVO)
            .stream()
            .map(this::convertirResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ProductoResponseDTO> listar(
        int pagina,
        int tamanio,
        String buscar,
        Long categoria,
        EstadoProducto estado
    ) {
        var page = productoRepository.buscar(
            buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT),
            categoria,
            estado,
            PageRequest.of(pagina, tamanio, Sort.by("nombreProducto").and(Sort.by("idProducto")))
        );
        return new PaginaResponseDTO<>(
            page.getContent().stream().map(this::convertirResponse).toList(),
            pagina,
            tamanio,
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    private void completar(Producto p, ProductoRequestDTO r) {
        p.setNombreProducto(r.getNombreProducto().trim());
        p.setPrecioActual(r.getPrecioActual());
        p.setStockMinimo(r.getStockMinimo());
        p.setEstadoProducto(r.getEstadoProducto());
        p.setCategoria(
            categoriaRepository
                .findById(r.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe"))
        );
        p.setImagenRuta(r.getImagenRuta() == null || r.getImagenRuta().isBlank() ? null : r.getImagenRuta().trim());
    }

    private void registrarAjuste(Producto producto, int diferencia, String observacion) {
        var tipo = tiposMovimiento
            .findByCodigo(diferencia > 0 ? "AJ_ENT" : "AJ_SAL")
            .orElseThrow(() -> new ReglaNegocioException("Falta configurar el tipo de ajuste de almacén"));
        if (tipo.getSigno() != (diferencia > 0 ? 1 : -1)) throw new ReglaNegocioException(
            "El signo del ajuste de almacén no es válido"
        );
        var m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimientoAlmacen(tipo);
        m.setCantidad(Math.abs(diferencia));
        m.setStockResultante(producto.getStock());
        m.setObservacion(observacion);
        m.setUsuario(
            usuarios
                .findByUsername("operacion_local")
                .orElseThrow(() ->
                    new RecursoNoEncontradoException("No existe el usuario responsable de registrar movimientos")
                )
        );
        movimientos.save(m);
    }

    private void validar(ProductoRequestDTO r) {
        var errores = validator.validate(r);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
    }

    private ProductoResponseDTO convertirResponse(Producto p) {
        return new ProductoResponseDTO(
            p.getIdProducto(),
            p.getNombreProducto(),
            p.getPrecioActual(),
            p.getStock(),
            p.getStockMinimo(),
            p.getCategoria().getIdCategoria(),
            p.getEstadoProducto(),
            p.getImagenRuta(),
            p.getCategoria().getNombreCategoria()
        );
    }
}
