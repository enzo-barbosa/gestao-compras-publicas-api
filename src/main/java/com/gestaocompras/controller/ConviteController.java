package com.gestaocompras.controller;

import com.gestaocompras.dto.AceitarCodigoRequestDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.ConviteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/aceitar-email")
    public ResponseEntity<List<OrganizacaoResponseDTO>> aceitarPorEmail(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado) {
        return ResponseEntity.ok(conviteService.aceitarPorEmail(usuarioLogado));
    }
}