package pe.com.rebolsysBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  HttpSessionSecurityContextRepository sesiones() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, CorsConfigurationSource corsConfigurationSource, HttpSessionSecurityContextRepository sesiones,
      @Value("${app.sesion-prueba.habilitada:false}") boolean prueba, Environment entorno) throws Exception {
    if (prueba && entorno.matchesProfiles("prod")) {
      throw new IllegalStateException("La sesión de prueba no está disponible en producción");
    }
    http.formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .securityContext(contexto -> contexto.securityContextRepository(sesiones))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .exceptionHandling(errores -> errores.authenticationEntryPoint((req, res, e) -> res.sendError(401)));
    if (prueba) http.csrf(AbstractHttpConfigurer::disable);
    http.authorizeHttpRequests(auth -> {
      auth.requestMatchers("/api/health", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
          .requestMatchers(HttpMethod.GET, "/api/v1/sesion/csrf").permitAll()
          .requestMatchers(HttpMethod.POST, "/api/v1/sesion/iniciar").permitAll();
      if (prueba) auth.requestMatchers("/api/v1/**").permitAll();
      else auth.requestMatchers("/api/v1/**").authenticated();
      auth.anyRequest().denyAll();
    });
    return http.build();
  }
}
