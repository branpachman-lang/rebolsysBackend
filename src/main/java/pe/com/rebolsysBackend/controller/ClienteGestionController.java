package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
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

  @Acceso("CLIENTES:VER")
  @GetMapping("/{id}")
  public ClienteDetalleDTO obtener(@PathVariable Long id) {
      return servicio.obtener(id);
  }

  @Acceso("CLIENTES:CREAR")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClienteDetalleDTO crear(@Valid @RequestBody ClienteGuardarDTO r) {
      return servicio.guardar(null, r);
  }

  @Acceso("CLIENTES:EDITAR")
  @PutMapping("/{id}")
  public ClienteDetalleDTO editar(@PathVariable Long id, @Valid @RequestBody ClienteGuardarDTO r) {
      return servicio.guardar(id, r);
  }
}
