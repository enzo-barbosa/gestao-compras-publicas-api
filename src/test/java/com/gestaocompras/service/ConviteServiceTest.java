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
    private static final String EMAIL_ADMIN = "admin@org.com";
    private static final String EMAIL_CONVIDADO = "convidado@org.com";

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

    @BeforeEach
    void setUp() {
        admin = usuario(1L, EMAIL_ADMIN);
        convidado = usuario(2L, EMAIL_CONVIDADO);
        organizacao = Organizacao.builder().id(ORGANIZACAO_ID).nome("Prefeitura")
                .criadoEm(LocalDateTime.now()).criadoPor(admin).build();
        lenient().when(usuarioRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));
        lenient().when(usuarioRepository.findByEmail(EMAIL_CONVIDADO))
                .thenReturn(Optional.of(convidado));
        lenient().when(organizacaoRepository.findById(ORGANIZACAO_ID))
                .thenReturn(Optional.of(organizacao));
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

    private static ConviteOrganizacao convite(Organizacao org, String email, String codigo,
            PapelOrganizacao papel) {
        return ConviteOrganizacao.builder()
                .id(1L)
                .organizacao(org)
                .email(email)
                .codigo(codigo)
                .papel(papel)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    void criarSemEmailNemCodigoDeveLancar400() {
        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(null, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(conviteRepository, never()).save(any(ConviteOrganizacao.class));
    }

    @Test
    void criarComEmailECodigoDeveLancar400() {
        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO("x@x.com", "ABC", PapelOrganizacao.OPERADOR)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criarPorEmailDeveSalvarConvitePendente() {
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);
        when(conviteRepository.findByEmailAndUsadoEmIsNullAndOrganizacaoId(EMAIL_CONVIDADO,
                ORGANIZACAO_ID)).thenReturn(Optional.empty());
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
    }

    @Test
    void criarPorCodigoDeveSalvarConvitePendente() {
        when(conviteRepository.findByCodigoAndUsadoEmIsNull("ABC")).thenReturn(Optional.empty());
        when(conviteRepository.save(any(ConviteOrganizacao.class))).thenAnswer(invocacao ->
                invocacao.getArgument(0));

        var resposta = conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(null, "ABC", PapelOrganizacao.VISITANTE));

        assertThat(resposta.codigo()).isEqualTo("ABC");
        assertThat(resposta.papel()).isEqualTo("VISITANTE");
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
        when(conviteRepository.findByEmailAndUsadoEmIsNullAndOrganizacaoId(EMAIL_CONVIDADO,
                ORGANIZACAO_ID))
                .thenReturn(Optional.of(convite(organizacao, EMAIL_CONVIDADO, null,
                        PapelOrganizacao.OPERADOR)));

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR)))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void criarPorCodigoJaPendenteDeveLancar409() {
        when(conviteRepository.findByCodigoAndUsadoEmIsNull("ABC"))
                .thenReturn(Optional.of(convite(organizacao, null, "ABC",
                        PapelOrganizacao.OPERADOR)));

        assertThatThrownBy(() -> conviteService.criar(ORGANIZACAO_ID, principalAdmin(),
                new ConviteRequestDTO(null, "ABC", PapelOrganizacao.OPERADOR)))
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
                .thenReturn(List.of(convite(organizacao, EMAIL_CONVIDADO, null,
                        PapelOrganizacao.OPERADOR)));

        var resposta = conviteService.listarPendentes(ORGANIZACAO_ID, principalAdmin());

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).organizacaoId()).isEqualTo(ORGANIZACAO_ID);
    }

    @Test
    void revogarConviteUsadoDeveLancar409() {
        var conviteUsado = convite(organizacao, EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR);
        conviteUsado.setUsadoEm(LocalDateTime.now());
        when(conviteRepository.findById(1L)).thenReturn(Optional.of(conviteUsado));

        assertThatThrownBy(() -> conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verify(conviteRepository, never()).delete(any(ConviteOrganizacao.class));
    }

    @Test
    void revogarConviteDeOutraOrganizacaoDeveLancar404() {
        var outro = convite(Organizacao.builder().id(999L).nome("Outra")
                .criadoEm(LocalDateTime.now()).criadoPor(admin).build(),
                EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR);
        when(conviteRepository.findById(1L)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void revogarConvitePendenteDeveDeletar() {
        when(conviteRepository.findById(1L))
                .thenReturn(Optional.of(convite(organizacao, EMAIL_CONVIDADO, null,
                        PapelOrganizacao.OPERADOR)));

        conviteService.revogar(ORGANIZACAO_ID, principalAdmin(), 1L);

        verify(conviteRepository).delete(any(ConviteOrganizacao.class));
    }

    @Test
    void aceitarPorCodigoDeveAdicionarMembroEMarcarUsado() {
        var convite = convite(organizacao, null, "ABC", PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByCodigoAndUsadoEmIsNull("ABC")).thenReturn(Optional.of(convite));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);

        var resposta = conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO("ABC"));

        assertThat(resposta.nome()).isEqualTo("Prefeitura");
        assertThat(resposta.papel()).isEqualTo("OPERADOR");
        assertThat(convite.getUsadoEm()).isNotNull();
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(PapelOrganizacao.OPERADOR);
        assertThat(captor.getValue().getId().getUsuario().getId()).isEqualTo(2L);
    }

    @Test
    void aceitarPorCodigoJaMembroDeveLancar409() {
        var convite = convite(organizacao, null, "ABC", PapelOrganizacao.OPERADOR);
        when(conviteRepository.findByCodigoAndUsadoEmIsNull("ABC")).thenReturn(Optional.of(convite));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO("ABC"))).isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void aceitarPorCodigoInvalidoDeveLancar404() {
        when(conviteRepository.findByCodigoAndUsadoEmIsNull("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conviteService.aceitarPorCodigo(principalConvidado(),
                new AceitarCodigoRequestDTO("XYZ"))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void aceitarPorEmailDeveEntrarNasOrgsPendentesESeIndiferenteParaJaMembro() {
        var pendente = convite(organizacao, EMAIL_CONVIDADO, null, PapelOrganizacao.OPERADOR);
        var outra = Organizacao.builder().id(51L).nome("Outra Org")
                .criadoEm(LocalDateTime.now()).criadoPor(admin).build();
        var jaMembro = convite(outra, EMAIL_CONVIDADO, null, PapelOrganizacao.VISITANTE);
        when(conviteRepository.findAllByEmailAndUsadoEmIsNull(EMAIL_CONVIDADO))
                .thenReturn(List.of(pendente, jaMembro));
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(ORGANIZACAO_ID, 2L))
                .thenReturn(false);
        when(membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(51L, 2L))
                .thenReturn(true);

        var resposta = conviteService.aceitarPorEmail(principalConvidado());

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).nome()).isEqualTo("Prefeitura");
        assertThat(pendente.getUsadoEm()).isNotNull();
        assertThat(jaMembro.getUsadoEm()).isNotNull();
        ArgumentCaptor<MembroOrganizacao> captor = ArgumentCaptor.forClass(MembroOrganizacao.class);
        verify(membroRepository).save(captor.capture());
        assertThat(captor.getValue().getId().getOrganizacao().getId()).isEqualTo(ORGANIZACAO_ID);
    }
}