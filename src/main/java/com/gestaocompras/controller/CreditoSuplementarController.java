package com.gestaocompras.controller;

import com.gestaocompras.dto.CreditoSuplementarRequestDTO;
import com.gestaocompras.dto.CreditoSuplementarResponseDTO;
import com.gestaocompras.service.CreditoSuplementarService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creditos-suplementares")
public class CreditoSuplementarController {

    private final CreditoSuplementarService creditoSuplementarService;

    public CreditoSuplementarController(CreditoSuplementarService creditoSuplementarService) {
        this.creditoSuplementarService = creditoSuplementarService;
    }

    @PostMapping
    public ResponseEntity<CreditoSuplementarResponseDTO> realizar(
            @Valid @RequestBody CreditoSuplementarRequestDTO request) {
        CreditoSuplementarResponseDTO resposta = creditoSuplementarService.realizar(request);
        return ResponseEntity.created(URI.create("/api/creditos-suplementares/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<CreditoSuplementarResponseDTO>> listar(
            @RequestParam(required = false) Long dotacaoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "data") Pageable pageable) {
        return ResponseEntity
                .ok(creditoSuplementarService.listar(dotacaoId, dataInicio, dataFim, pageable));
    }
}
