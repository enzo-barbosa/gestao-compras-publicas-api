package com.gestaocompras.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestaocompras.dto.LoginRequestDTO;
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
class OrganizacoesIntegrationTest {

    private static final String SENHA = "senhaSegura123";
    private static final String EMAIL_A = "org.a." + System.nanoTime() + "@gestao.com";
    private static final String EMAIL_B = "org.b." + System.nanoTime() + "@gestao.com";
    private static final String EMAIL_C = "org.c." + System.nanoTime() + "@gestao.com";
    private static final String EMAIL_D = "org.d." + System.nanoTime() + "@gestao.com";
    private static final String EMAIL_E = "org.e." + System.nanoTime() + "@gestao.com";
    private static final String NOME_ORG = "Grupo " + System.nanoTime();

    private static Long organizacaoId;
    private static Long usuarioAdminId;

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

    private String registrar(String email) {
        ResponseEntity<Map> resposta = troca("/api/auth/register", HttpMethod.POST,
                new HttpHeaders(),
                Map.of("nome", "Novo", "email", email, "senha", SENHA));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return email;
    }

    private String tokenDe(String email) {
        ResponseEntity<TokenResponseDTO> resposta = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(email, SENHA), TokenResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody().token();
    }

    private HttpHeaders comBearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders adminComOrga(String token, Long orgId) {
        HttpHeaders headers = comBearer(token);
        headers.set("X-Org-Id", String.valueOf(orgId));
        return headers;
    }

    private <T> ResponseEntity<Map> troca(String urlRelativa, HttpMethod metodo,
            HttpHeaders headers, Object body) {
        return http.exchange(url(urlRelativa), metodo, new HttpEntity<>(body, headers),
                Map.class);
    }

    private <T> ResponseEntity<java.util.List> trocaLista(String urlRelativa,
            HttpMethod metodo, HttpHeaders headers, Object body) {
        return http.exchange(url(urlRelativa), metodo, new HttpEntity<>(body, headers),
                java.util.List.class);
    }

    @Test
    @Order(1)
    void registroDeveSerPublico() {
        var resposta = http.postForEntity(url("/api/auth/register"), Map.of(
                "nome", "Novo", "email", EMAIL_A, "senha", SENHA), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @Order(2)
    void usuarioCriaGrupoEViraAdminComListaNoMe() {
        String token = tokenDe(EMAIL_A);
        TokenResponseDTO login = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_A, SENHA), TokenResponseDTO.class).getBody();
        usuarioAdminId = login.usuarioId();

        var criacao = troca("/api/organizacoes", HttpMethod.POST, comBearer(token),
                Map.of("nome", NOME_ORG));

        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        organizacaoId = ((Number) ((Map<?, ?>) criacao.getBody()).get("id")).longValue();
        assertThat((String) ((Map<?, ?>) criacao.getBody()).get("papel"))
                .isEqualTo("ADMIN");

        var minhas = trocaLista("/api/organizacoes", HttpMethod.GET, comBearer(token), null);
        assertThat(minhas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(minhas.getBody().stream()
                .anyMatch(m -> ((Number) ((Map<?, ?>) m).get("id")).longValue()
                        == organizacaoId)).isTrue();

        var me = troca("/api/auth/me", HttpMethod.GET, comBearer(token), null);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        var organizacoes = (java.util.List<?>) ((Map<?, ?>) me.getBody()).get("organizacoes");
        assertThat(organizacoes).hasSize(2);
        assertThat(organizacoes.stream()
                .anyMatch(g -> ((Number) ((Map<?, ?>) g).get("id")).longValue() == organizacaoId
                        && "ADMIN".equals(((Map<?, ?>) g).get("papel")))).isTrue();
        assertThat(organizacoes).allSatisfy(g ->
                assertThat(((Map<?, ?>) g).get("papel")).isEqualTo("ADMIN"));
    }

    @Test
    @Order(3)
    void naoMembroNaoPodeVerDetalhesDoGrupo() {
        String email = registrar(EMAIL_B);
        String token = tokenDe(email);

        var detalhe = troca("/api/organizacoes/" + organizacaoId, HttpMethod.GET,
                comBearer(token), null);

        assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat((String) ((Map<?, ?>) detalhe.getBody()).get("mensagem"))
                .contains("não é membro");
    }

    @Test
    @Order(4)
    void adminAdicionaMembroPorEmailEConvidadoPassaAVerOGrupo() {
        HttpHeaders admin = adminComOrga(tokenDe(EMAIL_A), organizacaoId);

        var adicao = troca("/api/organizacoes/" + organizacaoId + "/membros", HttpMethod.POST,
                admin, Map.of("email", EMAIL_B, "papel", "VISITANTE"));

        assertThat(adicao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) ((Map<?, ?>) adicao.getBody()).get("email")).isEqualTo(EMAIL_B);

        var agoraMembro = troca("/api/organizacoes/" + organizacaoId, HttpMethod.GET,
                comBearer(tokenDe(EMAIL_B)), null);
        assertThat(agoraMembro.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(5)
    void criadorNaoPodeSerRebaixadoNemRemovido() {
        HttpHeaders admin = adminComOrga(tokenDe(EMAIL_A), organizacaoId);

        var rebaixar = troca("/api/organizacoes/" + organizacaoId + "/membros/" + usuarioAdminId,
                HttpMethod.PUT, admin, Map.of("papel", "VISITANTE"));

        assertThat(rebaixar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var remover = troca("/api/organizacoes/" + organizacaoId + "/membros/" + usuarioAdminId,
                HttpMethod.DELETE, admin, null);
        assertThat(remover.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(6)
    void codigoDeAcessoUniversalConcedeVisitante() {
        HttpHeaders admin = adminComOrga(tokenDe(EMAIL_A), organizacaoId);

        var gerar = troca("/api/organizacoes/" + organizacaoId + "/codigo-acesso",
                HttpMethod.POST, admin, null);

        assertThat(gerar.getStatusCode()).isEqualTo(HttpStatus.OK);
        String codigo = (String) ((Map<?, ?>) gerar.getBody()).get("codigo");
        assertThat(codigo).isNotBlank();

        String email = registrar(EMAIL_C);
        String tokenConvidado = tokenDe(email);

        var aceite = troca("/api/convites/aceitar", HttpMethod.POST,
                comBearer(tokenConvidado), Map.of("codigo", codigo));

        assertThat(aceite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) ((Map<?, ?>) aceite.getBody()).get("id")).longValue())
                .isEqualTo(organizacaoId);
        assertThat((String) ((Map<?, ?>) aceite.getBody()).get("papel"))
                .isEqualTo("VISITANTE");

        var agoraMembro = troca("/api/organizacoes/" + organizacaoId, HttpMethod.GET,
                comBearer(tokenConvidado), null);
        assertThat(agoraMembro.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(7)
    void conviteNominalConcedeMembrosia() {
        String tokenConvidado = tokenDe(registrar(EMAIL_D));

        var convite = troca("/api/organizacoes/" + organizacaoId + "/convites", HttpMethod.POST,
                adminComOrga(tokenDe(EMAIL_A), organizacaoId),
                Map.of("email", EMAIL_D, "papel", "OPERADOR"));

        assertThat(convite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) ((Map<?, ?>) convite.getBody()).get("email")).isEqualTo(EMAIL_D);

        var pendentes = trocaLista("/api/convites/pendentes", HttpMethod.GET,
                comBearer(tokenConvidado), null);
        assertThat(pendentes.getStatusCode()).isEqualTo(HttpStatus.OK);
        var listaPendentes = (java.util.List<Map<String, Object>>) pendentes.getBody();
        Map<String, Object> convitePendente = listaPendentes.stream()
                .filter(c -> ((Number) c.get("organizacaoId")).longValue() == organizacaoId)
                .findFirst()
                .orElseThrow();

        var aceite = troca("/api/convites/"
                + ((Number) convitePendente.get("id")).longValue() + "/aceitar",
                HttpMethod.POST, comBearer(tokenConvidado), null);

        assertThat(aceite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) ((Map<?, ?>) aceite.getBody()).get("id")).longValue())
                .isEqualTo(organizacaoId);

        var agoraMembro = troca("/api/organizacoes/" + organizacaoId, HttpMethod.GET,
                comBearer(tokenConvidado), null);
        assertThat(agoraMembro.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(8)
    void membroNaoAdminNaoGerenciaMembros() {
        String tokenOperador = tokenDe(EMAIL_C);

        var tentativa = troca("/api/organizacoes/" + organizacaoId + "/membros",
                HttpMethod.POST, comBearer(tokenOperador),
                Map.of("email", "outro@x.com", "papel", "VISITANTE"));

        assertThat(tentativa.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(9)
    void adminRedefineSenhaDeMembroERevogaSessoes() {
        String tokenAdmin = tokenDe(EMAIL_A);
        Long usuarioOperador = membroIdPorEmail(tokenAdmin, EMAIL_C);
        String tokenAntigo = tokenDe(EMAIL_C);
        String novaSenha = "novaSenha456";

        var reset = troca("/api/organizacoes/" + organizacaoId + "/membros/" + usuarioOperador
                + "/senha", HttpMethod.PUT, adminComOrga(tokenAdmin, organizacaoId),
                Map.of("novaSenha", novaSenha));

        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var loginNovo = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_C, novaSenha), TokenResponseDTO.class);
        assertThat(loginNovo.getStatusCode()).isEqualTo(HttpStatus.OK);

        var loginAntigo = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(EMAIL_C, SENHA), Map.class);
        assertThat(loginAntigo.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var meAntigo = troca("/api/auth/me", HttpMethod.GET, comBearer(tokenAntigo), null);
        assertThat(meAntigo.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(10)
    void operadorNaoPodeRedefinirSenhaDeOutroMembro() {
        String tokenOperador = tokenDe(EMAIL_D);

        var tentativa = troca("/api/organizacoes/" + organizacaoId + "/membros/"
                + usuarioAdminId + "/senha", HttpMethod.PUT,
                adminComOrga(tokenOperador, organizacaoId), Map.of("novaSenha", "outraSenha456"));

        assertThat(tentativa.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(11)
    void criadorNaoPodeTerSenhaRedefinidaPorAdmin() {
        var tentativa = troca("/api/organizacoes/" + organizacaoId + "/membros/"
                + usuarioAdminId + "/senha", HttpMethod.PUT,
                adminComOrga(tokenDe(EMAIL_A), organizacaoId), Map.of("novaSenha", "outraSenha456"));

        assertThat(tentativa.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(12)
    void conviteNominalPodeSerRecusado() {
        String email = registrar(EMAIL_E);
        var convite = troca("/api/organizacoes/" + organizacaoId + "/convites", HttpMethod.POST,
                adminComOrga(tokenDe(EMAIL_A), organizacaoId),
                Map.of("email", email, "papel", "VISITANTE"));

        assertThat(convite.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long conviteId = ((Number) ((Map<?, ?>) convite.getBody()).get("id")).longValue();
        String token = tokenDe(email);

        var recusa = troca("/api/convites/" + conviteId + "/recusar", HttpMethod.POST,
                comBearer(token), null);

        assertThat(recusa.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var pendentes = trocaLista("/api/convites/pendentes", HttpMethod.GET,
                comBearer(token), null);
        assertThat(pendentes.getStatusCode()).isEqualTo(HttpStatus.OK);
        var listaPendentes = (java.util.List<Map<String, Object>>) pendentes.getBody();
        assertThat(listaPendentes.stream()
                .noneMatch(c -> ((Number) c.get("organizacaoId")).longValue()
                        == organizacaoId))
                .isTrue();

        var naoMembro = troca("/api/organizacoes/" + organizacaoId, HttpMethod.GET,
                comBearer(token), null);
        assertThat(naoMembro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @SuppressWarnings("unchecked")
    private Long membroIdPorEmail(String tokenAdmin, String email) {
        var membros = trocaLista("/api/organizacoes/" + organizacaoId + "/membros",
                HttpMethod.GET, adminComOrga(tokenAdmin, organizacaoId), null);
        assertThat(membros.getStatusCode()).isEqualTo(HttpStatus.OK);
        java.util.List<Map<String, Object>> lista =
                (java.util.List<Map<String, Object>>) membros.getBody();
        return lista.stream()
                .filter(membro -> email.equals(membro.get("email")))
                .map(membro -> ((Number) membro.get("usuarioId")).longValue())
                .findFirst()
                .orElseThrow();
    }
}