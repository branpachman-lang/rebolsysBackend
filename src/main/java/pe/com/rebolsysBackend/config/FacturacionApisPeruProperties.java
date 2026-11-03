package pe.com.rebolsysBackend.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="facturacion.apisperu")
@Getter @Setter
public class FacturacionApisPeruProperties {
  private boolean habilitada;
  private String url="https://facturacion.apisperu.com/api/v1";
  private String token="";
  private String usuario="";
  private String password="";
  private Long idEmpresa;
  private String ruc="";
  private String razonSocial="";
  private String direccion="";
  private String ubigeo="";
  private String departamento="";
  private String provincia="";
  private String distrito="";
  private int timeoutMillis=8000;

  public List<String> pendientes() {
    var p = new ArrayList<String>();
    if (token.isBlank() && (usuario.isBlank() || password.isBlank())) p.add("credencial de facturación (token o usuario y password)");
    if (idEmpresa == null || idEmpresa <= 0) p.add("idEmpresa de APIsPerú");
    if (!ruc.matches("[0-9]{11}")) p.add("RUC del emisor");
    if (razonSocial.isBlank()) p.add("razón social del emisor");
    if (direccion.isBlank()) p.add("dirección del emisor");
    if (!ubigeo.matches("[0-9]{6}")) p.add("ubigeo del emisor");
    if (departamento.isBlank() || provincia.isBlank() || distrito.isBlank()) p.add("departamento, provincia y distrito del emisor");
    return List.copyOf(p);
  }
}
