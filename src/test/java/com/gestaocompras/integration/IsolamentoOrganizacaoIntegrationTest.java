package com.gestaocompras.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IsolamentoOrganizacaoIntegrationTest {

    @LocalServerPort
    private int porta;

    @Autowired
    private OrganizacaoRepository organizacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MembroOrganizacaoRepository membroRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private HttpHeaders comBearerEOrganizacao(String token, Long organizacaoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Org-Id", String.valueOf(organizacaoId));
        return headers;
    }

    private String token(String email, String senha) {
        ResponseEntity<TokenResponseDTO> resposta = http.postForEntity(url("/api/auth/login"),
                new LoginRequestDTO(email, senha), TokenResponseDTO.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody().token();
    }

    private <T> ResponseEntity<Map> troca(String urlRelativa, HttpMethod metodo,
            HttpHeaders headers, Object body) {
        return http.exchange(url(urlRelativa), metodo, new HttpEntity<>(body, headers),
                Map.class);
    }

    private Usuario criarUsuario(String email) {
        return usuarioRepository.save(Usuario.builder()
                .nome("Integração " + email)
                .email(email)
                .senha(passwordEncoder.encode("senhaSegura123"))
                .perfil(Perfil.USUARIO)
                .build());
    }

    private Long criarOrganizacao(Usuario criador) {
        Organizacao organizacao = organizacaoRepository.save(Organizacao.builder()
                .nome("Org Teste " + System.nanoTime())
                .criadoEm(LocalDateTime.now())
                .criadoPor(criador)
                .build());
        return organizacao.getId();
    }

    private void vincular(Usuario usuario, Long organizacaoId, PapelOrganizacao papel) {
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId).orElseThrow();
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, usuario))
                .papel(papel)
                .desde(LocalDateTime.now())
                .build());
    }

    @Test
    void operadorDeOutraOrganizacaoNaoDeveEnxergarNemEscreverNosDadosDaOrigem() {
        String sufixo = String.valueOf(System.nanoTime());
        Usuario operador = criarUsuario("operador" + sufixo + "@x.com");
        Long orgSecundaria = criarOrganizacao(operador);
        vincular(operador, orgSecundaria, PapelOrganizacao.OPERADOR);
        String tokenOperador = token(operador.getEmail(), "senhaSegura123");

        String tokenAdmin = token("admin@admin.com", "admin");
        var criacao = troca("/api/dotacoes", HttpMethod.POST,
                comBearerEOrganizacao(tokenAdmin, 1L),
                Map.of("codigo", "7.1." + sufixo.substring(sufixo.length() - 5),
                        "descricao", "Dotação isolamento",
                        "saldoInicial", 5000, "anoExercicio", 2026));
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long dotacaoOrigem = ((Number) ((Map<?, ?>) criacao.getBody()).get("id")).longValue();

        var leituraDeFora = troca("/api/dotacoes/%d".formatted(dotacaoOrigem), HttpMethod.GET,
                comBearerEOrganizacao(tokenOperador, orgSecundaria), null);

        assertThat(leituraDeFora.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var escritaComOrgErrada = troca("/api/dotacoes", HttpMethod.POST,
                comBearerEOrganizacao(tokenOperador, 1L),
                Map.of("codigo", "7.2." + sufixo.substring(sufixo.length() - 5),
                        "descricao", "Invasão",
                        "saldoInicial", 100, "anoExercicio", 2026));

        assertThat(escritaComOrgErrada.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat((String) ((Map<?, ?>) escritaComOrgErrada.getBody()).get("mensagem"))
                .contains("não é membro");
    }

    @Test
    void visitanteSomenteLeEOperadorPodeEscreverNaPropriaOrganizacao() {
        String sufixo = String.valueOf(System.nanoTime());
        Usuario operador = criarUsuario("operador2" + sufixo + "@x.com");
        Long orgSecundaria = criarOrganizacao(operador);
        vincular(operador, orgSecundaria, PapelOrganizacao.OPERADOR);
        Usuario visitante = criarUsuario("visitante" + sufixo + "@x.com");
        vincular(visitante, orgSecundaria, PapelOrganizacao.VISITANTE);
        String tokenOperador = token(operador.getEmail(), "senhaSegura123");
        String tokenVisitante = token(visitante.getEmail(), "senhaSegura123");

        var escritaOperador = troca("/api/dotacoes", HttpMethod.POST,
                comBearerEOrganizacao(tokenOperador, orgSecundaria),
                Map.of("codigo", "7.3." + sufixo.substring(sufixo.length() - 5),
                        "descricao", "Dotação operador",
                        "saldoInicial", 5000, "anoExercicio", 2026));
        Long dotacao = ((Number) ((Map<?, ?>) escritaOperador.getBody()).get("id")).longValue();

        var leituraVisitante = troca("/api/dotacoes/%d".formatted(dotacao), HttpMethod.GET,
                comBearerEOrganizacao(tokenVisitante, orgSecundaria), null);
        var escritaVisitante = troca("/api/dotacoes", HttpMethod.POST,
                comBearerEOrganizacao(tokenVisitante, orgSecundaria),
                Map.of("codigo", "7.4." + sufixo.substring(sufixo.length() - 5),
                        "descricao", "Dotação visitante",
                        "saldoInicial", 100, "anoExercicio", 2026));

        assertThat(leituraVisitante.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(escritaVisitante.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}