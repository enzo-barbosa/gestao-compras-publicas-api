package com.gestaocompras.controller;

import com.gestaocompras.dto.AceitarCodigoRequestDTO;
import com.gestaocompras.dto.ConviteResponseDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.ConviteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convites")
public class ConviteController {

    private final ConviteService conviteService;

    public ConviteController(ConviteService conviteService) {
        this.conviteService = conviteService;
    }

    @PostMapping("/aceitar")
    public ResponseEntity<OrganizacaoResponseDTO> aceitarPorCodigo(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody AceitarCodigoRequestDTO request) {
        return ResponseEntity.ok(conviteService.aceitarPorCodigo(usuarioLogado, request));
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<ConviteResponseDTO>> meusPendentes(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado) {
        return ResponseEntity.ok(conviteService.meusPendentes(usuarioLogado));
    }

    @PostMapping("/{id}/aceitar")
    public ResponseEntity<OrganizacaoResponseDTO> aceitarNominal(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(conviteService.aceitarNominal(usuarioLogado, id));
    }

    @PostMapping("/{id}/recusar")
    public ResponseEntity<Void> recusarNominal(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        conviteService.recusarNominal(usuarioLogado, id);
        return ResponseEntity.noContent().build();
    }
}