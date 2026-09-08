package com.gestaocompras.controller;

import com.gestaocompras.dto.ConviteRequestDTO;
import com.gestaocompras.dto.ConviteResponseDTO;
import com.gestaocompras.dto.MembroPapelRequestDTO;
import com.gestaocompras.dto.MembroRequestDTO;
import com.gestaocompras.dto.MembroResponseDTO;
import com.gestaocompras.dto.OrganizacaoRequestDTO;
import com.gestaocompras.dto.OrganizacaoResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.ConviteService;
import com.gestaocompras.service.OrganizacaoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizacoes")
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;
    private final ConviteService conviteService;

    public OrganizacaoController(OrganizacaoService organizacaoService,
            ConviteService conviteService) {
        this.organizacaoService = organizacaoService;
        this.conviteService = conviteService;
    }

    @PostMapping
    public ResponseEntity<OrganizacaoResponseDTO> criar(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @Valid @RequestBody OrganizacaoRequestDTO request) {
        OrganizacaoResponseDTO resposta = organizacaoService.criar(usuarioLogado, request);
        return ResponseEntity.created(URI.create("/api/organizacoes/%d".formatted(resposta.id())))
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<OrganizacaoResponseDTO>> listarMinhas(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado) {
        return ResponseEntity.ok(organizacaoService.listarMinhas(usuarioLogado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizacaoResponseDTO> buscarPorId(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(organizacaoService.buscarPorId(id, usuarioLogado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizacaoResponseDTO> renomear(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody OrganizacaoRequestDTO request) {
        return ResponseEntity.ok(organizacaoService.renomear(id, usuarioLogado, request));
    }

    @GetMapping("/{id}/membros")
    public ResponseEntity<List<MembroResponseDTO>> listarMembros(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(organizacaoService.listarMembros(id, usuarioLogado));
    }

    @PostMapping("/{id}/membros")
    public ResponseEntity<MembroResponseDTO> adicionarMembro(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody MembroRequestDTO request) {
        return ResponseEntity.ok(organizacaoService.adicionarMembro(id, usuarioLogado, request));
    }

    @PutMapping("/{id}/membros/{usuarioId}")
    public ResponseEntity<MembroResponseDTO> alterarPapel(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @PathVariable Long usuarioId, @Valid @RequestBody MembroPapelRequestDTO request) {
        return ResponseEntity.ok(
                organizacaoService.alterarPapel(id, usuarioLogado, usuarioId, request));
    }

    @DeleteMapping("/{id}/membros/{usuarioId}")
    public ResponseEntity<Void> removerMembro(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id, @PathVariable Long usuarioId) {
        organizacaoService.removerMembro(id, usuarioLogado, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convites")
    public ResponseEntity<ConviteResponseDTO> criarConvite(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody ConviteRequestDTO request) {
        return ResponseEntity.ok(conviteService.criar(id, usuarioLogado, request));
    }

    @GetMapping("/{id}/convites")
    public ResponseEntity<List<ConviteResponseDTO>> listarConvites(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id) {
        return ResponseEntity.ok(conviteService.listarPendentes(id, usuarioLogado));
    }

    @DeleteMapping("/{id}/convites/{conviteId}")
    public ResponseEntity<Void> revogarConvite(@AuthenticationPrincipal UsuarioLogado usuarioLogado,
            @PathVariable Long id, @PathVariable Long conviteId) {
        conviteService.revogar(id, usuarioLogado, conviteId);
        return ResponseEntity.noContent().build();
    }
}