package com.gestaocompras.controller;

import com.gestaocompras.dto.DotacaoRequestDTO;
import com.gestaocompras.dto.DotacaoResponseDTO;
import com.gestaocompras.dto.MovimentacaoResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.DotacaoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping("/api/dotacoes")
public class DotacaoController {

    private final DotacaoService dotacaoService;

    public DotacaoController(DotacaoService dotacaoService) {
        this.dotacaoService = dotacaoService;
    }

    @PostMapping
    public ResponseEntity<DotacaoResponseDTO> criar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody DotacaoRequestDTO request) {
        DotacaoResponseDTO resposta = dotacaoService.criar(usuarioLogado.organizacaoId(), request);
        return ResponseEntity.created(URI.create("/api/dotacoes/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<DotacaoResponseDTO>> listar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @RequestParam(required = false) Integer anoExercicio,
            @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return ResponseEntity.ok(
                dotacaoService.listar(usuarioLogado.organizacaoId(), anoExercicio, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DotacaoResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                dotacaoService.buscarPorId(usuarioLogado.organizacaoId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DotacaoResponseDTO> atualizar(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id, @Valid @RequestBody DotacaoRequestDTO request) {
        return ResponseEntity.ok(
                dotacaoService.atualizar(usuarioLogado.organizacaoId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id) {
        dotacaoService.remover(usuarioLogado.organizacaoId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/saldo")
    public ResponseEntity<BigDecimal> consultarSaldo(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                dotacaoService.consultarSaldo(usuarioLogado.organizacaoId(), id));
    }

    @GetMapping("/{id}/movimentacoes")
    public ResponseEntity<Page<MovimentacaoResponseDTO>> listarMovimentacoes(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @PageableDefault(size = 20, sort = "dataHora") Pageable pageable) {
        return ResponseEntity.ok(dotacaoService.listarMovimentacoes(
                usuarioLogado.organizacaoId(), id, pageable));
    }
}
