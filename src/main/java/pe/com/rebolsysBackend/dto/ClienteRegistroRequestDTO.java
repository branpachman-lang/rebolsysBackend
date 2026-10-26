package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.validation.DocumentoIdentidadValido;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@DocumentoIdentidadValido
public class ClienteRegistroRequestDTO {

    @NotNull(message = "idTipoDocumento es obligatorio")
    private Long idTipoDocumento;

    @NotBlank(message = "numeroDocumento es obligatorio")
    @Size(max = 50, message = "numeroDocumento no debe superar 50 caracteres")
    private String numeroDocumento;

    @NotNull(message = "empresa es obligatorio")
    private Boolean empresa;

    @Size(max = 100, message = "nombres no debe superar 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "apellidos no debe superar 100 caracteres")
    private String apellidos;

    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @Size(max = 250, message = "razonSocial no debe superar 250 caracteres")
    private String razonSocial;

    @Size(max = 200, message = "direccionFiscal no debe superar 200 caracteres")
    private String direccionFiscal;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "correo no debe superar 100 caracteres")
    private String correo;

    @Size(max = 15, message = "telefono no debe superar 15 caracteres")
    private String telefono;

    @AssertTrue(message = "Completa nombres y apellidos, o la razón social de la empresa")
    @JsonIgnore
    public boolean isIdentidadValida() {
        return Boolean.TRUE.equals(empresa)
            ? razonSocial != null && !razonSocial.isBlank()
            : nombres != null && !nombres.isBlank() && apellidos != null && !apellidos.isBlank();
    }
}
