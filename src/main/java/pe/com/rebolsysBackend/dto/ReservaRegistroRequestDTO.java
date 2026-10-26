package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.FormaRegistro;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReservaRegistroRequestDTO {

    @NotBlank(message = "claveRegistro es obligatorio")
    @Size(max = 64, message = "claveRegistro no debe superar 64 caracteres")
    private String claveRegistro;

    @NotNull(message = "cliente es obligatorio")
    @Valid
    private ClienteRegistroRequestDTO cliente;

    @NotEmpty(message = "habitaciones debe contener al menos un elemento")
    private List<@Valid ReservaDetalleRequestDTO> habitaciones;

    @NotNull(message = "formaRegistro es obligatorio")
    private FormaRegistro formaRegistro;

    @DecimalMin(value = "0", message = "efectivoRecibido debe ser mayor o igual a 0")
    @Digits(integer = 8, fraction = 2, message = "efectivoRecibido admite hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal efectivoRecibido;

    @DecimalMin(value = "0.01", message = "El importe del pago debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2, message = "El importe del pago admite hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal montoPago;

    public ReservaRegistroRequestDTO(
        String claveRegistro,
        ClienteRegistroRequestDTO cliente,
        List<ReservaDetalleRequestDTO> habitaciones,
        FormaRegistro formaRegistro,
        BigDecimal efectivoRecibido
    ) {
        this(claveRegistro, cliente, habitaciones, formaRegistro, efectivoRecibido, null);
    }

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento del titular es obligatoria")
    public boolean isFechaNacimientoTitularValida() {
        return cliente == null || Boolean.TRUE.equals(cliente.getEmpresa()) || cliente.getFechaNacimiento() != null;
    }

    @JsonIgnore
    @AssertTrue(message = "El titular de la reserva debe tener al menos 18 años")
    public boolean isTitularAdulto() {
        return (
            cliente == null ||
            Boolean.TRUE.equals(cliente.getEmpresa()) ||
            cliente.getFechaNacimiento() == null ||
            !cliente.getFechaNacimiento().plusYears(18).isAfter(LocalDate.now(ZoneId.of("America/Lima")))
        );
    }

    @JsonIgnore
    @AssertTrue(message = "Todo menor debe tener padre, madre o tutor adulto registrado en la misma habitación")
    public boolean isResponsablesValidos() {
        if (habitaciones == null) return true;
        for (ReservaDetalleRequestDTO habitacion : habitaciones) {
            if (
                habitacion == null || habitacion.getFechaIngreso() == null || habitacion.getHuespedes() == null
            ) continue;
            LocalDate ingreso = habitacion.getFechaIngreso().toLocalDate();
            for (ReservaHuespedRegistroRequestDTO huesped : habitacion.getHuespedes()) {
                if (
                    huesped == null || huesped.getPersona() == null || huesped.getPersona().getFechaNacimiento() == null
                ) continue;
                boolean menor = huesped.getPersona().getFechaNacimiento().plusYears(18).isAfter(ingreso);
                ResponsableHuespedRequestDTO responsable = huesped.getResponsable();
                if (responsable == null) {
                    if (menor) return false;
                    else continue;
                }
                boolean adultoPresente = habitacion
                    .getHuespedes()
                    .stream()
                    .anyMatch(
                        adulto ->
                            adulto != null &&
                            adulto != huesped &&
                            adulto.getPersona() != null &&
                            adulto.getPersona().getFechaNacimiento() != null &&
                            !adulto.getPersona().getFechaNacimiento().plusYears(18).isAfter(ingreso) &&
                            java.util.Objects.equals(
                                adulto.getPersona().getIdTipoDocumento(),
                                responsable.getIdTipoDocumento()
                            ) &&
                            java.util.Objects.equals(
                                adulto.getPersona().getNumeroDocumento(),
                                responsable.getNumeroDocumento()
                            )
                    );
                if (!adultoPresente) return false;
            }
        }
        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "Solo los menores de 7 años al ingresar pueden alojarse sin plaza de cama propia")
    public boolean isUsoCamaValido() {
        if (habitaciones == null) return true;
        for (var habitacion : habitaciones) {
            if (
                habitacion == null || habitacion.getFechaIngreso() == null || habitacion.getHuespedes() == null
            ) continue;
            for (var huesped : habitacion.getHuespedes()) {
                if (huesped == null || !Boolean.FALSE.equals(huesped.getOcupaPlazaCama())) continue;
                if (
                    huesped.getPersona() == null ||
                    huesped.getPersona().getFechaNacimiento() == null ||
                    !huesped
                        .getPersona()
                        .getFechaNacimiento()
                        .plusYears(7)
                        .isAfter(habitacion.getFechaIngreso().toLocalDate())
                ) return false;
            }
        }
        return true;
    }
}
