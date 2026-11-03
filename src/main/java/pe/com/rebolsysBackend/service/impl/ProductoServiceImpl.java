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
import pe.com.rebolsysBackend.repository.CategoriaRepository;
import pe.com.rebolsysBackend.repository.MovimientoInventarioRepository;
import pe.com.rebolsysBackend.repository.ProductoRepository;
import pe.com.rebolsysBackend.service.service.ProductoService;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
  private final jakarta.persistence.EntityManager entityManager;

  private final ProductoRepository productoRepository;
  private final CategoriaRepository categoriaRepository;
  private final MovimientoInventarioRepository movimientos;
  private final Validator validator;
  private final ReglasPropuesta reglas;

  @Override
  @Transactional
  public ProductoResponseDTO create(ProductoRequestDTO request) {
    validar(request);
    var p = new Producto();
    completar(p, request);
    ReglasPropuesta.exigir(
        request.getStock() == 0,
        "Crea el producto sin existencias y registra después un ajuste de stock con su"
            + " responsable");
    p.setStock(0);
    productoRepository.saveAndFlush(p);

    return convertirResponse(p);
  }

  @Override
  @Transactional
  public ProductoResponseDTO update(Long id, ProductoRequestDTO request) {
    validar(request);
    var p =
        productoRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
    entityManager.refresh(p);
    int actual = p.getStock();
    ReglasPropuesta.exigir(
        request.getStock() == actual
            || (request.getStockAnterior() != null
                && request.getStock().equals(request.getStockAnterior())),
        "Usa la operación de ajuste para modificar existencias");
    completar(p, request);
    productoRepository.saveAndFlush(p);
    return convertirResponse(p);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductoResponseDTO read(Long id) {
    return convertirResponse(
        productoRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe")));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    var p =
        productoRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
    p.setEstadoProducto(EstadoProducto.INACTIVO);
    productoRepository.save(p);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductoResponseDTO> readAll() {
    return productoRepository.findAll(Sort.by("nombreProducto")).stream()
        .map(this::convertirResponse)
        .toList();
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
      int pagina, int tamanio, String buscar, Long categoria, EstadoProducto estado) {
    return listar(pagina, tamanio, buscar, categoria, estado, null);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponseDTO<ProductoResponseDTO> listar(
      int pagina, int tamanio, String buscar, Long categoria, EstadoProducto estado, Boolean stockBajo) {
    ReglasPropuesta.exigir(pagina >= 0 && tamanio >= 1 && tamanio <= 100,
        "La página debe ser positiva o cero y el tamaño debe estar entre 1 y 100");
    if (categoria != null && !categoriaRepository.existsById(categoria))
      throw new RecursoNoEncontradoException("La categoría no existe");
    var page =
        productoRepository.buscar(
            ReglasPropuesta.patronBusqueda(buscar),
            categoria,
            estado,
            stockBajo,
            PageRequest.of(pagina, tamanio, Sort.by("nombreProducto").and(Sort.by("idProducto"))));
    return new PaginaResponseDTO<>(
        page.getContent().stream().map(this::convertirResponse).toList(),
        pagina,
        tamanio,
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }

  private void completar(Producto p, ProductoRequestDTO r) {
    p.setNombreProducto(r.getNombreProducto().trim());
    p.setPrecioActual(r.getPrecioActual());
    p.setStockMinimo(r.getStockMinimo());
    p.setEstadoProducto(r.getEstadoProducto());
    p.setCategoria(
        categoriaRepository
            .findById(r.getIdCategoria())
            .orElseThrow(() -> new RecursoNoEncontradoException("La categoría no existe")));
    p.setImagenRuta(
        r.getImagenRuta() == null || r.getImagenRuta().isBlank() ? null : r.getImagenRuta().trim());
  }

  @Override
  @Transactional
  public ProductoResponseDTO ajustarStock(Long id, pe.com.rebolsysBackend.dto.AjusteStockDTO r) {
    reglas.validar(r);
    var u = reglas.usuario(r.idUsuario());
    var p =
        productoRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
    entityManager.refresh(p);
    String clave = ReglasPropuesta.hash("stock:" + r.claveOperacion());
    var previo = movimientos.findByClaveOperacion(clave);
    if (previo.isPresent()) {
      var m = previo.get();
      ReglasPropuesta.exigir(
          m.getProducto() != null
              && m.getProducto().getIdProducto().equals(id)
              && m.getUsuario().getIdUsuario().equals(r.idUsuario())
              && m.getStockResultante().equals(r.stockNuevo())
              && m.getCantidad() == Math.abs(r.stockNuevo() - r.stockAnterior())
              && m.getTipoMovimientoAlmacen().getSigno()
                  == Integer.signum(r.stockNuevo() - r.stockAnterior())
              && m.getObservacion().equals(r.observacion().trim()),
          "La clave de operación corresponde a otro ajuste");
      return convertirResponse(p);
    }
    ReglasPropuesta.exigir(
        p.getStock().equals(r.stockAnterior()),
        "El stock cambió; consulta el producto antes de ajustar sus existencias");
    int diferencia = r.stockNuevo() - r.stockAnterior();
    ReglasPropuesta.exigir(diferencia != 0, "El ajuste debe modificar las existencias");
    var m = new MovimientoInventario();
    m.setProducto(p);
    m.setUsuario(u);
    m.setTipoMovimientoAlmacen(
        reglas.tipo(diferencia > 0 ? "AJ_ENT" : "AJ_SAL", diferencia > 0 ? 1 : -1));
    m.setCantidad(Math.abs(diferencia));
    m.setStockResultante(r.stockNuevo());
    m.setObservacion(r.observacion().trim());
    m.setClaveOperacion(clave);
    p.setStock(r.stockNuevo());
    movimientos.saveAndFlush(m);
    return convertirResponse(p);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<pe.com.rebolsysBackend.dto.MovimientoRespuestaDTO> movimientos(Long id) {
    if (!productoRepository.existsById(id))
      throw new RecursoNoEncontradoException("El producto no existe");
    return movimientos.findByProductoIdProductoOrderByIdMovimientoDesc(id).stream()
        .map(ItemServiceImpl::movimiento)
        .toList();
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
        p.getCategoria().getNombreCategoria());
  }
}
