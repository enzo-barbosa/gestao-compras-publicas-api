package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.MembroPapelRequestDTO;
import com.gestaocompras.dto.MembroRequestDTO;
import com.gestaocompras.dto.OrganizacaoRequestDTO;
import com.gestaocompras.exception.NaoMembroException;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.UsuarioLogado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizacaoServiceTest {

    private static final long ORGANIZACAO_ID = 10L;
    private static final String EMAIL_ADMIN = "admin@org.com";

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private MembroOrganizacaoRepository membroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OrganizacaoService organizacaoService;

    private Usuario admin;
    private Usuario criador;
    private Organizacao organizacao;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, EMAIL_ADMIN);
        criador = usuario(7L, "criador@org.com");
        organizacao = organizacao(ORGANIZACAO_ID, "Prefeitura", criador);
        lenient().when(usuarioRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));
        lenient().when(organizacaoRepository.findById(ORGANIZACAO_ID))
                .thenReturn(Optional.of(organizacao));
        lenient().when(organizacaoRepository.findAll()).thenReturn(List.of(organizacao));
        lenient().when(organizacaoRepository.getReferenceById(ORGANIZACAO_ID))
                .thenReturn(organizacao);
    }

    private UsuarioLogado principal(String email) {
        return UsuarioLogado.membro(email, ORGANIZACAO_ID, "ADMIN");
    }

    private static Usuario usuario(Long id, String email) {
        return Usuario.builder().id(id).nome("Usuário " + id).email(email)
                .senha("$2a$10$abc").perfil(Perfil.USUARIO).build();
    }

    private static Organizacao organizacao(Long id, String nome, Usuario criador) {
        return Organizacao.builder().id(id).nome(nome).criadoEm(LocalDateTime.now())
                .criadoPor(criador).build();
    }

    private static MembroOrganizacao membro(Organizacao org, Usuario user, PapelOrganizacao papel) {
        return MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(org, user))
                .papel(papel)
                .desde(LocalDateTime.now())
                .build();
    }

    @Test
    void criarDeveSalvarOrganizacaoEAdicionarCriadorComoAdmin() {
        when(organizacaoRepository.save(any(Organizacao.class))).thenAnswer(invocacao -> {
            Organizacao salva = invocacao.getArgument(0);
            salva.setId(100L);
            return salva;
        });

        var resposta = organizacaoService.criar(principal(EMAIL_ADMIN),
                new OrganizacaoRequestDTO("Nova Org"));

        assertThat(resposta.id()).isEqualTo(100L);
        assertThat(resposta.nome()).isEqualTo("Nova Org");
        assertThat(resposta.papel()).isEqualTo("ADMIN");
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(PapelOrganizacao.ADMIN);
        assertThat(captor.getValue().getId().getUsuario().getId()).isEqualTo(admin.getId());
        assertThat(captor.getValue().getId().getOrganizacao().getId()).isEqualTo(100L);
    }

    @Test
    void criarComNomeDuplicadoDeveLancar409() {
        assertThatThrownBy(() -> organizacaoService.criar(principal(EMAIL_ADMIN),
                new OrganizacaoRequestDTO("prefeitura")))
                .isInstanceOf(RegistroDuplicadoException.class);
        verify(organizacaoRepository, never()).save(any(Organizacao.class));
    }

    @Test
    void listarMinhasDeveRetornarOrgsComPapel() {
        when(membroRepository.findByIdUsuarioId(admin.getId()))
                .thenReturn(List.of(membro(organizacao, admin, PapelOrganizacao.ADMIN)));

        var resposta = organizacaoService.listarMinhas(principal(EMAIL_ADMIN));

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).nome()).isEqualTo("Prefeitura");
        assertThat(resposta.get(0).papel()).isEqualTo("ADMIN");
    }

    @Test
    void buscarPorIdParaNaoMembroDeveLancar403() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizacaoService.buscarPorId(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN))).isInstanceOf(NaoMembroException.class);
    }

    @Test
    void renomearPorAdminDeveAlterarNome() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membro(organizacao, admin, PapelOrganizacao.ADMIN)));
        when(organizacaoRepository.findAll()).thenReturn(List.of(organizacao(999L, "Outra", criador)));

        var resposta = organizacaoService.renomear(ORGANIZACAO_ID, principal(EMAIL_ADMIN),
                new OrganizacaoRequestDTO("Prefeitura Nova"));

        assertThat(resposta.nome()).isEqualTo("Prefeitura Nova");
        assertThat(organizacao.getNome()).isEqualTo("Prefeitura Nova");
    }

    @Test
    void renomearPorMembroNaoAdminDeveLancar409() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membro(organizacao, admin, PapelOrganizacao.VISITANTE)));

        assertThatThrownBy(() -> organizacaoService.renomear(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN), new OrganizacaoRequestDTO("Novo")))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void adicionarMembroDeveSalvarComPapelSolicitado() {
        stubsAdmin();
        Usuario convidado = usuario(2L, "convidado@org.com");
        when(usuarioRepository.findByEmail("convidado@org.com")).thenReturn(Optional.of(convidado));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);
        when(membroRepository.save(any(MembroOrganizacao.class))).thenAnswer(invocacao ->
                invocacao.getArgument(0));

        var resposta = organizacaoService.adicionarMembro(ORGANIZACAO_ID, principal(EMAIL_ADMIN),
                new MembroRequestDTO("convidado@org.com", PapelOrganizacao.OPERADOR));

        assertThat(resposta.email()).isEqualTo("convidado@org.com");
        assertThat(resposta.papel()).isEqualTo("OPERADOR");
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(PapelOrganizacao.OPERADOR);
    }

    @Test
    void adicionarMembroNaoRegistradoDeveLancar404() {
        stubsAdmin();
        when(usuarioRepository.findByEmail("sumido@org.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizacaoService.adicionarMembro(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN),
                new MembroRequestDTO("sumido@org.com", PapelOrganizacao.VISITANTE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void adicionarMembroJaExistenteDeveLancar409() {
        stubsAdmin();
        Usuario convidado = usuario(2L, "convidado@org.com");
        when(usuarioRepository.findByEmail("convidado@org.com")).thenReturn(Optional.of(convidado));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> organizacaoService.adicionarMembro(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN),
                new MembroRequestDTO("convidado@org.com", PapelOrganizacao.VISITANTE)))
                .isInstanceOf(RegistroDuplicadoException.class);
        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void alterarPapelDoCriadorDeveLancar409() {
        stubsAdmin();
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, criador.getId()))
                .thenReturn(Optional.of(membro(organizacao, criador, PapelOrganizacao.ADMIN)));

        assertThatThrownBy(() -> organizacaoService.alterarPapel(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN), criador.getId(),
                new MembroPapelRequestDTO(PapelOrganizacao.VISITANTE)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void alterarPapelDeMembroComumPorAdminDeveFuncionar() {
        stubsAdmin();
        Usuario alvo = usuario(2L, "alvo@org.com");
        MembroOrganizacao membro = membro(organizacao, alvo, PapelOrganizacao.VISITANTE);
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(Optional.of(membro));

        var resposta = organizacaoService.alterarPapel(ORGANIZACAO_ID, principal(EMAIL_ADMIN), 2L,
                new MembroPapelRequestDTO(PapelOrganizacao.OPERADOR));

        assertThat(resposta.papel()).isEqualTo("OPERADOR");
    }

    @Test
    void removerASiMesmoDeveLancar409() {
        stubsAdmin();
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membro(organizacao, admin, PapelOrganizacao.ADMIN)));

        assertThatThrownBy(() -> organizacaoService.removerMembro(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN), admin.getId()))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verify(membroRepository, never()).delete(any(MembroOrganizacao.class));
    }

    @Test
    void removerMembroPorAdminDeveDeletar() {
        stubsAdmin();
        Usuario alvo = usuario(2L, "alvo@org.com");
        MembroOrganizacao membro = membro(organizacao, alvo, PapelOrganizacao.OPERADOR);
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(Optional.of(membro));

        organizacaoService.removerMembro(ORGANIZACAO_ID, principal(EMAIL_ADMIN), 2L);

        verify(membroRepository).delete(membro);
    }

    @Test
    void listarMembrosExigeMembresia() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizacaoService.listarMembros(ORGANIZACAO_ID,
                principal(EMAIL_ADMIN))).isInstanceOf(NaoMembroException.class);
    }

    @Test
    void listarMembrosDeveRetornarListaOrdenada() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membro(organizacao, admin, PapelOrganizacao.ADMIN)));
        Usuario beta = usuario(3L, "beta@org.com");
        Usuario alfa = usuario(2L, "alfa@org.com");
        when(membroRepository.findByIdOrganizacaoId(ORGANIZACAO_ID))
                .thenReturn(List.of(membro(organizacao, beta, PapelOrganizacao.VISITANTE),
                        membro(organizacao, alfa, PapelOrganizacao.OPERADOR)));

        var resposta = organizacaoService.listarMembros(ORGANIZACAO_ID, principal(EMAIL_ADMIN));

        assertThat(resposta).hasSize(2);
        assertThat(resposta.get(0).nome()).isEqualTo("Usuário 2");
        assertThat(resposta.get(1).nome()).isEqualTo("Usuário 3");
    }

    private void stubsAdmin() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membro(organizacao, admin, PapelOrganizacao.ADMIN)));
    }
}