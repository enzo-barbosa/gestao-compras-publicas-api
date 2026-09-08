package com.gestaocompras.config;

import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_EMAIL = "admin@admin.com";
    private static final String ORGANIZACAO_PADRAO = "Minha Organização";

    @Bean
    CommandLineRunner inicializarDados(UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            MembroOrganizacaoRepository membroRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc,
            @Value("${spring.profiles.active:}") String perfisAtivos) {
        return args -> {
            if (ehProduto(perfisAtivos)) {
                log.info("Profile 'prod' ativo: seed de desenvolvimento e organização padrão " +
                        "desabilitados (crie o admin manualmente em produção).");
                return;
            }
            Usuario admin = semearAdmin(usuarioRepository, passwordEncoder);
            Organizacao principal = assegurarOrganizacaoPadrao(
                    organizacaoRepository, membroRepository, admin);
            migrarDadosPreexistentes(jdbc, principal.getId());
            garantirColunasObrigatorias(jdbc);
            log.info("Organização padrão '{}' (id={}) pronta para o administrador {}.",
                    principal.getNome(), principal.getId(), admin.getEmail());
        };
    }

    private boolean ehProduto(String perfisAtivos) {
        return java.util.Arrays.asList(perfisAtivos.split(",")).contains("prod");
    }

    private Usuario semearAdmin(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        return usuarioRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            Usuario admin = usuarioRepository.save(Usuario.builder()
                    .nome("Administrador")
                    .email(ADMIN_EMAIL)
                    .senha(passwordEncoder.encode("admin"))
                    .perfil(Perfil.SUPER_ADMIN)
                    .build());
            log.info("Usuário administrador criado (email: {})", ADMIN_EMAIL);
            return admin;
        });
    }

    private Organizacao assegurarOrganizacaoPadrao(OrganizacaoRepository organizacaoRepository,
            MembroOrganizacaoRepository membroRepository, Usuario admin) {
        List<Organizacao> existentes = organizacaoRepository.findAll();
        Organizacao principal = existentes.stream()
                .filter(o -> ORGANIZACAO_PADRAO.equals(o.getNome()))
                .findFirst()
                .orElse(existentes.isEmpty() ? null : existentes.get(0));
        if (principal == null) {
            principal = organizacaoRepository.save(Organizacao.builder()
                    .nome(ORGANIZACAO_PADRAO)
                    .criadoEm(LocalDateTime.now())
                    .criadoPor(admin)
                    .build());
            log.info("Organização '{}' criada.", ORGANIZACAO_PADRAO);
        }
        var chave = new MembroOrganizacao.Id(principal, admin);
        if (!membroRepository.existsById(chave)) {
            membroRepository.save(MembroOrganizacao.builder()
                    .id(chave)
                    .papel(PapelOrganizacao.ADMIN)
                    .desde(LocalDateTime.now())
                    .build());
            log.info("Administrador {} vinculado como ADMIN da organização {}.",
                    admin.getEmail(), principal.getNome());
        }
        return principal;
    }

    private void migrarDadosPreexistentes(JdbcTemplate jdbc, Long organizacaoId) {
        migrarTabela(jdbc, "dotacoes_orcamentarias", organizacaoId);
        migrarTabela(jdbc, "fornecedores", organizacaoId);
        migrarTabela(jdbc, "licitacoes", organizacaoId);
        migrarTabela(jdbc, "contratos", organizacaoId);
        migrarTabela(jdbc, "empenhos", organizacaoId);
        migrarTabela(jdbc, "creditos_suplementares", organizacaoId);
    }

    private void migrarTabela(JdbcTemplate jdbc, String tabela, Long organizacaoId) {
        int atualizados = jdbc.update(
                "UPDATE " + tabela + " SET organizacao_id = ? WHERE organizacao_id IS NULL",
                organizacaoId);
        if (atualizados > 0) {
            log.info("Backfill {}: {} registro(s) movido(s) para a organização {}.",
                    tabela, atualizados, organizacaoId);
        }
    }

    private void garantirColunasObrigatorias(JdbcTemplate jdbc) {
        for (String tabela : List.of("dotacoes_orcamentarias", "fornecedores", "licitacoes",
                "contratos", "empenhos", "creditos_suplementares")) {
            jdbc.execute("ALTER TABLE " + tabela + " ALTER COLUMN organizacao_id SET NOT NULL");
        }
    }
}