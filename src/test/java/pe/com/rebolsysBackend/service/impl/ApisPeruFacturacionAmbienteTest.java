package pe.com.rebolsysBackend.service.impl;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import tools.jackson.databind.ObjectMapper;

class ApisPeruFacturacionAmbienteTest {
  @Test void admiteLaFormaRealNombreBeta() throws Exception {
    probar(Map.of("nombre","beta","fe_url","https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService"),"20123456789",true);
  }
  @Test void mantieneCompatibilidadConNameBeta() throws Exception {
    probar(Map.of("name","beta"),"20123456789",true);
  }
  @Test void bloqueaNombreProduccion() throws Exception {
    probar(Map.of("nombre","produccion"),"20123456789",false);
  }
  @Test void bloqueaBetaConReceptorDeProduccion() throws Exception {
    probar(Map.of("nombre","beta","fe_url","https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService"),"20123456789",false);
  }
  @Test void bloqueaRucDeOtraEmpresa() throws Exception {
    probar(Map.of("nombre","beta"),"20000000001",false);
  }

  private void probar(Map<String,String> ambiente,String rucRespuesta,boolean permitir) throws Exception {
    var json=new ObjectMapper();
    var envios=new AtomicInteger();
    var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/api/v1/companies/1",exchange->{
      byte[] body=json.writeValueAsBytes(Map.of("id",1,"ruc",rucRespuesta,"environment",ambiente));
      exchange.getResponseHeaders().set("Content-Type","application/json");
      exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
    });
    server.createContext("/api/v1/invoice/send",exchange->{
      envios.incrementAndGet();exchange.getRequestBody().readAllBytes();
      byte[] body="{\"sunatResponse\":{\"success\":true,\"cdrResponse\":{\"id\":\"BINT-1\",\"code\":\"0\",\"accepted\":true,\"description\":\"Fixture local\"}}}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type","application/json");
      exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
    });
    server.start();
    try {
      var p=new FacturacionApisPeruProperties();p.setHabilitada(true);p.setToken("token-ficticio");p.setIdEmpresa(1L);
      p.setUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/api/v1");
      var cliente=new ApisPeruFacturacionClient(p,json);
      if (permitir) {
        assertThat(cliente.enviar("{}","20123456789","03","BINT",1).aceptado()).isTrue();
        assertThat(envios).hasValue(1);
      } else {
        assertThatThrownBy(()->cliente.enviar("{}","20123456789","03","BINT",1)).hasMessageContaining("beta");
        assertThat(envios).hasValue(0);
      }
    } finally { server.stop(0); }
  }
}
