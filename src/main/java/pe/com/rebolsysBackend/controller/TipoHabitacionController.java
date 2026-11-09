package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
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

  @Acceso("HABITACIONES:VER")
  @GetMapping
  public Iterable<TipoHabitacionResponseDTO> listar() {
    return servicio.readAll();
  }

  @Acceso("HABITACIONES:VER")
  @GetMapping("/{id}")
  public TipoHabitacionResponseDTO obtener(@PathVariable Long id) {
    return servicio.read(id);
  }

  @Acceso("HABITACIONES:CREAR_TIPO")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TipoHabitacionResponseDTO crear(@Valid @RequestBody TipoHabitacionRequestDTO r) {
    return servicio.create(r);
  }

  @Acceso("HABITACIONES:EDITAR_TIPO")
  @PutMapping("/{id}")
  public TipoHabitacionResponseDTO editar(@PathVariable Long id, @Valid @RequestBody TipoHabitacionRequestDTO r) {
    return servicio.update(id, r);
  }

  @Acceso("HABITACIONES:ELIMINAR_TIPO")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    servicio.delete(id);
  }
}
