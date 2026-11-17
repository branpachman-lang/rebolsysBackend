package pe.com.rebolsysBackend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.config.Acceso;
import pe.com.rebolsysBackend.dto.SistemaDTO.*;
import pe.com.rebolsysBackend.dto.TipoHabitacionResponseDTO;
import pe.com.rebolsysBackend.service.impl.HospedajeService;
import pe.com.rebolsysBackend.service.impl.PoliticaSeguridadService;
import pe.com.rebolsysBackend.service.service.TipoHabitacionService;

@RestController
@RequestMapping("/api/v1/configuraciones")
@Tag(name = "Configuración del sistema", description = "Edición para GERENTE y consulta de marca para sesiones autenticadas.")
@Acceso(Acceso.GERENTE)
@RequiredArgsConstructor
public class SistemaController {
  private final HospedajeService hospedaje;
  private final PoliticaSeguridadService politica;
  private final TipoHabitacionService tipos;

  @GetMapping("/sistema/hospedaje")
  public Marca consultarHospedaje() {
    return hospedaje.marca();
  }

  @PutMapping("/sistema/hospedaje")
  public Marca guardarHospedaje(@Valid @RequestBody DatosHospedaje datos) {
    return hospedaje.guardar(datos);
  }

  @PutMapping(value = "/sistema/hospedaje/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Marca guardarLogo(@RequestPart("archivo") MultipartFile archivo) {
    return hospedaje.guardarLogo(archivo);
  }

  @Acceso(Acceso.SESION)
  @GetMapping("/marca")
  public Marca marca() {
    return hospedaje.marca();
  }

  @Acceso(Acceso.SESION)
  @GetMapping("/marca/logo/{nombre}")
  public ResponseEntity<Resource> logo(@PathVariable String nombre) {
    var logo = hospedaje.logo(nombre);
    return ResponseEntity.ok().cacheControl(CacheControl.noCache())
        .contentType(nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
        .body(logo);
  }

  @GetMapping("/sistema/seguridad")
  public Politica consultarPolitica() {
    return politica.consultar();
  }

  @PutMapping("/sistema/seguridad")
  public Politica guardarPolitica(@Valid @RequestBody Politica datos) {
    return politica.guardar(datos);
  }

  @GetMapping("/sistema/comerciales")
  public Iterable<TipoHabitacionResponseDTO> tiposComerciales() {
    return tipos.readAll();
  }

  @PutMapping("/sistema/comerciales/{id}")
  public TipoHabitacionResponseDTO guardarTipo(@PathVariable @Positive Long id,
      @Valid @RequestBody ParametrosComerciales datos) {
    return tipos.actualizarParametros(id, datos);
  }
}
