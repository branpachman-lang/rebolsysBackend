package pe.com.rebolsysBackend.config;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Acceso {
  String PUBLICO = "@PUBLICO";
  String SESION = "@SESION";
  String GERENTE = "@GERENTE";
  String CATALOGOS = "@CATALOGOS";
  String CIERRE = "@CIERRE";
  String[] value();

}
