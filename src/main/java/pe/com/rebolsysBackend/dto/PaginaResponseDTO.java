package pe.com.rebolsysBackend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaginaResponseDTO<T> {

    private List<T> contenido;

    private int pagina;

    private int tamanio;

    private long totalElementos;

    private int totalPaginas;

    private boolean ultima;
}
