package com.gestaocompras.controller;

import com.gestaocompras.dto.ContratoRequestDTO;
import com.gestaocompras.dto.ContratoResponseDTO;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.service.ContratoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ContratoResponseDTO> criar(@Valid @RequestBody ContratoRequestDTO request) {
        ContratoResponseDTO resposta = contratoService.criar(request);
        return ResponseEntity.created(URI.create("/api/contratos/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<ContratoResponseDTO>> listar(
            @RequestParam(required = false) Long dotacaoId,
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) StatusContrato status,
            @PageableDefault(size = 20, sort = "dataInicio") Pageable pageable) {
        return ResponseEntity.ok(contratoService.listar(dotacaoId, fornecedorId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody ContratoRequestDTO request) {
        return ResponseEntity.ok(contratoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        contratoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
