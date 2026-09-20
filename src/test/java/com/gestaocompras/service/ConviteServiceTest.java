package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.AceitarCodigoRequestDTO;
import com.gestaocompras.dto.ConviteRequestDTO;
import com.gestaocompras.exception.NaoMembroException;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.ConviteOrganizacao;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ConviteOrganizacaoRepository;
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
class ConviteServiceTest {

    private static final long ORGANIZACAO_ID = 50L;
    private static final long OUTRA_ORGANIZACAO_ID = 51L;
    private static final String EMAIL_ADMIN = "admin@org.com";
    private static final String EMAIL_CONVIDADO = "convidado@org.com";
    private static final String EMAIL_DESCONHECIDO = "sumido@org.com";

    @Mock
    private ConviteOrganizacaoRepository conviteRepository;

    @Mock
    private MembroOrganizacaoRepository membroRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ConviteService conviteService;

    private Usuario admin;
    private Usuario convidado;
    private Organizacao organizacao;
    private Organizacao outraOrganizacao;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, EMAIL_ADMIN);
        convidado = usuario(2L, EMAIL_CONVIDADO);
        organizacao = Organizacao.builder().id(ORGANIZACAO_ID).nome("Prefeitura")
                .criadoEm(LocalDateTime.now()).criadoPor(admin).build();
        outraOrganizacao = Organizacao.builder().id(OUTRA_ORGANIZACAO_ID).nome("Outra Org")
                .criadoEm(LocalDateTime.now()).criadoPor(admin).build();
        lenient().when(usuarioRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));
        lenient().when(usuarioRepository.findByEmail(EMAIL_CONVIDADO))
                .thenReturn(Optional.of(convidado));
        lenient().when(organizacaoRepository.findById(ORGANIZACAO_ID))
                .thenReturn(Optional.of(organizacao));
        lenient().when(organizacaoRepository.getReferenceById(ORGANIZACAO_ID))
                .thenReturn(organizacao);
        lenient().when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(
                ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(membroAdmin()));
    }

    private UsuarioLogado principalAdmin() {
        return UsuarioLogado.membro(EMAIL_ADMIN, ORGANIZACAO_ID, "ADMIN");
    }

    private UsuarioLogado principalConvidado() {
        return UsuarioLogado.membro(EMAIL_CONVIDADO, null, null);
    }

    private static Usuario usuario(Long id, String email) {
        return Usuario.builder().id(id).nome("Usuário " + id).email(email)
                .senha("$2a$10$abc").perfil(Perfil.USUARIO).build();
    }

    private MembroOrganizacao membroAdmin() {
        return MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, admin))
                .papel(PapelOrganizacao.ADMIN)
                .desde(LocalDateTime.now())
                .build();
    }

    private static ConviteOrganizacao convite(Organizacao org, String email,
            PapelOrganizacao papel) {
        return ConviteOrganizacao.builder()
                .id(1L)
                .organizacao(org)
                .email(email)
                .papel(papel)
                .criadoPor(null)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    void criarSemEmailDeveLancar400() {
        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(null, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(conviteRepository, never()).save(any(ConviteOrganizacao.class));
    }

    @Test
    void criarComCodigoDeveLancar400() {
        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(null, "ABCD", PapelOrganizacao.VISITANTE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nominais");
        verify(conviteRepository, never()).save(any(ConviteOrganizacao.class));
    }

    @Test
    void criarPorEmailDeUsuarioNaoRegistradoDeveLancar404() {
        when(usuarioRepository.findByEmail(EMAIL_DESCONHECIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_DESCONHECIDO, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("registrar");
        verify(conviteRepository, never()).save(any(ConviteOrganizacao.class));
    }

    @Test
    void criarPorEmailDeveSalvarConvitePendente() {
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);
        when(conviteRepository.findByEmailAndUsadoEmIsNullAndRecusadoEmIsNullAndOrganizacaoId(
                EMAIL_CONVIDADO, ORGANIZACAO_ID)).thenReturn(Optional.empty());
        when(conviteRepository.save(any(ConviteOrganizacao.class))).thenAnswer(invocacao -> {
            ConviteOrganizacao salvo = invocacao.getArgument(0);
            salvo.setId(99L);
            return salvo;
        });

        var resposta = conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR));

        assertThat(resposta.id()).isEqualTo(99L);
        assertThat(resposta.email()).isEqualTo(EMAIL_CONVIDADO);
        assertThat(resposta.codigo()).isNull();
        assertThat(resposta.papel()).isEqualTo("OPERADOR");
        ArgumentCaptor<ConviteOrganizacao> captor =
                ArgumentCaptor.forClass(ConviteOrganizacao.class);
        verify(conviteRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL_CONVIDADO);
        assertThat(captor.getValue().getCodigo()).isNull();
        assertThat(captor.getValue().getExpiraEm()).isNull();
    }

    @Test
    void criarPorEmailDeUsuarioJaMembroDeveLancar409() {
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void criarPorEmailComConvitePendenteDeveLancar409() {
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);
        when(conviteRepository.findByEmailAndUsadoEmIsNullAndRecusadoEmIsNullAndOrganizacaoId(
                EMAIL_CONVIDADO, ORGANIZACAO_ID))
                .thenReturn(Optional.of(convite(organizacao, EMAIL_CONVIDADO,
                        PapelOrganizacao.OPERADOR)));

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void criarPorMembroNaoAdminDeveLancar409() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.of(MembroOrganizacao.builder()
                        .id(new MembroOrganizacao.Id(organizacao, admin))
                        .papel(PapelOrganizacao.VISITANTE)
                        .desde(LocalDateTime.now())
                        .build()));

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void listarPendentesDeveExigirAdmin() {
        when(membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, admin.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.listarPendentes(ORGANIZACAO_ID,
                principalAdmin())).isInstanceOf(NaoMembroException.class);
    }

    @Test
    void listarPendentesDeveRetornarApenasNaoUsados() {
        when(conviteRepository.findByOrganizacaoIdAndUsadoEmIsNull(ORGANIZACAO_ID))
                .thenReturn(List.of(convite(organizacao, EMAIL_CONVIDADO,
                        PapelOrganizacao.OPERADOR)));

        var resposta = conviteService.listarPendentes(ORGANIZACAO_ID, principalAdmin());

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).organizacaoId()).isEqualTo(ORGANIZACAO_ID);
    }

    @Test
    void revogarConviteUsadoDeveLancar409() {
        var conviteUsado = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        conviteUsado.setUsadoEm(LocalDateTime.now());
        when(conviteRepository.findById(1L)).thenReturn(Optional.of(conviteUsado));

        assertThatThrownBy(() -> conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verify(conviteRepository, never()).delete(any(ConviteOrganizacao.class));
    }

    @Test
    void revogarConviteDeOutraOrganizacaoDeveLancar404() {
        var outro = convite(outraOrganizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findById(1L)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void revogarConvitePendenteDeveDeletar() {
        when(conviteRepository.findById(1L))
                .thenReturn(Optional.of(convite(organizacao, EMAIL_CONVIDADO,
                        PapelOrganizacao.OPERADOR)));

        conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L);

        verify(conviteRepository).delete(any(ConviteOrganizacao.class));
    }

    @Test
    void meusPendentesDeveRetornarApenasConvitesDoMeuEmail() {
        var convites = List.of(
                convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR),
                convite(outraOrganizacao, EMAIL_CONVIDADO, PapelOrganizacao.VISITANTE));
        when(conviteRepository.findByEmailAndUsadoEmIsNullAndRecusadoEmIsNull(EMAIL_CONVIDADO))
                .thenReturn(convites);

        var resposta = conviteService.meusPendentes(principalConvidado());

        assertThat(resposta).hasSize(2);
        assertThat(resposta).extracting("organizacaoNome")
                .containsExactlyInAnyOrder("Prefeitura", "Outra Org");
    }

    @Test
    void aceitarNominalDeveAdicionarMembroComOPapelDoConviteEMarcarUsado() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNullAndRecusadoEmIsNull(
                99L, EMAIL_CONVIDADO)).thenReturn(Optional.of(pendente));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);

        var resposta = conviteService.aceitarNominal(principalConvidado(), 99L);

        assertThat(resposta.nome()).isEqualTo("Prefeitura");
        assertThat(resposta.papel()).isEqualTo("OPERADOR");
        assertThat(pendente.getUsadoEm()).isNotNull();
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(PapelOrganizacao.OPERADOR);
        assertThat(captor.getValue().getId().getUsuario().getId()).isEqualTo(2L);
    }

    @Test
    void aceitarNominalJaMembroDeveLancar409() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNullAndRecusadoEmIsNull(
                99L, EMAIL_CONVIDADO)).thenReturn(Optional.of(pendente));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.aceitarNominal(principalConvidado(), 99L))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void aceitarNominalDeConviteUsadoOuRecusadoDeveLancar404() {
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNullAndRecusadoEmIsNull(
                99L, EMAIL_CONVIDADO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.aceitarNominal(principalConvidado(), 99L))
                .isInstanceOf(NotFoundException.class);

        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void recusarNominalDeveMarcarRecusadoEm() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNull(99L, EMAIL_CONVIDADO))
                .thenReturn(Optional.of(pendente));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);

        conviteService.recusarNominal(principalConvidado(), 99L);

        assertThat(pendente.getRecusadoEm()).isNotNull();
        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void recusarNominalJaMembroDeveLancar409() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNull(99L, EMAIL_CONVIDADO))
                .thenReturn(Optional.of(pendente));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.recusarNominal(principalConvidado(), 99L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        assertThat(pendente.getRecusadoEm()).isNull();
    }

    @Test
    void recusarNominalJaRecusadoDeveLancar409() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, PapelOrganizacao.OPERADOR);
        pendente.setRecusadoEm(LocalDateTime.now());
        when(conviteRepository.findByIdAndEmailAndUsadoEmIsNull(99L, EMAIL_CONVIDADO))
                .thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> conviteService.recusarNominal(principalConvidado(), 99L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void aceitarPorCodigoDeveAdicionarMembroComoVisitante() {
        when(organizacaoRepository.findByCodigoAcesso("ABCD")).thenReturn(Optional.of(organizacao));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);

        var resposta = conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO(" abcd "));

        assertThat(resposta.nome()).isEqualTo("Prefeitura");
        assertThat(resposta.papel()).isEqualTo("VISITANTE");
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(PapelOrganizacao.VISITANTE);
        assertThat(captor.getValue().getId().getUsuario().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getId().getOrganizacao().getId()).isEqualTo(ORGANIZACAO_ID);
    }

    @Test
    void aceitarPorCodigoJaMembroDeveLancar409() {
        when(organizacaoRepository.findByCodigoAcesso("ABCD")).thenReturn(Optional.of(organizacao));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO("ABCD"))).isInstanceOf(RegistroDuplicadoException.class);

        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void aceitarPorCodigoInvalidoDeveLancar404() {
        when(organizacaoRepository.findByCodigoAcesso("XYZ0")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO("xyz0"))).isInstanceOf(NotFoundException.class);

        verify(membroRepository, never()).save(any(MembroOrganizacao.class));
    }

    @Test
    void normalizarCodigoDeveLimparETransformarEmMaiusculas() {
        assertThat(ConviteService.normalizarCodigo("  aB3cD ")).isEqualTo("AB3CD");
        assertThat(ConviteService.normalizarCodigo(null)).isNull();
    }
}