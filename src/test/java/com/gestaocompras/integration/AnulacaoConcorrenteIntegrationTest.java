package com.gestaocompras.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnulacaoConcorrenteIntegrationTest {

    private static final String ORGANIZACAO_ID = "1";

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
            public void handleError(java.net.URI url,
                    org.springframework.http.HttpMethod metodo,
                    org.springframework.http.client.ClientHttpResponse resposta) {
            }
        });
        return template;
    }

    private String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    private String tokenDoAdmin() {
        ResponseEntity<TokenResponseDTO> resposta = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO("admin@admin.com", "admin"), TokenResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody().token();
    }

    private HttpHeaders adminComOrganizacao() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenDoAdmin());
        headers.set("X-Org-Id", ORGANIZACAO_ID);
        return headers;
    }

    private ResponseEntity<Map> troca(String urlRelativa, HttpMethod metodo,
            HttpHeaders headers, Object body) {
        return http.exchange(url(urlRelativa), metodo, new HttpEntity<>(body, headers),
                Map.class);
    }

    private Long criarId(String urlRelativa, HttpHeaders headers, Object body) {
        var resposta = troca(urlRelativa, HttpMethod.POST, headers, body);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = (Number) ((Map<?, ?>) resposta.getBody()).get("id");
        return id.longValue();
    }

    private static String cnpjValidoUnico() {
        int[] digitos = new int[14];
        long base = System.nanoTime() % 80000000 + 10000000;
        String baseStr = String.format("%08d0001", base);
        for (int i = 0; i < 12; i++) {
            digitos[i] = baseStr.charAt(i) - '0';
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += digitos[i] * pesos1[i];
        }
        int resto = soma % 11;
        digitos[12] = resto < 2 ? 0 : 11 - resto;
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += digitos[i] * pesos2[i];
        }
        resto = soma % 11;
        digitos[13] = resto < 2 ? 0 : 11 - resto;
        StringBuilder sb = new StringBuilder();
        for (int d : digitos) {
            sb.append(d);
        }
        return sb.toString();
    }

    private BigDecimal saldoDaDotacao(Long dotacaoId, HttpHeaders admin) {
        ResponseEntity<Object> resposta = http.exchange(
                url("/api/dotacoes/%d/saldo".formatted(dotacaoId)),
                HttpMethod.GET, new HttpEntity<>(null, admin), Object.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new BigDecimal(String.valueOf((Number) resposta.getBody()));
    }

    private BigDecimal saldoRestanteDoContrato(Long contratoId, HttpHeaders admin) {
        var resposta = troca("/api/contratos/%d".formatted(contratoId),
                HttpMethod.GET, admin, null);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new BigDecimal(String.valueOf(((Map<?, ?>) resposta.getBody()).get("saldoRestante")));
    }

    @Test
    void anulacoesConcorrentesDevemResultarEmUmaUnicaOperacaoAceitaEUmUnicoEstorno() throws Exception {
        HttpHeaders admin = adminComOrganizacao();
        String sufixo = String.valueOf(System.nanoTime());

        Long dotacaoId = criarId("/api/dotacoes", admin, Map.of(
                "codigo", "9.9." + sufixo.substring(sufixo.length() - 6),
                "descricao", "Dotação anulação concorrente",
                "saldoInicial", 20000, "anoExercicio", 2026));
        String cnpj = cnpjValidoUnico();
        Long fornecedorId = criarId("/api/fornecedores", admin, Map.of(
                "nome", "Fornecedor Anulação " + sufixo, "cnpj", cnpj));
        Long licitacaoId = criarId("/api/licitacoes", admin, Map.of(
                "numeroEdital", "AC-" + sufixo + "/2026",
                "modalidade", "DISPENSA",
                "objeto", "Objeto anulação concorrente",
                "dataAbertura", "2026-01-01",
                "valorEstimado", 20000));
        troca("/api/licitacoes/%d/vencedor".formatted(licitacaoId), HttpMethod.PUT, admin,
                Map.of("fornecedorId", fornecedorId));
        Long contratoId = criarId("/api/contratos", admin, Map.of(
                "numero", "AC-" + sufixo + "/2026",
                "objeto", "Contrato anulação concorrente",
                "valorTotal", 20000, "duracaoMeses", 2,
                "dataInicio", "2026-01-01",
                "dotacaoId", dotacaoId, "licitacaoId", licitacaoId,
                "fornecedorId", fornecedorId));
        Long empenhoId = criarId("/api/empenhos", admin, Map.of(
                "contratoId", contratoId, "mesReferencia", 1, "anoReferencia", 2026));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<HttpStatus> statuses;
        try {
            CountDownLatch pronto = new CountDownLatch(2);
            CountDownLatch disparo = new CountDownLatch(1);
            List<Future<HttpStatus>> futuros = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futuros.add(executor.submit(() -> {
                    pronto.countDown();
                    disparo.await(10, TimeUnit.SECONDS);
                    return (HttpStatus) troca("/api/empenhos/%d".formatted(empenhoId),
                            HttpMethod.DELETE, admin, null).getStatusCode();
                }));
            }
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            disparo.countDown();

            statuses = new ArrayList<>();
            for (Future<HttpStatus> futuro : futuros) {
                statuses.add(futuro.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        assertThat(saldoDaDotacao(dotacaoId, admin))
                .isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(saldoRestanteDoContrato(contratoId, admin))
                .isEqualByComparingTo(new BigDecimal("20000"));
    }
}