package com.gestaocompras.service;

import com.gestaocompras.dto.AceitarCodigoRequestDTO;
import com.gestaocompras.dto.ConviteRequestDTO;
import com.gestaocompras.dto.ConviteResponseDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.exception.NaoMembroException;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.ConviteOrganizacao;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ConviteOrganizacaoRepository;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.UsuarioLogado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {

    private final ConviteOrganizacaoRepository conviteRepository;
    private final MembroOrganizacaoRepository membroRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public ConviteService(ConviteOrganizacaoRepository conviteRepository,
            MembroOrganizacaoRepository membroRepository,
            OrganizacaoRepository organizacaoRepository,
            UsuarioRepository usuarioRepository) {
        this.conviteRepository = conviteRepository;
        this.membroRepository = membroRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ConviteResponseDTO criar(Long organizacaoId, UsuarioLogado principal,
            ConviteRequestDTO request) {
        org(organizacaoId);
        Usuario solicitante = buscarAutenticado(principal);
        exigirAdmin(organizacaoId, principal, solicitante);
        if (request.codigo() != null && !request.codigo().isBlank()) {
            throw new IllegalArgumentException("Convites são nominais, por e-mail. O acesso "
                    + "por código é o código de acesso da organização (ver \"Código de acesso\").");
        }
        String email = request.email() == null ? null
                : request.email().trim().toLowerCase(Locale.ROOT);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Informe o e-mail do convidado.");
        }
        Usuario convidado = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Não existe conta com o e-mail %s. "
                        + "O convidado precisa se registrar antes de ser convidado."
                        .formatted(email)));
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                organizacaoId, convidado.getId())) {
            throw new RegistroDuplicadoException(
                    "O usuário %s já é membro desta organização.".formatted(email));
        }
        if (conviteRepository.findByEmailAndUsadoEmIsNullAndRecusadoEmIsNullAndOrganizacaoId(
                email, organizacaoId).isPresent()) {
            throw new RegistroDuplicadoException(
                    "Já existe um convite pendente para %s nesta organização.".formatted(email));
        }
        ConviteOrganizacao convite = ConviteOrganizacao.builder()
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .email(email)
                .papel(request.papel())
                .criadoPor(solicitante)
                .criadoEm(LocalDateTime.now())
                .build();
        return ConviteResponseDTO.from(conviteRepository.save(convite));
    }

    @Transactional(readOnly = true)
    public List<ConviteResponseDTO> listarPendentes(Long organizacaoId, UsuarioLogado principal) {
        org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        return conviteRepository.findByOrganizacaoIdAndUsadoEmIsNull(organizacaoId).stream()
                .map(ConviteResponseDTO::from)
                .toList();
    }

    @Transactional
    public void revogar(Long organizacaoId, UsuarioLogado principal, Long conviteId) {
        org(organizacaoId);
        exigirAdmin(organizacaoId, principal, buscarAutenticado(principal));
        ConviteOrganizacao convite = conviteRepository.findById(conviteId)
                .orElseThrow(() -> new NotFoundException("Convite", conviteId));
        if (!convite.getOrganizacao().getId().equals(organizacaoId)) {
            throw new NotFoundException("Convite", conviteId);
        }
        if (convite.getUsadoEm() != null) {
            throw new OperacaoNaoPermitidaException(
                    "Um convite já utilizado não pode ser revogado.");
        }
        conviteRepository.delete(convite);
    }

    @Transactional(readOnly = true)
    public List<ConviteResponseDTO> meusPendentes(UsuarioLogado principal) {
        Usuario usuario = buscarAutenticado(principal);
        return conviteRepository.findByEmailAndUsadoEmIsNullAndRecusadoEmIsNull(
                        usuario.getEmail()).stream()
                .map(ConviteResponseDTO::from)
                .toList();
    }

    @Transactional
    public OrganizacaoResponseDTO aceitarNominal(UsuarioLogado principal, Long conviteId) {
        Usuario usuario = buscarAutenticado(principal);
        ConviteOrganizacao convite = conviteRepository
                .findByIdAndEmailAndUsadoEmIsNullAndRecusadoEmIsNull(conviteId, usuario.getEmail())
                .orElseThrow(() -> new NotFoundException("Convite inválido, aceito ou recusado."));
        return aceitar(convite, usuario);
    }

    @Transactional
    public void recusarNominal(UsuarioLogado principal, Long conviteId) {
        Usuario usuario = buscarAutenticado(principal);
        ConviteOrganizacao convite = conviteRepository
                .findByIdAndEmailAndUsadoEmIsNull(conviteId, usuario.getEmail())
                .orElseThrow(() -> new NotFoundException("Convite não encontrado para o seu e-mail."));
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                convite.getOrganizacao().getId(), usuario.getId())) {
            throw new OperacaoNaoPermitidaException(
                    "Você já é membro desta organização; o convite não pode ser recusado.");
        }
        if (convite.getRecusadoEm() != null) {
            throw new OperacaoNaoPermitidaException("Este convite já foi recusado.");
        }
        convite.setRecusadoEm(LocalDateTime.now());
    }

    @Transactional
    public OrganizacaoResponseDTO aceitarPorCodigo(UsuarioLogado principal,
            AceitarCodigoRequestDTO request) {
        Usuario usuario = buscarAutenticado(principal);
        String codigo = normalizarCodigo(request.codigo());
        Organizacao organizacao = organizacaoRepository.findByCodigoAcesso(codigo)
                .orElseThrow(() -> new NotFoundException("Código de acesso inválido."));
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                organizacao.getId(), usuario.getId())) {
            throw new RegistroDuplicadoException(
                    "Você já é membro desta organização.");
        }
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, usuario))
                .papel(PapelOrganizacao.VISITANTE)
                .desde(LocalDateTime.now())
                .build());
        return OrganizacaoResponseDTO.from(organizacao, PapelOrganizacao.VISITANTE.name());
    }

    static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private OrganizacaoResponseDTO aceitar(ConviteOrganizacao convite, Usuario usuario) {
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                convite.getOrganizacao().getId(), usuario.getId())) {
            throw new RegistroDuplicadoException("Você já é membro desta organização.");
        }
        Organizacao organizacao = convite.getOrganizacao();
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, usuario))
                .papel(convite.getPapel())
                .desde(LocalDateTime.now())
                .build());
        convite.setUsadoEm(LocalDateTime.now());
        return OrganizacaoResponseDTO.from(organizacao, convite.getPapel().name());
    }

    private Organizacao org(Long id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organização", id));
    }

    private Usuario buscarAutenticado(UsuarioLogado principal) {
        return usuarioRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuário", principal.getUsername()));
    }

    private MembroOrganizacao exigirAdmin(Long organizacaoId, UsuarioLogado principal,
            Usuario usuario) {
        if (principal.ehSuperAdmin()) {
            return null;
        }
        MembroOrganizacao membro = membroRepository
                .findByIdOrganizacaoIdAndIdUsuarioId(organizacaoId, usuario.getId())
                .orElseThrow(NaoMembroException::new);
        if (membro.getPapel() != PapelOrganizacao.ADMIN) {
            throw new OperacaoNaoPermitidaException(
                    "Apenas o administrador da organização pode executar esta operação.");
        }
        return membro;
    }
}