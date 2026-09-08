package com.gestaocompras.controller;

import com.gestaocompras.dto.EmpenhoRequestDTO;
import com.gestaocompras.dto.EmpenhoResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.EmpenhoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empenhos")
public class EmpenhoController {

    private final EmpenhoService empenhoService;

    public EmpenhoController(EmpenhoService empenhoService) {
        this.empenhoService = empenhoService;
    }

    @PostMapping
    public ResponseEntity<EmpenhoResponseDTO> gerar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody EmpenhoRequestDTO request) {
        EmpenhoResponseDTO resposta = empenhoService.gerar(usuarioLogado.organizacaoId(), request);
        return ResponseEntity.created(URI.create("/api/empenhos/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<EmpenhoResponseDTO>> listar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long dotacaoId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(name = "dataDe", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDe,
            @RequestParam(name = "dataAte", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAte,
            @PageableDefault(size = 20, sort = "dataEmissao", direction = Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(empenhoService.listar(usuarioLogado.organizacaoId(), contratoId,
                dotacaoId, mes, ano, dataDe, dataAte, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpenhoResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(
                empenhoService.buscarPorId(usuarioLogado.organizacaoId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmpenhoResponseDTO> anular(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(empenhoService.anular(usuarioLogado.organizacaoId(), id));
    }
}
