package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.RedefinirSenhaRequestDTO;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.RecuperacaoSenhaToken;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.RecuperacaoSenhaTokenRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    private static final String EMAIL = "usuario@gestao.com";
    private static final String CODIGO = "123456";
    private static final Instant INSTANTE = Instant.parse("2026-09-13T10:00:00Z");
    private static final LocalDateTime AGORA = LocalDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RecuperacaoSenhaTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecuperacaoSenhaService servico;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(INSTANTE);
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        usuario = Usuario.builder().id(1L).nome("Usuário").email(EMAIL)
                .perfil(Perfil.USUARIO).versaoToken(0).build();
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private RecuperacaoSenhaToken tokenExistente(int tentativas) {
        return RecuperacaoSenhaToken.builder()
                .id(1L).usuario(usuario).tokenHash(sha256(CODIGO))
                .expiraEm(AGORA.plusMinutes(15)).tentativas(tentativas)
                .criadoEm(AGORA).build();
    }

    @Test
    void solicitarDeveGerarCodigoEnviarEmailESalvarToken() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(RecuperacaoSenhaToken.class))).thenAnswer(inv -> inv.getArgument(0));

        servico.solicitar(EMAIL);

        ArgumentCaptor<RecuperacaoSenhaToken> captor =
                ArgumentCaptor.forClass(RecuperacaoSenhaToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64);
        assertThat(captor.getValue().getExpiraEm()).isEqualTo(AGORA.plusMinutes(15));
        verify(emailService).enviarCodigoRecuperacao(eq(EMAIL), anyString());
    }

    @Test
    void solicitarParaEmailInexistenteDeveIgnorar() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        servico.solicitar(EMAIL);

        verify(tokenRepository, never()).save(any(RecuperacaoSenhaToken.class));
        verify(emailService, never()).enviarCodigoRecuperacao(anyString(), anyString());
    }

    @Test
    void solicitarDeveDescartarTokenPendenteAnterior() {
        RecuperacaoSenhaToken pendente = tokenExistente(0);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L)).thenReturn(Optional.of(pendente));
        when(tokenRepository.save(any(RecuperacaoSenhaToken.class))).thenAnswer(inv -> inv.getArgument(0));

        servico.solicitar(EMAIL);

        verify(tokenRepository).delete(pendente);
    }

    @Test
    void redefinirComCodigoCorretoDeveRedefinirSenhaEBumpVersaoToken() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L))
                .thenReturn(Optional.of(tokenExistente(0)));
        when(passwordEncoder.matches("novaSenhaSegura123", usuario.getSenha())).thenReturn(false);
        when(passwordEncoder.encode("novaSenhaSegura123")).thenReturn("senhaCodificada");

        servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, CODIGO, "novaSenhaSegura123"));

        verify(usuarioRepository).save(usuario);
        assertThat(usuario.getSenha()).isEqualTo("senhaCodificada");
        assertThat(usuario.getVersaoToken()).isEqualTo(1);
        verify(passwordEncoder).encode("novaSenhaSegura123");
    }

    @Test
    void redefinirComCodigoErradoDeveIncrementarTentativas() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L))
                .thenReturn(Optional.of(tokenExistente(0)));

        assertThatThrownBy(() -> servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, "000000", "novaSenhaSegura123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Código inválido ou expirado");

        verify(tokenRepository).save(any(RecuperacaoSenhaToken.class));
    }

    @Test
    void redefinirComTokenExpiradoDeveLancarEMarcarUsado() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        var expirado = tokenExistente(0);
        expirado.setExpiraEm(AGORA.minusMinutes(1));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L)).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, CODIGO, "novaSenhaSegura123")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(expirado.getUsadoEm()).isNotNull();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void redefinirAposMaxTentativasDeveInvalidarToken() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        var esgotado = tokenExistente(5);
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L)).thenReturn(Optional.of(esgotado));

        assertThatThrownBy(() -> servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, CODIGO, "novaSenhaSegura123")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(esgotado.getUsadoEm()).isNotNull();
    }

    @Test
    void redefinirParaEmailSemTokenDeveLancar() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, CODIGO, "novaSenhaSegura123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redefinirComSenhaIgualAtualDeveLancar() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEmIsNull(1L))
                .thenReturn(Optional.of(tokenExistente(0)));
        when(passwordEncoder.matches("novaSenhaSegura123", usuario.getSenha())).thenReturn(true);

        assertThatThrownBy(() -> servico.redefinir(new RedefinirSenhaRequestDTO(
                EMAIL, CODIGO, "novaSenhaSegura123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente da senha atual");
    }
}