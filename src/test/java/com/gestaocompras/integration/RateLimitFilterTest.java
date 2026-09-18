package com.gestaocompras.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "rate-limit.habilitado=true",
        "rate-limit.limites.login.requisicoes=2",
        "rate-limit.limites.login.janela-segundos=60"
})
class RateLimitFilterTest {

    private static final Map<String, String> CREDENCIAIS_INVALIDAS =
            Map.of("email", "nao-existe@exemplo.com", "senha", "senha-invalida");

    @LocalServerPort
    private int porta;

    private final RestTemplate http = criarSemTratamentoDeErro();

    private static RestTemplate criarSemTratamentoDeErro() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse resposta) {
                return false;
            }

            @Override
            public void handleError(java.net.URI url, org.springframework.http.HttpMethod metodo,
                    org.springframework.http.client.ClientHttpResponse resposta) {
            }
        });
        return template;
    }

    private String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    @Test
    void bloqueiaLoginCom429AposExcederLimite() {
        ResponseEntity<String> primeira =
                http.postForEntity(url("/api/auth/login"), CREDENCIAIS_INVALIDAS, String.class);
        ResponseEntity<String> segunda =
                http.postForEntity(url("/api/auth/login"), CREDENCIAIS_INVALIDAS, String.class);
        ResponseEntity<String> terceira =
                http.postForEntity(url("/api/auth/login"), CREDENCIAIS_INVALIDAS, String.class);

        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(terceira.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(terceira.getHeaders().getFirst("Retry-After")).isNotBlank();
        assertThat(terceira.getBody()).contains("Muitas tentativas");
    }

    @Test
    void naoAplicaLimiteForaDasRotasDeAutenticacao() {
        ResponseEntity<String> resposta = http.getForEntity(url("/api/auth/me"), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
