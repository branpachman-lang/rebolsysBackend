package pe.com.rebolsysBackend.propuesta;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Entity;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.*;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class ModeloPropuestoTest {
  @Test
  void mapearExactamenteLas47TablasY293ColumnasDelDdlPropuestoConV15() throws Exception {
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();
    try {
      var sources = new MetadataSources(registry);
      var scanner = new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
      for (var candidate : scanner.findCandidateComponents("pe.com.rebolsysBackend.entity"))
        sources.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
      var metadata = (MetadataImplementor) sources.buildMetadata();
      metadata.validate();
      String sql =
          Files.readString(Path.of("src/main/resources/db/schema/DDL_REBOLSYS_PROPUESTO.sql"))
              .replace("\"", "");
      var tables =
          Pattern.compile("CREATE TABLE\\s+(\\w+)\\s*\\(([\\s\\S]*?)\\);", Pattern.CASE_INSENSITIVE)
              .matcher(sql);
      Map<String, Set<String>> esperado = new HashMap<>();
      while (tables.find()) {
        var columns =
            Pattern.compile(
                    "(?m)^\\s*(\\w+)\\s+(?:NUMBER|VARCHAR2|CHAR|DATE|TIMESTAMP|CLOB)\\b",
                    Pattern.CASE_INSENSITIVE)
                .matcher(tables.group(2));
        Set<String> nombres = new HashSet<>();
        while (columns.find()) nombres.add(columns.group(1).toUpperCase(Locale.ROOT));
        esperado.put(tables.group(1).toUpperCase(Locale.ROOT), nombres);
      }
      Map<String, Set<String>> real = new HashMap<>();
      for (var ns : metadata.getDatabase().getNamespaces())
        for (var table : ns.getTables())
          real.put(
              table.getName().toUpperCase(Locale.ROOT),
              table.getColumns().stream()
                  .map(c -> c.getName().toUpperCase(Locale.ROOT))
                  .collect(Collectors.toSet()));
      assertThat(esperado).hasSize(47);
      assertThat(real).isEqualTo(esperado);
      assertThat(real.values().stream().mapToInt(Set::size).sum()).isEqualTo(293);
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @Test
  void columnasCalculadasNoSeEscribenDesdeJava() throws Exception {
    for (Class<?> clase :
        List.of(
            pe.com.rebolsysBackend.entity.DetalleVenta.class,
            pe.com.rebolsysBackend.entity.ComprobanteDetalle.class))
      for (String campo : List.of("subtotal", "total")) {
        var c = clase.getDeclaredField(campo).getAnnotation(jakarta.persistence.Column.class);
        assertThat(c.insertable()).isFalse();
        assertThat(c.updatable()).isFalse();
      }
  }
}
