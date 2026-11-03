package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {

  @NotNull(message = "idPerfil es obligatorio")
  private Long idPerfil;

  @jakarta.validation.constraints.Size(max = 500)
  private String restriccionesMedicas;
}
