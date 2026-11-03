package pe.com.rebolsysBackend.service.impl;

import java.util.List;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.service.service.*;

@Service
@RequiredArgsConstructor
public class ConfiguracionServiceImpl implements ConfiguracionService {
  private final TipoHabitacionService habitaciones;
  private final TipoItemService items;
  private final CategoriaService categorias;

  @Override
  @Transactional(readOnly = true)
  public ConfiguracionCatalogosDTO catalogos() {
    return new ConfiguracionCatalogosDTO(
        StreamSupport.stream(habitaciones.readAll().spliterator(), false).toList(),
        StreamSupport.stream(items.readAll().spliterator(), false).toList(),
        StreamSupport.stream(categorias.readAll().spliterator(), false).toList(),
        List.of(EstadoHabitacion.values()), List.of(EstadoItem.values()),
        List.of(EstadoConservacion.values()), List.of(EstadoProducto.values()));
  }
}
