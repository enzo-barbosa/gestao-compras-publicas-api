package com.gestaocompras.service;

import com.gestaocompras.dto.AtualizarContaRequestDTO;
import com.gestaocompras.dto.AlterarSenhaRequestDTO;
import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.dto.RegistroRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import com.gestaocompras.dto.UsuarioResponseDTO;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Genero;
import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.PapelOrganizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.JwtService;
import java.time.LocalDateTime;
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
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
            MembroOrganizacaoRepository membroRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.membroRepository = membroRepository;
        this.organizacaoRepository = organizacaoRepository;
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
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .perfil(Perfil.USUARIO)
                .genero(request.genero() == null ? Genero.NAO_INFORMADO : request.genero())
                .versaoToken(0)
                .build());
        Organizacao espacoPessoal = organizacaoRepository.save(Organizacao.builder()
                .nome(nomePessoalDisponivel(usuario.getNome()))
                .criadoEm(LocalDateTime.now())
                .criadoPor(usuario)
                .build());
        membroRepository.save(MembroOrganizacao.builder()
                .id(new MembroOrganizacao.Id(espacoPessoal, usuario))
                .papel(PapelOrganizacao.ADMIN)
                .desde(LocalDateTime.now())
                .build());
        return UsuarioResponseDTO.from(usuario, organizacoesDoUsuario(usuario.getId()));
    }

    private String nomePessoalDisponivel(String nome) {
        String base = "Espaço de " + nome.trim();
        int sufixo = 1;
        String candidato;
        do {
            candidato = sufixo == 1 ? base : base + " (" + sufixo + ")";
            sufixo++;
        } while (nomeJaUsado(candidato));
        return candidato;
    }

    private boolean nomeJaUsado(String nome) {
        return organizacaoRepository.findAll().stream()
                .anyMatch(existente -> existente.getNome().equalsIgnoreCase(nome));
    }

    @Transactional
    public UsuarioResponseDTO atualizarConta(String email, AtualizarContaRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        if (request.nome() == null && request.genero() == null) {
            throw new IllegalArgumentException("Informe ao menos um campo para atualizar.");
        }
        if (request.nome() != null) {
            if (request.nome().isBlank()) {
                throw new IllegalArgumentException("O nome não pode ser vazio.");
            }
            usuario.setNome(request.nome().trim());
        }
        if (request.genero() != null) {
            usuario.setGenero(request.genero());
        }
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
