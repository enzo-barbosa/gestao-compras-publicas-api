package com.gestaocompras.controller;

import com.gestaocompras.dto.AlterarSenhaRequestDTO;
import com.gestaocompras.dto.AtualizarContaRequestDTO;
import com.gestaocompras.dto.EsqueciSenhaRequestDTO;
import com.gestaocompras.dto.LoginRequestDTO;
import com.gestaocompras.dto.RedefinirSenhaRequestDTO;
import com.gestaocompras.dto.RegistroRequestDTO;
import com.gestaocompras.dto.TokenResponseDTO;
import com.gestaocompras.dto.UsuarioResponseDTO;
import com.gestaocompras.service.AuthService;
import com.gestaocompras.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService,
            RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public UsuarioResponseDTO me(@AuthenticationPrincipal UserDetails principal) {
        return authService.buscarUsuarioAtual(principal.getUsername());
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> registrar(
            @Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PutMapping("/minha-conta")
    public UsuarioResponseDTO atualizarConta(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AtualizarContaRequestDTO request) {
        return authService.atualizarConta(principal.getUsername(), request);
    }

    @PutMapping("/alterar-senha")
    public TokenResponseDTO alterarSenha(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AlterarSenhaRequestDTO request) {
        return authService.alterarSenha(principal.getUsername(), request);
    }

    @PostMapping("/logout-todos")
    public ResponseEntity<Void> sairEmTodosDispositivos(
            @AuthenticationPrincipal UserDetails principal) {
        authService.sairEmTodosDispositivos(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(
            @Valid @RequestBody EsqueciSenhaRequestDTO request) {
        recuperacaoSenhaService.solicitar(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequestDTO request) {
        recuperacaoSenhaService.redefinir(request);
        return ResponseEntity.ok().build();
    }
}
