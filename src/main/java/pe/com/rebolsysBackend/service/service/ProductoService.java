package pe.com.rebolsysBackend.service.service;

import java.util.List;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.ProductoRequestDTO;
import pe.com.rebolsysBackend.dto.ProductoResponseDTO;
import pe.com.rebolsysBackend.enums.EstadoProducto;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface ProductoService extends CrudService<ProductoRequestDTO, ProductoResponseDTO, Long> {
    PaginaResponseDTO<ProductoResponseDTO> listar(int pagina, int tamanio, String buscar, Long categoria, EstadoProducto estado);
    PaginaResponseDTO<ProductoResponseDTO> listar(int pagina, int tamanio, String buscar, Long categoria, EstadoProducto estado, Boolean stockBajo);
    List<ProductoResponseDTO> catalogo();
    ProductoResponseDTO ajustarStock(Long id, pe.com.rebolsysBackend.dto.AjusteStockDTO r);
    java.util.List<pe.com.rebolsysBackend.dto.MovimientoRespuestaDTO> movimientos(Long id);
}
