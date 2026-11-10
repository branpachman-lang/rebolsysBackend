package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.*;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.service.service.LimpiezaService;

@RestController
@RequestMapping("/api/v1/limpieza")
@RequiredArgsConstructor
@Tag(name = "Limpieza y mantenimiento")
public class LimpiezaController {
  private final LimpiezaService servicio;

  @Acceso("LIMPIEZA:VER_INICIO")
  @GetMapping("/inicio")
  @Operation(summary = "Consultar el resumen de limpieza según el rol del usuario")
  public Inicio inicio() {
    return servicio.inicio();
  }

  @Acceso("LIMPIEZA:VER_HABITACIONES")
  @GetMapping("/habitaciones")
  @Operation(summary = "Listar habitaciones con su estado en el flujo de limpieza")
  public List<HabitacionResumen> habitaciones(@Valid @ParameterObject @ModelAttribute FiltroHabitaciones filtro) {
    return servicio.habitaciones(filtro);
  }

  @Acceso("LIMPIEZA:VER_HABITACIONES")
  @GetMapping("/habitaciones/{id}")
  @Operation(summary = "Ver mobiliario y último informe visible de una habitación")
  public HabitacionDetalle habitacion(@PathVariable @Positive Long id) {
    return servicio.habitacion(id);
  }

  @Acceso("LIMPIEZA:INICIAR")
  @PostMapping("/habitaciones/{id}/iniciar")
  @Operation(summary = "Iniciar o retomar el borrador de limpieza de la habitación")
  public Borrador iniciar(@PathVariable @Positive Long id) {
    return servicio.iniciar(id);
  }

  @Acceso("LIMPIEZA:CANCELAR_BORRADOR")
  @DeleteMapping("/borradores/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Cancelar el borrador propio y devolver la habitación a Por limpiar")
  public void cancelar(@PathVariable @Positive Long id) {
    servicio.cancelarBorrador(id);
  }

  @Acceso("LIMPIEZA:ENVIAR_INFORME")
  @PostMapping("/borradores/{id}/enviar")
  @Operation(operationId = "enviarInformeLimpieza", summary = "Enviar el informe completo y dejarlo por aprobar")
  public InformeDetalle enviar(@PathVariable @Positive Long id, @Valid @RequestBody EnviarInforme datos) {
    return servicio.enviar(id, datos);
  }

  @Acceso("LIMPIEZA:VER_INFORMES")
  @GetMapping("/informes")
  @Operation(summary = "Buscar informes de limpieza enviados")
  public PaginaResponseDTO<InformeResumen> informes(@Valid @ParameterObject @ModelAttribute FiltroInformes filtro,
      @RequestParam(defaultValue = "0") @PositiveOrZero int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return servicio.informes(filtro, PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaHora", "idInformeRevision")));
  }

  @Acceso("LIMPIEZA:VER_INFORMES")
  @GetMapping("/informes/{id}")
  @Operation(summary = "Ver un informe, sus incidencias y sus tickets")
  public InformeDetalle informe(@PathVariable @Positive Long id) {
    return servicio.informe(id);
  }

  @Acceso("LIMPIEZA:EDITAR_INFORME")
  @PatchMapping("/informes/{id}")
  @Operation(summary = "Corregir los ítems indicados de un informe por aprobar")
  public InformeDetalle editarInforme(@PathVariable @Positive Long id, @Valid @RequestBody EditarInforme datos) {
    return servicio.editarInforme(id, datos);
  }

  @Acceso("LIMPIEZA:ELIMINAR_INFORME")
  @DeleteMapping("/informes/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Eliminar un informe por aprobar y sus tickets")
  public void eliminarInforme(@PathVariable @Positive Long id) {
    servicio.eliminarInforme(id);
  }

  @Acceso("LIMPIEZA:APROBAR_INFORME")
  @PostMapping("/informes/{id}/aprobar")
  @Operation(summary = "Aprobar un informe sin incidencias pendientes y liberar la habitación")
  public InformeDetalle aprobar(@PathVariable @Positive Long id) {
    return servicio.aprobar(id);
  }

  @Acceso("LIMPIEZA:CREAR_TICKET")
  @PostMapping("/informes/{id}/items/{idDetalleInforme}/ticket")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar la resolución de una incidencia con un ticket de mantenimiento")
  public TicketDetalle crearTicket(@PathVariable @Positive Long id, @PathVariable @Positive Long idDetalleInforme,
      @Valid @RequestBody CrearTicket datos) {
    return servicio.crearTicket(id, idDetalleInforme, datos);
  }

  @Acceso("LIMPIEZA:VER_TICKETS")
  @GetMapping("/tickets")
  @Operation(summary = "Buscar tickets de mantenimiento")
  public PaginaResponseDTO<TicketResumen> tickets(@Valid @ParameterObject @ModelAttribute FiltroTickets filtro,
      @RequestParam(defaultValue = "0") @PositiveOrZero int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return servicio.tickets(filtro, PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaGeneracion", "idTicket")));
  }

  @Acceso("LIMPIEZA:VER_TICKETS")
  @GetMapping("/tickets/{id}")
  @Operation(summary = "Ver un ticket de mantenimiento")
  public TicketDetalle ticket(@PathVariable @Positive Long id) {
    return servicio.ticket(id);
  }

  @Acceso("LIMPIEZA:EDITAR_TICKET")
  @PutMapping("/tickets/{id}")
  @Operation(summary = "Editar acción, observaciones y costo de un ticket de un informe por aprobar")
  public TicketDetalle editarTicket(@PathVariable @Positive Long id, @Valid @RequestBody DatosTicket datos) {
    return servicio.editarTicket(id, datos);
  }

  @Acceso("LIMPIEZA:ELIMINAR_TICKET")
  @DeleteMapping("/tickets/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Eliminar un ticket y reabrir la incidencia de su informe por aprobar")
  public void eliminarTicket(@PathVariable @Positive Long id) {
    servicio.eliminarTicket(id);
  }
}
