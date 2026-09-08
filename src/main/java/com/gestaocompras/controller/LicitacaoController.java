package com.gestaocompras.controller;

import com.gestaocompras.dto.LicitacaoRequestDTO;
import com.gestaocompras.dto.LicitacaoResponseDTO;
import com.gestaocompras.dto.VencedorRequestDTO;
import com.gestaocompras.model.ModalidadeLicitacao;
import com.gestaocompras.model.StatusLicitacao;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.LicitacaoService;
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
@RequestMapping("/api/licitacoes")
public class LicitacaoController {

    private final LicitacaoService licitacaoService;

    public LicitacaoController(LicitacaoService licitacaoService) {
        this.licitacaoService = licitacaoService;
    }

    @PostMapping
    public ResponseEntity<LicitacaoResponseDTO> criar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody LicitacaoRequestDTO request) {
        LicitacaoResponseDTO resposta = licitacaoService.criar(
                usuarioLogado.organizacaoId(), request);
        return ResponseEntity.created(URI.create("/api/licitacoes/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<LicitacaoResponseDTO>> listar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @RequestParam(required = false) StatusLicitacao status,
            @RequestParam(required = false) ModalidadeLicitacao modalidade,
            @PageableDefault(size = 20, sort = "dataAbertura") Pageable pageable) {
        return ResponseEntity.ok(
                licitacaoService.listar(usuarioLogado.organizacaoId(), status, modalidade, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicitacaoResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                licitacaoService.buscarPorId(usuarioLogado.organizacaoId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LicitacaoResponseDTO> atualizar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody LicitacaoRequestDTO request) {
        return ResponseEntity.ok(
                licitacaoService.atualizar(usuarioLogado.organizacaoId(), id, request));
    }

    @PutMapping("/{id}/vencedor")
    public ResponseEntity<LicitacaoResponseDTO> definirVencedor(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody VencedorRequestDTO request) {
        return ResponseEntity.ok(licitacaoService.definirVencedor(
                usuarioLogado.organizacaoId(), id, request.fornecedorId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id) {
        licitacaoService.remover(usuarioLogado.organizacaoId(), id);
        return ResponseEntity.noContent().build();
    }
}
