package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.TipoHabitacionRequestDTO;
import pe.com.rebolsysBackend.dto.TipoHabitacionResponseDTO;
import pe.com.rebolsysBackend.service.service.TipoHabitacionService;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuración de habitaciones")
@RequestMapping("/api/v1/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionController {
  private final TipoHabitacionService servicio;

  @GetMapping
  public Iterable<TipoHabitacionResponseDTO> listar() {
    return servicio.readAll();
  }

  @GetMapping("/{id}")
  public TipoHabitacionResponseDTO obtener(@PathVariable Long id) {
    return servicio.read(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TipoHabitacionResponseDTO crear(@Valid @RequestBody TipoHabitacionRequestDTO r) {
    return servicio.create(r);
  }

  @PutMapping("/{id}")
  public TipoHabitacionResponseDTO editar(@PathVariable Long id, @Valid @RequestBody TipoHabitacionRequestDTO r) {
    return servicio.update(id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    servicio.delete(id);
  }
}
