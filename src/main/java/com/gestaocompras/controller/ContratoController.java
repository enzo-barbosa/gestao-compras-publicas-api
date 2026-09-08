package com.gestaocompras.controller;

import com.gestaocompras.dto.ContratoRequestDTO;
import com.gestaocompras.dto.ContratoResponseDTO;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.ContratoService;
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
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping
    public ResponseEntity<ContratoResponseDTO> criar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody ContratoRequestDTO request) {
        ContratoResponseDTO resposta = contratoService.criar(usuarioLogado.organizacaoId(), request);
        return ResponseEntity.created(URI.create("/api/contratos/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<ContratoResponseDTO>> listar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @RequestParam(required = false) Long dotacaoId,
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) StatusContrato status,
            @PageableDefault(size = 20, sort = "dataInicio") Pageable pageable) {
        return ResponseEntity.ok(contratoService.listar(usuarioLogado.organizacaoId(), dotacaoId,
                fornecedorId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                contratoService.buscarPorId(usuarioLogado.organizacaoId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> atualizar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody ContratoRequestDTO request) {
        return ResponseEntity.ok(
                contratoService.atualizar(usuarioLogado.organizacaoId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id) {
        contratoService.remover(usuarioLogado.organizacaoId(), id);
        return ResponseEntity.noContent().build();
    }
}
