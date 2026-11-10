package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import pe.com.rebolsysBackend.enums.*;

public final class LimpiezaDTO {
  private LimpiezaDTO() {}

  public record FiltroHabitaciones(String numeroHabitacion, @Positive Long idTipoHabitacion,
      EstadoHabitacionLimpieza estado, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {}

  public record FiltroInformes(String nombre, EstadoInforme estado,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEmision,
      String numeroHabitacion) {}

  public record FiltroTickets(String nombre, String numeroHabitacion, TipoAccionMantenimiento tipoAccion,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEmision) {}

  public record Evidencia(@NotBlank @Size(max = 300) String evidenciaRuta,
      @NotNull @Pattern(regexp = "[a-fA-F0-9]{64}") String evidenciaSha256) {}

  public record ItemInforme(@NotNull @Positive Long idItem, @NotNull EstadoConservacion estadoEncontrado,
      @Size(max = 1000) String observaciones, @Valid Evidencia evidencia) {}

  public record EnviarInforme(@Size(max = 1000) String observaciones,
      @NotNull List<@NotNull @Valid ItemInforme> items) {}

  public record EditarInforme(@NotNull List<@NotNull @Valid ItemInforme> items) {}

  public record DatosTicket(@NotNull TipoAccionMantenimiento tipoAccion,
      @Size(max = 1000) String observacionTecnica,
      @Digits(integer = 8, fraction = 2) @PositiveOrZero BigDecimal costoReparacion) {}

  public record CrearTicket(@NotNull @Valid DatosTicket accion, @NotNull @Valid Evidencia evidencia) {}

  public record Responsable(Long idUsuario, String usuario) {}

  public record HabitacionResumen(Long idHabitacion, String numeroHabitacion, Long idTipoHabitacion,
      String tipoHabitacion, LocalDateTime fechaSalida, EstadoHabitacionLimpieza estado,
      Responsable responsable, boolean puedeIniciarLimpieza, boolean puedeRetomarBorrador) {}

  public record ItemHabitacion(Long idItem, String item, String estado) {}

  public record HabitacionDetalle(HabitacionResumen habitacion, List<ItemHabitacion> items,
      String estadoInforme, InformeDetalle ultimoInforme, String estadoTickets, List<TicketResumen> tickets) {}

  public record Borrador(Long idInformeRevision, HabitacionResumen habitacion, List<ItemHabitacion> items) {}

  public record InformeResumen(Long idInformeRevision, String nombre, Long idHabitacion, String numeroHabitacion,
      LocalDateTime fechaEnvio, EstadoInforme estado, Responsable responsable, boolean tieneIncidencias,
      long incidenciasPendientes, boolean tieneTickets, boolean puedeAprobar, boolean puedeEditar, boolean puedeEliminar) {}

  public record DetalleItem(Long idDetalleInforme, Long idItem, String item,
      EstadoConservacion estadoEncontrado, EstadoConservacion estadoActualItem, String observaciones,
      Evidencia evidencia, Long idTicket, boolean puedeCrearTicket) {}

  public record InformeDetalle(InformeResumen informe, Long idDetalleReserva, String observaciones,
      List<DetalleItem> items, List<TicketResumen> tickets) {}

  public record TicketResumen(Long idTicket, String nombre, Long idInformeRevision, Long idDetalleInforme,
      Long idHabitacion, String numeroHabitacion, Long idItem, String item, TipoAccionMantenimiento tipoAccion,
      EstadoTicket estado, LocalDateTime fechaGeneracion, boolean puedeEditar, boolean puedeEliminar) {}

  public record TicketDetalle(TicketResumen ticket, Long idEmpleado, LocalDateTime fechaResolucion,
      String observacionTecnica, BigDecimal costoReparacion, Evidencia evidencia) {}

  public record Inicio(Long idTurno, LocalDateTime fechaApertura, long informesEnviados, long informesPorAprobar,
      long informesAprobados, long ticketsGenerados, String alcance) {}
}
