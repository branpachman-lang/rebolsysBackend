package pe.com.rebolsysBackend.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import pe.com.rebolsysBackend.dto.ResultadoProveedorCpeDTO;
import pe.com.rebolsysBackend.exception.FacturacionProveedorException;
import pe.com.rebolsysBackend.service.service.ProveedorFacturacionClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ApisPeruFacturacionClient implements ProveedorFacturacionClient {
  private final FacturacionApisPeruProperties config;
  private final ObjectMapper json;
  private final RestClient http;
  private String tokenSesion;
  private Instant expira=Instant.EPOCH;

  public ApisPeruFacturacionClient(FacturacionApisPeruProperties config, ObjectMapper json) {
    this.config=config;
    this.json=json;
    URI uri=URI.create(config.getUrl());
    boolean local=Set.of("127.0.0.1","localhost","::1").contains(uri.getHost());
    if (!(local && "http".equals(uri.getScheme()))
        && !("https".equals(uri.getScheme()) && "facturacion.apisperu.com".equals(uri.getHost())))
      throw new IllegalArgumentException("La URL de facturación debe ser APIsPerú HTTPS o el simulador local");
    if (config.getTimeoutMillis()<100 || config.getTimeoutMillis()>60000)
      throw new IllegalArgumentException("El tiempo de espera de facturación debe estar entre 100 y 60000 ms");
    var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(config.getTimeoutMillis())).build());
    factory.setReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
    http=RestClient.builder().baseUrl(config.getUrl()).requestFactory(factory).build();
  }

  private synchronized String token() {
    if (!config.isHabilitada()) throw new FacturacionProveedorException("La facturación beta está deshabilitada", false);
    if (!config.getToken().isBlank()) return config.getToken();
    if (tokenSesion!=null && Instant.now().isBefore(expira)) return tokenSesion;
    try {
      var body=http.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("username",config.getUsuario(),"password",config.getPassword()))
          .retrieve().body(String.class);
      var node=json.readTree(body);
      if (!node.path("token").isString() || node.path("token").asString().isBlank())
        throw new IllegalArgumentException();
      tokenSesion=node.path("token").asString();
      expira=Instant.now().plus(Duration.ofHours(23));
      return tokenSesion;
    } catch (Exception ex) {
      throw new FacturacionProveedorException("No se pudo autenticar la cuenta de facturación de APIsPerú", false);
    }
  }

  private void verificarBeta(String ruc) {
    try {
      String body=http.get().uri("/companies/{id}",config.getIdEmpresa())
          .headers(h->h.setBearerAuth(token())).retrieve().body(String.class);
      var node=json.readTree(body);
      var ambiente=node.path("environment");
      boolean beta=ambiente.isString() && "beta".equalsIgnoreCase(ambiente.asString());
      boolean produccion=false;
      if (ambiente.isObject())
        for (String campo:List.of("nombre","code","name","value","id")) {
          var valor=ambiente.path(campo);
          beta |= valor.isString() && "beta".equalsIgnoreCase(valor.asString());
          produccion |= valor.isString() && Set.of("prod","production","produccion","producción").contains(valor.asString().toLowerCase(Locale.ROOT));
        }
      // El proveedor publica la dirección del receptor SUNAT dentro de environment.
      // Si la informa, debe coincidir con el servicio beta de facturas y boletas.
      var receptor=ambiente.path("fe_url");
      if (receptor.isString() && !receptor.asString().isBlank())
        produccion |= !"https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService".equals(receptor.asString());
      if (!ruc.equals(node.path("ruc").asString()) || !beta || produccion)
        throw new FacturacionProveedorException("APIsPerú debe confirmar el mismo RUC y ambiente beta; se bloqueó la operación",false);
    } catch (FacturacionProveedorException ex) { throw ex; }
    catch (Exception ex) {
      invalidarSiExpira(ex);
      throw new FacturacionProveedorException("No se pudo verificar la empresa beta en APIsPerú; no se envió el documento",false);
    }
  }

  @Override
  public ResultadoProveedorCpeDTO enviar(String solicitud,String ruc,String tipo,String serie,long numero) {
    verificarBeta(ruc);
    try {
      String body=http.post().uri("/invoice/send").headers(h->h.setBearerAuth(token()))
          .contentType(MediaType.APPLICATION_JSON).body(solicitud).retrieve().body(String.class);
      return interpretar(body,true,ruc,tipo,serie,numero);
    } catch (FacturacionProveedorException ex) { throw ex; }
    catch (RestClientResponseException ex) {
      invalidarSiExpira(ex);
      int codigo=ex.getStatusCode().value();
      boolean incierto=!Set.of(400,401,403,422).contains(codigo);
      throw new FacturacionProveedorException("APIsPerú respondió HTTP "+codigo
          +(incierto ? "; consultar el estado antes de reenviar" : "; revisar configuración o datos de la copia fiscal"),incierto);
    } catch (Exception ex) {
      throw new FacturacionProveedorException("El envío no pudo confirmarse; consultar el estado antes de reenviar",true);
    }
  }

  @Override
  public ResultadoProveedorCpeDTO consultar(String ruc,String tipo,String serie,long numero) {
    verificarBeta(ruc);
    try {
      String body=http.get().uri(b->b.path("/invoice/status").queryParam("tipo",tipo)
          .queryParam("serie",serie).queryParam("numero",numero).queryParam("ruc",ruc).build())
          .headers(h->h.setBearerAuth(token())).retrieve().body(String.class);
      return interpretar(body,false,ruc,tipo,serie,numero);
    } catch (FacturacionProveedorException ex) { throw ex; }
    catch (Exception ex) { throw new FacturacionProveedorException("No se pudo confirmar el CDR en APIsPerú; se conserva el documento",true); }
  }

  private ResultadoProveedorCpeDTO interpretar(String body,boolean envio,String ruc,String tipo,String serie,long numero) {
    var root=json.readTree(body);
    var resultado=envio ? root.path("sunatResponse") : root;
    var cdr=resultado.path("cdrResponse");
    String codigo=cdr.path("code").asString("");
    String id=cdr.path("id").asString("");
    if (!id.isBlank() && !Set.of(serie+"-"+numero,ruc+"-"+tipo+"-"+serie+"-"+numero).contains(id))
      throw new FacturacionProveedorException("El CDR recibido corresponde a otro documento; requiere revisión",true);
    Boolean aceptado=null;
    if (cdr.isObject() && !codigo.isBlank() && !id.isBlank() && resultado.path("success").asBoolean(false)) {
      if ("0".equals(codigo) && (!cdr.path("accepted").isBoolean() || cdr.path("accepted").asBoolean())) aceptado=true;
      else if (!"0".equals(codigo) && !cdr.path("accepted").asBoolean(false)) aceptado=false;
    }
    String mensaje=cdr.path("description").asString("");
    if (mensaje.isBlank()) mensaje=resultado.path("error").path("message").asString("CDR no confirmado; consultar antes de reenviar");
    if (codigo.isBlank()) codigo=resultado.path("error").path("code").asString("");
    return new ResultadoProveedorCpeDTO(envio?texto(root,"xml",2_000_000):null,
        envio?texto(root,"hash",100):null,texto(resultado,"cdrZip",7_000_000),
        limitar(codigo,20),limitar(sinSecretos(mensaje),500),aceptado);
  }

  private static String texto(JsonNode node,String campo,int max) {
    var value=node.path(campo);
    if (!value.isString() || value.asString().isBlank()) return null;
    if (value.asString().length()>max) throw new FacturacionProveedorException("La respuesta excede el tamaño admitido; requiere revisión",true);
    return value.asString();
  }
  private static String limitar(String s,int n) { return s.length()>n ? s.substring(0,n) : s; }

  private synchronized void invalidarSiExpira(Exception ex) {
    if (ex instanceof RestClientResponseException r && r.getStatusCode().value()==401) {
      tokenSesion=null; expira=Instant.EPOCH;
    }
  }
  private synchronized String sinSecretos(String mensaje) {
    for (String secreto:Arrays.asList(config.getToken(),config.getPassword(),tokenSesion))
      if (secreto!=null && !secreto.isBlank()) mensaje=mensaje.replace(secreto,"[OCULTO]");
    return mensaje;
  }

  @Override
  public byte[] pdf(String solicitud,String ruc) {
    verificarBeta(ruc);
    try {
      byte[] body=http.post().uri("/invoice/pdf").headers(h->h.setBearerAuth(token()))
          .contentType(MediaType.APPLICATION_JSON).body(solicitud).retrieve().body(byte[].class);
      if (body==null || body.length<5 || body.length>5_000_000
          || !new String(body,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
        throw new IllegalArgumentException();
      return body;
    } catch (Exception ex) { throw new FacturacionProveedorException("No se pudo obtener un PDF válido de APIsPerú",false); }
  }
}
