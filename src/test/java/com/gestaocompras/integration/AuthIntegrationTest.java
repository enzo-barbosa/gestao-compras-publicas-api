package com.gestaocompras.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.RegistroRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    private static final String EMAIL_USUARIO = "maria.operacional"
            + System.nanoTime() + "@gestao.com";

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

    private HttpHeaders comBearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private <T> ResponseEntity<Map> troca(String urlRelativa, HttpMethod metodo,
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

    @Test
    @Order(1)
    void loginDeveRetornarTokenParaOAdminDaSeed() {
        var resposta = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO("admin@admin.com", "admin"), TokenResponseDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().token()).isNotBlank();
        assertThat(resposta.getBody().perfil()).isEqualTo("ADMIN");
    }

    @Test
    @Order(2)
    void loginNaoDeveAceitarSenhaErrada() {
        var resposta = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO("admin@admin.com", "errada"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void requisicaoSemTokenDeveReceber401Json() {
        var resposta = http.getForEntity(url("/api/dotacoes"), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(4)
    void tokenAdulteradoDeveSerRecusado() {
        var resposta = http.exchange(url("/api/dotacoes"), HttpMethod.GET,
                new HttpEntity<>(comBearer(tokenDoAdmin().substring(0, 40) + "x")),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(5)
    void adminDeveRegistrarUsuarioELoginDeleDeveFuncionar() {
        String tokenAdmin = tokenDoAdmin();

        var registro = troca("/api/auth/register", HttpMethod.POST, comBearer(tokenAdmin),
                new RegistroRequestDTO("Maria Operacional", EMAIL_USUARIO, "senhaSegura123",
                        com.gestaocompras.model.Perfil.USUARIO));

        assertThat(registro.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var loginNovo = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_USUARIO, "senhaSegura123"), TokenResponseDTO.class);

        assertThat(loginNovo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginNovo.getBody().perfil()).isEqualTo("USUARIO");
    }

    @Test
    @Order(6)
    void usuarioComumNaoPodeRegistrarNemEscreverMasLe() {
        String tokenUsuario = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_USUARIO, "senhaSegura123"), TokenResponseDTO.class)
                .getBody().token();
        HttpHeaders headers = comBearer(tokenUsuario);

        var tentativaRegistro = troca("/api/auth/register", HttpMethod.POST, headers,
                new RegistroRequestDTO("Outro", "outro" + System.nanoTime()
                        + "@x.com", "senhaSegura123",
                        com.gestaocompras.model.Perfil.USUARIO));
        var tentativaEscrita = troca("/api/dotacoes", HttpMethod.POST, headers,
                Map.of("codigo", "X", "descricao", "x", "saldoInicial", 1, "anoExercicio", 2026));
        var leituraAutenticada = troca("/api/dotacoes", HttpMethod.GET, headers, null);

        assertThat(tentativaRegistro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(tentativaEscrita.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(leituraAutenticada.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(7)
    void usuarioComumDeveGerarEmpenhoComUsuarioIdPreenchido() {
        HttpHeaders admin = comBearer(tokenDoAdmin());
        String sufixo = String.valueOf(System.nanoTime());

        Long dotacaoId = criarId("/api/dotacoes", admin, Map.of(
                "codigo", "9.9." + sufixo.substring(sufixo.length() - 6),
                "descricao", "Dotação teste integração auth",
                "saldoInicial", 20000, "anoExercicio", 2026));
        String cnpj = cnpjValidoUnico();
        Long fornecedorId = criarId("/api/fornecedores", admin, Map.of(
                "nome", "Fornecedor Integração " + sufixo, "cnpj", cnpj));
        Long licitacaoId = criarId("/api/licitacoes", admin, Map.of(
                "numeroEdital", "IT-" + sufixo + "/2026",
                "modalidade", "DISPENSA",
                "objeto", "Objeto integração auth",
                "dataAbertura", "2026-01-01",
                "valorEstimado", 20000));
        troca("/api/licitacoes/%d/vencedor".formatted(licitacaoId), HttpMethod.PUT, admin,
                Map.of("fornecedorId", fornecedorId));
        Long contratoId = criarId("/api/contratos", admin, Map.of(
                "numero", "CT-" + sufixo + "/2026",
                "objeto", "Contrato integração auth",
                "valorTotal", 20000, "duracaoMeses", 2,
                "dataInicio", "2026-01-01",
                "dotacaoId", dotacaoId, "licitacaoId", licitacaoId,
                "fornecedorId", fornecedorId));

        String tokenUsuario = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_USUARIO, "senhaSegura123"), TokenResponseDTO.class)
                .getBody().token();

        var empenho = troca("/api/empenhos", HttpMethod.POST, comBearer(tokenUsuario), Map.of(
                "contratoId", contratoId, "mesReferencia", 1, "anoReferencia", 2026));

        assertThat(empenho.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) ((Map<?, ?>) empenho.getBody()).get("usuarioId")).longValue())
                .isNotNull();
    }
}
