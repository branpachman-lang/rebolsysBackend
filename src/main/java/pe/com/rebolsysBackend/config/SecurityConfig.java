package pe.com.rebolsysBackend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import pe.com.rebolsysBackend.exception.dto.ErrorResponseDTO;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource,
        ObjectMapper objectMapper
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(HttpMethod.GET, "/api/health")
                    .permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/habitaciones/*/ocupacion")
                    .access((authentication, context) -> new AuthorizationDecision(esAccesoLocal(context.getRequest())))
                    .requestMatchers(
                        "/api/v1/habitaciones",
                        "/api/v1/reservas",
                        "/api/v1/reservas/**",
                        "/api/v1/documentos/**",
                        "/api/v1/recepcion/**",
                        "/api/v1/productos/**",
                        "/api/v1/categorias/**",
                        "/api/v1/ventas/**"
                    )
                    .permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll()
            )
            .exceptionHandling(errors ->
                errors
                    .authenticationEntryPoint((request, response, exception) ->
                        writeError(
                            objectMapper,
                            request,
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "Debe iniciar sesión para acceder a este recurso"
                        )
                    )
                    .accessDeniedHandler((request, response, exception) ->
                        writeError(
                            objectMapper,
                            request,
                            response,
                            HttpStatus.FORBIDDEN,
                            "No tiene permisos para realizar esta operación"
                        )
                    )
            );
        return http.build();
    }

    private static boolean esAccesoLocal(HttpServletRequest request) {
        return java.util.Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(request.getRemoteAddr());
    }

    private static void writeError(
        ObjectMapper mapper,
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response
            .getWriter()
            .write(
                mapper.writeValueAsString(
                    new ErrorResponseDTO(
                        java.time.LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI(),
                        null
                    )
                )
            );
    }
}
