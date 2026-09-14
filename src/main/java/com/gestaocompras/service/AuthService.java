package com.gestaocompras.service;

import com.gestaocompras.dto.AlterarNomeRequestDTO;
import com.gestaocompras.dto.AlterarSenhaRequestDTO;
import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.dto.RegistroRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import com.gestaocompras.dto.UsuarioResponseDTO;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.JwtService;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final MembroOrganizacaoRepository membroRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
            MembroOrganizacaoRepository membroRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.membroRepository = membroRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas."));
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        return TokenResponseDTO.of(
                jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name(),
                        usuario.getVersaoToken()), usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarUsuarioAtual(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        return UsuarioResponseDTO.from(usuario, organizacoesDoUsuario(usuario.getId()));
    }

    @Transactional
    public UsuarioResponseDTO registrar(RegistroRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RegistroDuplicadoException(
                    "Já existe um usuário com o e-mail %s.".formatted(request.email()));
        }
        return UsuarioResponseDTO.from(usuarioRepository.save(Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .perfil(Perfil.USUARIO)
                .versaoToken(0)
                .build()), List.of());
    }

    @Transactional
    public UsuarioResponseDTO atualizarNome(String email, AlterarNomeRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        usuario.setNome(request.nome());
        return UsuarioResponseDTO.from(usuarioRepository.save(usuario),
                organizacoesDoUsuario(usuario.getId()));
    }

    @Transactional
    public TokenResponseDTO alterarSenha(String email, AlterarSenhaRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException(
                    "A nova senha deve ser diferente da senha atual.");
        }
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setVersaoToken(incrementarVersao(usuario.getVersaoToken()));
        usuarioRepository.save(usuario);
        return TokenResponseDTO.of(
                jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name(),
                        usuario.getVersaoToken()), usuario);
    }

    @Transactional
    public void sairEmTodosDispositivos(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        usuario.setVersaoToken(incrementarVersao(usuario.getVersaoToken()));
        usuarioRepository.save(usuario);
    }

    private int incrementarVersao(Integer atual) {
        return atual == null ? 1 : atual + 1;
    }

    private List<OrganizacaoResponseDTO> organizacoesDoUsuario(Long usuarioId) {
        return membroRepository.findByIdUsuarioId(usuarioId).stream()
                .map(membro -> OrganizacaoResponseDTO.from(
                        membro.getId().getOrganizacao(), membro.getPapel().name()))
                .sorted(Comparator.comparing(OrganizacaoResponseDTO::nome))
                .toList();
    }
}
