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
import java.util.ArrayList;
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
        Organizacao organizacao = org(organizacaoId);
        Usuario solicitante = buscarAutenticado(principal);
        exigirAdmin(organizacaoId, principal, solicitante);
        boolean temEmail = request.email() != null && !request.email().isBlank();
        boolean temCodigo = request.codigo() != null && !request.codigo().isBlank();
        if (temEmail == temCodigo) {
            throw new IllegalArgumentException("Informe exatamente um dos campos: "
                    + "e-mail do convidado ou código do convite.");
        }
        ConviteOrganizacao convite = ConviteOrganizacao.builder()
                .organizacao(organizacao)
                .papel(request.papel())
                .criadoPor(solicitante)
                .criadoEm(LocalDateTime.now())
                .build();
        if (temEmail) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                        organizacaoId, usuario.getId())) {
                    throw new RegistroDuplicadoException(
                            "O usuário %s já é membro desta organização.".formatted(email));
                }
            });
            if (conviteRepository
                    .findByEmailAndUsadoEmIsNullAndOrganizacaoId(email, organizacaoId)
                    .isPresent()) {
                throw new RegistroDuplicadoException(
                        "Já existe um convite pendente para %s nesta organização."
                                .formatted(email));
            }
            convite.setEmail(email);
        } else {
            String codigo = request.codigo().trim();
            if (conviteRepository.findByCodigoAndUsadoEmIsNull(codigo).isPresent()) {
                throw new RegistroDuplicadoException(
                        "Já existe um convite pendente com o código %s.".formatted(codigo));
            }
            convite.setCodigo(codigo);
        }
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

    @Transactional
    public OrganizacaoResponseDTO aceitarPorCodigo(UsuarioLogado principal,
            AceitarCodigoRequestDTO request) {
        Usuario usuario = buscarAutenticado(principal);
        ConviteOrganizacao convite = conviteRepository
                .findByCodigoAndUsadoEmIsNull(request.codigo().trim())
                .orElseThrow(() -> new NotFoundException("Convite inválido ou já utilizado."));
        return aceitar(convite, usuario);
    }

    @Transactional
    public List<OrganizacaoResponseDTO> aceitarPorEmail(UsuarioLogado principal) {
        Usuario usuario = buscarAutenticado(principal);
        List<OrganizacaoResponseDTO> aceitas = new ArrayList<>();
        for (ConviteOrganizacao convite : conviteRepository
                .findAllByEmailAndUsadoEmIsNull(usuario.getEmail())) {
            Organizacao organizacao = convite.getOrganizacao();
            if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                    organizacao.getId(), usuario.getId())) {
                convite.setUsadoEm(LocalDateTime.now());
                continue;
            }
            aceitas.add(membro(convite, usuario));
            convite.setUsadoEm(LocalDateTime.now());
        }
        return aceitas;
    }

    private OrganizacaoResponseDTO aceitar(ConviteOrganizacao convite, Usuario usuario) {
        if (membroRepository.existsByIdOrganizacaoIdAndIdUsuarioId(
                convite.getOrganizacao().getId(), usuario.getId())) {
            throw new RegistroDuplicadoException("Você já é membro desta organização.");
        }
        OrganizacaoResponseDTO resposta = membro(convite, usuario);
        convite.setUsadoEm(LocalDateTime.now());
        return resposta;
    }

    private OrganizacaoResponseDTO membro(ConviteOrganizacao convite, Usuario usuario) {
        Organizacao organizacao = convite.getOrganizacao();
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(organizacao, usuario))
                .papel(convite.getPapel())
                .desde(LocalDateTime.now())
                .build());
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