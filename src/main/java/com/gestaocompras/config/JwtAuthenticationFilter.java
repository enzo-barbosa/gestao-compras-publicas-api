package com.gestaocompras.config;

import com.gestaocompras.dto.ErroResposta;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import com.gestaocompras.security.JwtService;
import com.gestaocompras.security.UsuarioLogado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CABECALHO_ORGANIZACAO = "X-Org-Id";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final MembroOrganizacaoRepository membroRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
            UsuarioRepository usuarioRepository,
            MembroOrganizacaoRepository membroRepository,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.membroRepository = membroRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = cabecalho.substring(7);
        if (SecurityContextHolder.getContext().getAuthentication() != null
                || !jwtService.tokenValido(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        String email = jwtService.extrairEmail(token);
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            UsuarioLogado principal;
            if (usuario.getPerfil() == Perfil.SUPER_ADMIN) {
                principal = UsuarioLogado.superAdmin(email,
                        extrairOrganizacaoId(request, response));
            } else {
                Long organizacaoId = extrairOrganizacaoId(request, response);
                if (organizacaoId == null) {
                    principal = UsuarioLogado.semOrganizacao(email);
                } else {
                    var membro = membroRepository
                            .findByIdOrganizacaoIdAndIdUsuarioId(organizacaoId, usuario.getId());
                    if (membro.isEmpty()) {
                        escreverNaoMembro(response);
                        return;
                    }
                    principal = UsuarioLogado.membro(email, organizacaoId,
                            membro.get().getPapel().name());
                }
            }
            UsernamePasswordAuthenticationToken autenticacao =
                    new UsernamePasswordAuthenticationToken(principal, null,
                            principal.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        });
        if (!response.isCommitted()) {
            filterChain.doFilter(request, response);
        }
    }

    private Long extrairOrganizacaoId(HttpServletRequest request,
            HttpServletResponse response) {
        String valor = request.getHeader(CABECALHO_ORGANIZACAO);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException excecao) {
            escreverOrganizacaoInvalida(response);
            return null;
        }
    }

    private void escreverNaoMembro(HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ErroResposta.of(HttpStatus.FORBIDDEN.value(),
                            HttpStatus.FORBIDDEN.getReasonPhrase(),
                            "Você não é membro desta organização."));
        } catch (IOException ignorada) {
        }
    }

    private void escreverOrganizacaoInvalida(HttpServletResponse response) {
        try {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ErroResposta.of(HttpStatus.BAD_REQUEST.value(),
                            HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "O cabeçalho X-Org-Id deve ser um número válido."));
        } catch (IOException ignorada) {
        }
    }
}