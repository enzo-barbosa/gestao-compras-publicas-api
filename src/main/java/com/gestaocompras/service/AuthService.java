package com.gestaocompras.service;

import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.RegistroRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import com.gestaocompras.dto.UsuarioResponseDTO;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
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
                jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name()), usuario);
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
                .build()));
    }
}
