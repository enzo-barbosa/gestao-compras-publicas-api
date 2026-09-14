package com.gestaocompras.service;

import com.gestaocompras.dto.RedefinirSenhaRequestDTO;
import com.gestaocompras.model.RecuperacaoSenhaToken;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.RecuperacaoSenhaTokenRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecuperacaoSenhaService {

    private static final int VALIDADE_MINUTOS = 15;
    private static final int MAX_TENTATIVAS = 5;

    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
            RecuperacaoSenhaTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Transactional
    public void solicitar(String email) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailNormalizado);
        if (usuarioOpt.isEmpty()) {
            return;
        }
        Usuario usuario = usuarioOpt.get();
        tokenRepository.findByUsuarioIdAndUsadoEmIsNull(usuario.getId())
                .ifPresent(tokenRepository::delete);
        String codigo = gerarCodigo();
        tokenRepository.save(RecuperacaoSenhaToken.builder()
                .usuario(usuario)
                .tokenHash(hex(sha256(codigo)))
                .expiraEm(agora().plusMinutes(VALIDADE_MINUTOS))
                .criadoEm(agora())
                .build());
        emailService.enviarCodigoRecuperacao(usuario.getEmail(), codigo);
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequestDTO request) {
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Código inválido ou expirado."));
        RecuperacaoSenhaToken token = tokenRepository
                .findByUsuarioIdAndUsadoEmIsNull(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Código inválido ou expirado."));
        if (token.getExpiraEm().isBefore(agora())) {
            token.setUsadoEm(agora());
            tokenRepository.save(token);
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }
        if (token.getTentativas() >= MAX_TENTATIVAS) {
            token.setUsadoEm(agora());
            tokenRepository.save(token);
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }
        if (!MessageDigest.isEqual(sha256(request.codigo().trim()), hexBytes(token.getTokenHash()))) {
            token.setTentativas(token.getTentativas() + 1);
            if (token.getTentativas() >= MAX_TENTATIVAS) {
                token.setUsadoEm(agora());
            }
            tokenRepository.save(token);
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException(
                    "A nova senha deve ser diferente da senha atual.");
        }
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setVersaoToken(usuario.getVersaoToken() == null
                ? 1 : usuario.getVersaoToken() + 1);
        token.setUsadoEm(agora());
        usuarioRepository.save(usuario);
        tokenRepository.save(token);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private static String gerarCodigo() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private static byte[] hexBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}