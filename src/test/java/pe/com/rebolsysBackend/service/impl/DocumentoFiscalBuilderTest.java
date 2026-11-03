package pe.com.rebolsysBackend.service.impl;

import static org.assertj.core.api.Assertions.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import pe.com.rebolsysBackend.dto.EmisionElectronicaRequestDTO;
import pe.com.rebolsysBackend.entity.*;
import tools.jackson.databind.ObjectMapper;

class DocumentoFiscalBuilderTest {
  private final ObjectMapper json=new ObjectMapper();
  private final DocumentoFiscalBuilder builder=new DocumentoFiscalBuilder(config(),json);
  static FacturacionApisPeruProperties config() {
    var p=new FacturacionApisPeruProperties();
    p.setRuc("20123456789"); p.setRazonSocial("HOSPEDAJE FICTICIO"); p.setDireccion("DIRECCION FICTICIA");
    p.setUbigeo("150101"); p.setDepartamento("LIMA"); p.setProvincia("LIMA"); p.setDistrito("LIMA");
    return p;
  }
  Comprobante cuenta(String monto) {
    var c=new Comprobante(); var tipo=new TipoComprobante();tipo.setCodigo("BOLETA");c.setTipoComprobante(tipo);
    c.setSerie("BINT");c.setNumeroComprobante("1");c.setFechaEmision(LocalDateTime.now());
    c.setReceptorTipoDoc("1");c.setReceptorNumeroDoc("11111111");c.setReceptorNombre("PERSONA FICTICIA");
    c.setTotalPagar(new BigDecimal(monto));return c;
  }
  ComprobanteDetalle linea(int numero,String monto) {
    var l=new ComprobanteDetalle();l.setNumeroLinea(numero);l.setDescripcion("Servicio "+numero);
    l.setCantidad(BigDecimal.ONE);l.setValorUnitario(new BigDecimal(monto));l.setIgv(BigDecimal.ZERO);return l;
  }
  EmisionElectronicaRequestDTO request(String tasa,List<EmisionElectronicaRequestDTO.TratamientoLinea> tratamientos,List<EmisionElectronicaRequestDTO.Cuota> cuotas) {
    return new EmisionElectronicaRequestDTO("prueba",1L,new BigDecimal(tasa),null,tratamientos,cuotas);
  }
  @Test void desglosaImpuestoIncluidoSinAumentarElTotal() {
    var d=builder.construir(cuenta("118"),List.of(linea(1,"118")),BigDecimal.ZERO,request("18",null,null));
    assertThat(d.subtotal()).isEqualByComparingTo("100");assertThat(d.igv()).isEqualByComparingTo("18");
    assertThat(d.total()).isEqualByComparingTo("118");
    var n=json.readTree(d.solicitud());assertThat(n.path("company").path("address").has("ubigueo")).isTrue();
    assertThat(n.path("formaPago").path("tipo").asString()).isEqualTo("Contado");
    assertThat(n.path("fechaEmision").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-05:00");
  }
  @Test void admitePorcentajeDecimalExplicito() {
    var d=builder.construir(cuenta("110.50"),List.of(linea(1,"110.50")),BigDecimal.ZERO,request("10.50",null,null));
    assertThat(d.subtotal()).isEqualByComparingTo("100");assertThat(d.igv()).isEqualByComparingTo("10.50");
  }
  @Test void combinaTratamientosPorLineaSinAplicarBeneficiosAutomaticos() {
    var t=List.of(new EmisionElectronicaRequestDTO.TratamientoLinea(2,"10",new BigDecimal("10.50")),
        new EmisionElectronicaRequestDTO.TratamientoLinea(3,"20",BigDecimal.ZERO));
    var d=builder.construir(cuenta("238.50"),List.of(linea(1,"118"),linea(2,"110.50"),linea(3,"10")),BigDecimal.ZERO,request("18",t,null));
    assertThat(d.subtotal()).isEqualByComparingTo("210");assertThat(d.igv()).isEqualByComparingTo("28.50");
    assertThat(json.readTree(d.solicitud()).path("mtoOperExoneradas").decimalValue()).isEqualByComparingTo("10");
  }
  @Test void redondeaPorLineaConCantidadFraccionada() {
    var l=linea(1,"39.3333");l.setCantidad(new BigDecimal("3"));
    var d=builder.construir(cuenta("118"),List.of(l),BigDecimal.ZERO,request("18",null,null));
    assertThat(d.subtotal().add(d.igv())).isEqualByComparingTo("118");
  }
  @Test void creditoExigeCuotasPorElSaldoYNoPorElTotal() {
    var r=request("18",null,List.of(new EmisionElectronicaRequestDTO.Cuota(LocalDate.now().plusDays(2),new BigDecimal("40"))));
    var n=json.readTree(builder.construir(cuenta("118"),List.of(linea(1,"118")),new BigDecimal("40"),r).solicitud());
    assertThat(n.path("formaPago").path("tipo").asString()).isEqualTo("Credito");
    assertThat(n.path("formaPago").path("monto").decimalValue()).isEqualByComparingTo("40");
    assertThat(n.path("cuotas").get(0).path("fechaPago").asString()).matches("\\d{4}-\\d{2}-\\d{2}T00:00:00-05:00");
    assertThatThrownBy(()->builder.construir(cuenta("118"),List.of(linea(1,"118")),new BigDecimal("41"),r)).hasMessageContaining("sumar");
  }
  @Test void rechazaLineaDesconocidaOImpuestoIncompatibleConExoneracion() {
    var t=new EmisionElectronicaRequestDTO.TratamientoLinea(1,"20",BigDecimal.ONE);
    assertThatThrownBy(()->builder.construir(cuenta("10"),List.of(linea(1,"10")),BigDecimal.ZERO,request("18",List.of(t),null)))
        .hasMessageContaining("tasa cero");
    var desconocido=new EmisionElectronicaRequestDTO.TratamientoLinea(2,"20",BigDecimal.ZERO);
    assertThatThrownBy(()->builder.construir(cuenta("10"),List.of(linea(1,"10")),BigDecimal.ZERO,request("18",List.of(desconocido),null)))
        .hasMessageContaining("inexistente");
  }
  @Test void facturaAdmitePersonaNaturalConRucYExigeDireccionFiscal() {
    var c=cuenta("118");c.getTipoComprobante().setCodigo("FACTURA");c.setSerie("FINT");c.setReceptorTipoDoc("6");c.setReceptorNumeroDoc("10123456789");
    assertThatThrownBy(()->builder.construir(c,List.of(linea(1,"118")),BigDecimal.ZERO,request("18",null,null))).hasMessageContaining("dirección");
    var r=new EmisionElectronicaRequestDTO("prueba",1L,new BigDecimal("18"),"DIRECCION FICTICIA",null,null);
    assertThat(json.readTree(builder.construir(c,List.of(linea(1,"118")),BigDecimal.ZERO,r).solicitud()).path("tipoDoc").asString()).isEqualTo("01");
  }
  @Test void leyendaConservaCentimosSinConvertirADouble() {
    assertThat(MontoEnLetras.soles(new BigDecimal("118.50"))).isEqualTo("SON CIENTO DIECIOCHO CON 50/100 SOLES");
    assertThat(MontoEnLetras.soles(new BigDecimal("1000000"))).isEqualTo("SON UN MILLÓN CON 00/100 SOLES");
  }
}
