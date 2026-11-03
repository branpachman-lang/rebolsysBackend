package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.TipoHabitacion;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.TipoHabitacionService;

@Service
@RequiredArgsConstructor
public class TipoHabitacionServiceImpl implements TipoHabitacionService {
  private final TipoHabitacionRepository tipos;
  private final HabitacionRepository habitaciones;
  private final ReglasPropuesta reglas;

  @Override
  @Transactional
  public TipoHabitacionResponseDTO create(TipoHabitacionRequestDTO r) {
    reglas.validar(r);
    var t = new TipoHabitacion();
    completar(t, r);
    return respuesta(tipos.saveAndFlush(t));
  }

  @Override
  @Transactional
  public TipoHabitacionResponseDTO update(Long id, TipoHabitacionRequestDTO r) {
    reglas.validar(r);
    var t = bloquear(id);
    exigir(!habitaciones.existsByTipoHabitacionIdTipoHabitacionAndCapacidadPersonasGreaterThan(id, r.capacidadMax()),
        "Hay habitaciones cuya capacidad supera el nuevo máximo del tipo");
    completar(t, r);
    return respuesta(tipos.saveAndFlush(t));
  }

  @Override
  @Transactional(readOnly = true)
  public TipoHabitacionResponseDTO read(Long id) {
    return respuesta(tipos.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de habitación no existe")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<TipoHabitacionResponseDTO> readAll() {
    return tipos.findAll(Sort.by("nombreTipo")).stream().map(this::respuesta).toList();
  }

  @Override
  @Transactional
  public void delete(Long id) {
    var t = bloquear(id);
    exigir(!habitaciones.existsByTipoHabitacionIdTipoHabitacion(id),
        "El tipo tiene habitaciones asociadas");
    tipos.delete(t);
    tipos.flush();
  }

  private TipoHabitacion bloquear(Long id) {
    return tipos.bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de habitación no existe"));
  }

  private void completar(TipoHabitacion t, TipoHabitacionRequestDTO r) {
    t.setNombreTipo(r.nombreTipo().trim());
    t.setCapacidadMax(r.capacidadMax());
    t.setPrecioBase(r.precioBase());
  }

  private TipoHabitacionResponseDTO respuesta(TipoHabitacion t) {
    return new TipoHabitacionResponseDTO(
        t.getIdTipoHabitacion(), t.getNombreTipo(), t.getCapacidadMax(), t.getPrecioBase());
  }
}
