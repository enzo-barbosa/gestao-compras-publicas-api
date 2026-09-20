package com.gestaocompras.service;

import com.gestaocompras.dto.CodigoAcessoResponseDTO;
import com.gestaocompras.dto.MembroPapelRequestDTO;
import com.gestaocompras.dto.MembroRequestDTO;
import com.gestaocompras.dto.MembroResponseDTO;
import com.gestaocompras.dto.MembroSenhaRequestDTO;
import com.gestaocompras.dto.OrganizacaoRequestDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.exception.NaoMembroException;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.ConviteOrganizacaoRepository;
import com.gestaocompras.repository.CreditoSuplementarRepository;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.EmpenhoRepository;
import com.gestaocompras.repository.EmpenhoSequenciaRepository;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.MovimentacaoDotacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.UsuarioLogado;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacaoService {

    private static final char[] ALFABETO_CODIGO =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TAMANHO_CODIGO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizacaoRepository organizacaoRepository;
    private final MembroOrganizacaoRepository membroRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpenhoRepository empenhoRepository;
    private final EmpenhoSequenciaRepository empenhoSequenciaRepository;
    private final MovimentacaoDotacaoRepository movimentacaoRepository;
    private final CreditoSuplementarRepository creditoRepository;
    private final ContratoRepository contratoRepository;
    private final LicitacaoRepository licitacaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final DotacaoRepository dotacaoRepository;
    private final ConviteOrganizacaoRepository conviteRepository;

    public OrganizacaoService(OrganizacaoRepository organizacaoRepository,
            MembroOrganizacaoRepository membroRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            EmpenhoRepository empenhoRepository,
            EmpenhoSequenciaRepository empenhoSequenciaRepository,
            MovimentacaoDotacaoRepository movimentacaoRepository,
            CreditoSuplementarRepository creditoRepository,
            ContratoRepository contratoRepository,
            LicitacaoRepository licitacaoRepository,
            FornecedorRepository fornecedorRepository,
            DotacaoRepository dotacaoRepository,
            ConviteOrganizacaoRepository conviteRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.membroRepository = membroRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.empenhoRepository = empenhoRepository;
        this.empenhoSequenciaRepository = empenhoSequenciaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.creditoRepository = creditoRepository;
        this.contratoRepository = contratoRepository;
        this.licitacaoRepository = licitacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.dotacaoRepository = dotacaoRepository;
        this.conviteRepository = conviteRepository;
    }

    @Transactional
    public OrganizacaoResponseDTO criar(UsuarioLogado principal, OrganizacaoRequestDTO request) {
        Usuario criador = buscarAutenticado(principal);
        validarNomeDisponivel(request.nome());
        Organizacao organizacao = organizacaoRepository.save(Organizacao.builder()
                .nome(request.nome().trim())
                .criadoEm(LocalDateTime.now())
                .criadoPor(criador)
                .build());
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, criador))
                .papel(PapelOrganizacao.ADMIN)
                .desde(LocalDateTime.now())
                .build());
        return OrganizacaoResponseDTO.from(organizacao, PapelOrganizacao.ADMIN.name());
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponseDTO> listarMinhas(UsuarioLogado principal) {
        Usuario usuario = buscarAutenticado(principal);
        return membroRepository.findByIdUsuarioId(usuario.getId()).stream()
                .map(membro -> OrganizacaoResponseDTO.from(
                        membro.getId().getOrganizacao(), membro.getPapel().name()))
                .sorted(Comparator.comparing(OrganizacaoResponseDTO::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponseDTO buscarPorId(Long organizacaoId, UsuarioLogado principal) {
        Organizacao organizacao = org(organizacaoId);
        MembroOrganizacao membro = exigirMembro(organizacaoId, principal, buscarAutenticado(principal));
        return OrganizacaoResponseDTO.from(organizacao, papelResposta(membro, principal));
    }

    @Transactional
    public OrganizacaoResponseDTO renomear(Long organizacaoId, UsuarioLogado principal,
            OrganizacaoRequestDTO request) {
        Organizacao organizacao = org(organizacaoId);
        MembroOrganizacao membro = exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        validarNomeDisponivel(request.nome());
        organizacao.setNome(request.nome().trim());
        return OrganizacaoResponseDTO.from(organizacao, papelResposta(membro, principal));
    }

    @Transactional(readOnly = true)
    public List<MembroResponseDTO> listarMembros(Long organizacaoId, UsuarioLogado principal) {
        exigirMembro(organizacaoId, principal, buscarAutenticado(principal));
        return membroRepository.findByIdOrganizacaoId(organizacaoId).stream()
                .map(MembroResponseDTO::from)
                .sorted(Comparator.comparing(MembroResponseDTO::nome))
                .toList();
    }

    @Transactional
    public MembroResponseDTO adicionarMembro(Long organizacaoId, UsuarioLogado principal,
            MembroRequestDTO request) {
        org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        Usuario convidado = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("Usuário com e-mail %s não encontrado. "
                        + "Ele precisa se registrar antes de ser adicionado."
                        .formatted(request.email().trim())));
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(organizacaoId, convidado.getId())) {
            throw new RegistroDuplicadoException("O usuário %s já é membro desta organização."
                    .formatted(convidado.getEmail()));
        }
        MembroOrganizacao membro = membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(
                        organizacaoRepository.getReferenceById(organizacaoId), convidado))
                .papel(request.papel())
                .desde(LocalDateTime.now())
                .build());
        return MembroResponseDTO.from(membro);
    }

    @Transactional
    public MembroResponseDTO alterarPapel(Long organizacaoId, UsuarioLogado principal,
            Long usuarioId, MembroPapelRequestDTO request) {
        Organizacao organizacao = org(organizacaoId);
        MembroOrganizacao solicitante = exigirAdmin(organizacaoId, principal,
                buscarAutenticado(principal));
        MembroOrganizacao membro = membro(organizacaoId, usuarioId);
        protegerCriadorESolicitante(organizacao, solicitante, usuarioId);
        membro.setPapel(request.papel());
        return MembroResponseDTO.from(membro);
    }

    @Transactional
    public void removerMembro(Long organizacaoId, UsuarioLogado principal, Long usuarioId) {
        Organizacao organizacao = org(organizacaoId);
        MembroOrganizacao solicitante = exigirAdmin(organizacaoId, principal,
                buscarAutenticado(principal));
        MembroOrganizacao membro = membro(organizacaoId, usuarioId);
        protegerCriadorESolicitante(organizacao, solicitante, usuarioId);
        membroRepository.delete(membro);
    }

    @Transactional
    public void sairDaOrganizacao(Long organizacaoId, UsuarioLogado principal) {
        org(organizacaoId);
        Usuario usuario = buscarAutenticado(principal);
        MembroOrganizacao membro = exigirMembro(organizacaoId, principal, usuario);
        if (membro == null) {
            throw new OperacaoNaoPermitidaException(
                    "Administradores globais não participam de grupos.");
        }
        if (membro.getPapel() == PapelOrganizacao.ADMIN) {
            boolean outroAdmin = membroRepository.findByIdOrganizacaoId(organizacaoId).stream()
                    .anyMatch(outro -> outro.getPapel() == PapelOrganizacao.ADMIN
                            && !outro.getId().getUsuario().getId().equals(usuario.getId()));
            if (!outroAdmin) {
                throw new OperacaoNaoPermitidaException(
                        "Você é o único administrador. Promova outro membro a administrador "
                                + "antes de sair do grupo.");
            }
        }
        membroRepository.delete(membro);
    }

    @Transactional(readOnly = true)
    public CodigoAcessoResponseDTO buscarCodigoAcesso(Long organizacaoId,
            UsuarioLogado principal) {
        Organizacao organizacao = org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        return new CodigoAcessoResponseDTO(organizacao.getCodigoAcesso());
    }

    @Transactional
    public CodigoAcessoResponseDTO gerarCodigoAcesso(Long organizacaoId,
            UsuarioLogado principal) {
        Organizacao organizacao = org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        organizacao.setCodigoAcesso(gerarCodigoUnico());
        return new CodigoAcessoResponseDTO(organizacao.getCodigoAcesso());
    }

    @Transactional
    public void revogarCodigoAcesso(Long organizacaoId, UsuarioLogado principal) {
        Organizacao organizacao = org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        organizacao.setCodigoAcesso(null);
    }

    @Transactional
    public void excluir(Long organizacaoId, UsuarioLogado principal) {
        Organizacao organizacao = org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        empenhoRepository.deleteByOrganizacaoId(organizacaoId);
        empenhoSequenciaRepository.deleteByOrganizacaoId(organizacaoId);
        movimentacaoRepository.deleteByOrganizacaoId(organizacaoId);
        creditoRepository.deleteByOrganizacaoId(organizacaoId);
        contratoRepository.deleteByOrganizacaoId(organizacaoId);
        licitacaoRepository.deleteByOrganizacaoId(organizacaoId);
        fornecedorRepository.deleteByOrganizacaoId(organizacaoId);
        dotacaoRepository.deleteByOrganizacaoId(organizacaoId);
        conviteRepository.deleteByOrganizacaoId(organizacaoId);
        membroRepository.deleteByOrganizacaoId(organizacaoId);
        organizacaoRepository.delete(organizacao);
    }

    private String gerarCodigoUnico() {
        String codigo;
        do {
            codigo = gerarCodigo();
        } while (organizacaoRepository.findByCodigoAcesso(codigo).isPresent());
        return codigo;
    }

    private String gerarCodigo() {
        StringBuilder codigo = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO[RANDOM.nextInt(ALFABETO_CODIGO.length)]);
        }
        return codigo.toString();
    }

    @Transactional
    public void redefinirSenhaMembro(Long organizacaoId, UsuarioLogado principal, Long usuarioId,
            MembroSenhaRequestDTO request) {
        Organizacao organizacao = org(organizacaoId);
        MembroOrganizacao solicitante = exigirAdmin(organizacaoId, principal,
                buscarAutenticado(principal));
        MembroOrganizacao membro = membro(organizacaoId, usuarioId);
        protegerResetDeSenha(organizacao, solicitante, usuarioId);
        Usuario alvo = membro.getId().getUsuario();
        if (passwordEncoder.matches(request.novaSenha(), alvo.getSenha())) {
            throw new IllegalArgumentException(
                    "A nova senha deve ser diferente da senha atual.");
        }
        alvo.setSenha(passwordEncoder.encode(request.novaSenha()));
        alvo.setVersaoToken(incrementarVersao(alvo.getVersaoToken()));
        usuarioRepository.save(alvo);
    }

    private void protegerResetDeSenha(Organizacao organizacao, MembroOrganizacao solicitante,
            Long usuarioAlvo) {
        if (organizacao.getCriadoPor().getId().equals(usuarioAlvo)) {
            throw new OperacaoNaoPermitidaException(
                    "A senha do criador do grupo não pode ser redefinida por outro administrador.");
        }
        if (solicitante != null && solicitante.getId().getUsuario().getId().equals(usuarioAlvo)) {
            throw new OperacaoNaoPermitidaException(
                    "Use \"Minha conta\" para trocar a sua própria senha.");
        }
    }

    private void protegerCriadorESolicitante(Organizacao organizacao,
            MembroOrganizacao solicitante, Long usuarioAlvo) {
        if (organizacao.getCriadoPor().getId().equals(usuarioAlvo)) {
            throw new OperacaoNaoPermitidaException(
                    "O criador do grupo não pode ser rebaixado ou removido.");
        }
        if (solicitante != null && solicitante.getId().getUsuario().getId().equals(usuarioAlvo)) {
            throw new OperacaoNaoPermitidaException(
                    "Você não pode alterar ou remover sua própria participação na organização.");
        }
    }

    private MembroOrganizacao membro(Long organizacaoId, Long usuarioId) {
        return membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(organizacaoId, usuarioId)
                .orElseThrow(() -> new NotFoundException("O usuário de id %d não é membro desta "
                        + "organização.".formatted(usuarioId)));
    }

    private void validarNomeDisponivel(String nome) {
        if (organizacaoRepository.existsByNomeIgnoreCase(nome.trim())) {
            throw new RegistroDuplicadoException(
                    "Já existe um grupo com o nome %s.".formatted(nome.trim()));
        }
    }

    private int incrementarVersao(Integer atual) {
        return atual == null ? 1 : atual + 1;
    }

    private Organizacao org(Long id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organização", id));
    }

    private Usuario buscarAutenticado(UsuarioLogado principal) {
        return usuarioRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuário", principal.getUsername()));
    }

    private MembroOrganizacao exigirMembro(Long organizacaoId, UsuarioLogado principal,
            Usuario usuario) {
        if (principal.ehSuperAdmin()) {
            return null;
        }
        return membroRepository.findByIdOrganizacaoIdAndIdUsuarioId(organizacaoId, usuario.getId())
                .orElseThrow(NaoMembroException::new);
    }

    private MembroOrganizacao exigirAdmin(Long organizacaoId, UsuarioLogado principal,
            Usuario usuario) {
        MembroOrganizacao membro = exigirMembro(organizacaoId, principal, usuario);
        if (membro == null) {
            return null;
        }
        if (membro.getPapel() != PapelOrganizacao.ADMIN) {
            throw new OperacaoNaoPermitidaException(
                    "Apenas o administrador da organização pode executar esta operação.");
        }
        return membro;
    }

    private String papelResposta(MembroOrganizacao membro, UsuarioLogado principal) {
        if (principal.ehSuperAdmin()) {
            return "SUPER_ADMIN";
        }
        return membro.getPapel().name();
    }
}