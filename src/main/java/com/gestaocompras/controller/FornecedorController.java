package com.gestaocompras.controller;

import com.gestaocompras.dto.FornecedorRequestDTO;
import com.gestaocompras.dto.FornecedorResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.FornecedorService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody FornecedorRequestDTO request) {
        FornecedorResponseDTO resposta = fornecedorService.criar(
                usuarioLogado.organizacaoId(), request);
        return ResponseEntity.created(URI.create("/api/fornecedores/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<FornecedorResponseDTO>> listar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(
                fornecedorService.listar(usuarioLogado.organizacaoId(), nome, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                fornecedorService.buscarPorId(usuarioLogado.organizacaoId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody FornecedorRequestDTO request) {
        return ResponseEntity.ok(
                fornecedorService.atualizar(usuarioLogado.organizacaoId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id) {
        fornecedorService.remover(usuarioLogado.organizacaoId(), id);
        return ResponseEntity.noContent().build();
    }
}