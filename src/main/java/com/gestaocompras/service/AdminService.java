package com.gestaocompras.service;

import com.gestaocompras.dto.AdminOrganizacaoResponseDTO;
import com.gestaocompras.dto.AdminPerfilRequestDTO;
import com.gestaocompras.dto.AdminUsuarioResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final MembroOrganizacaoRepository membroRepository;

    public AdminService(UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            MembroOrganizacaoRepository membroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.membroRepository = membroRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll(Sort.by("nome")).stream()
                .map(AdminUsuarioResponseDTO::from)
                .toList();
    }

    @Transactional
    public AdminUsuarioResponseDTO alterarPerfil(Long usuarioId, AdminPerfilRequestDTO request,
            String emailLogado) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        Perfil perfilAlvo = request.perfil();
        if (usuario.getPerfil() == perfilAlvo) {
            return AdminUsuarioResponseDTO.from(usuario);
        }
        Usuario usuarioLogado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (usuario.getId().equals(usuarioLogado.getId()) && perfilAlvo != Perfil.SUPER_ADMIN) {
            throw new OperacaoNaoPermitidaException(
                    "Você não pode rebaixar o próprio perfil.");
        }
        if (usuario.getPerfil() == Perfil.SUPER_ADMIN
                && usuarioRepository.countByPerfil(Perfil.SUPER_ADMIN) <= 1) {
            throw new OperacaoNaoPermitidaException(
                    "Resta apenas um super administrador no sistema.");
        }
        usuario.setPerfil(perfilAlvo);
        return AdminUsuarioResponseDTO.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<AdminOrganizacaoResponseDTO> listarOrganizacoes() {
        return organizacaoRepository.findAll(Sort.by("nome")).stream()
                .map(organizacao -> AdminOrganizacaoResponseDTO.from(organizacao,
                        membroRepository.findByIdOrganizacaoId(organizacao.getId()).size()))
                .toList();
    }
}