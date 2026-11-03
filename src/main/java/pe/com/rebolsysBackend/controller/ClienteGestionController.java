package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.ClienteService;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteGestionController {
  private final ClienteService servicio;

  @GetMapping("/{id}")
  public ClienteDetalleDTO obtener(@PathVariable Long id) {
      return servicio.obtener(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClienteDetalleDTO crear(@Valid @RequestBody ClienteGuardarDTO r) {
      return servicio.guardar(null, r);
  }

  @PutMapping("/{id}")
  public ClienteDetalleDTO editar(@PathVariable Long id, @Valid @RequestBody ClienteGuardarDTO r) {
      return servicio.guardar(id, r);
  }
}
