package com.gestaocompras.config;

import tools.jackson.databind.ObjectMapper;
import com.gestaocompras.dto.ErroResposta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/organizacoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/organizacoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/organizacoes/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/organizacoes/**")
                                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/organizacoes/**")
                                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/organizacoes/**")
                                .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/convites/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**")
                                .hasAnyRole("ADMIN", "OPERADOR", "VISITANTE", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/**")
                                .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**")
                                .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**")
                                .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .anyRequest().hasAnyRole("ADMIN", "SUPER_ADMIN"))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint((requisicao, resposta, naoAutenticado) ->
                                escreverErro(resposta, HttpStatus.UNAUTHORIZED,
                                        "Autenticação obrigatória ou token inválido."))
                        .accessDeniedHandler((requisicao, resposta, acessoNegado) ->
                                escreverErro(resposta, HttpStatus.FORBIDDEN,
                                        "Você não tem permissão para esta operação.")))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escreverErro(HttpServletResponse resposta, HttpStatus status, String mensagem)
            throws java.io.IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getWriter(),
                ErroResposta.of(status.value(), status.getReasonPhrase(), mensagem));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Org-Id"));
        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);
        return origem;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
