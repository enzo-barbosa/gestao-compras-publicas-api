package com.gestaocompras.config;

import com.gestaocompras.dto.ErroResposta;
import com.gestaocompras.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "rate-limit.habilitado", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, String> ROTAS = Map.of(
            "/api/auth/login", "login",
            "/api/auth/register", "registro",
            "/api/auth/esqueci-senha", "esqueci-senha",
            "/api/auth/redefinir-senha", "redefinir-senha");

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitProperties properties,
            ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !ROTAS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String rota = ROTAS.get(request.getRequestURI());
        RateLimitProperties.Limite limite = properties.getLimites().get(rota);
        if (limite == null || limite.getRequisicoes() <= 0) {
            filterChain.doFilter(request, response);
            return;
        }
        String chave = rota + "|" + request.getRemoteAddr();
        RateLimitService.Decisao decisao =
                rateLimitService.verificar(chave, limite.getRequisicoes(), limite.getJanelaSegundos());
        if (!decisao.permitido()) {
            escreverLimiteExcedido(response, decisao.retryAfterSegundos());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void escreverLimiteExcedido(HttpServletResponse response, long retryAfter)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ErroResposta.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "Muitas tentativas. Tente novamente em " + retryAfter + " s."));
    }
}
