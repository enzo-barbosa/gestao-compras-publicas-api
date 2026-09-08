package com.gestaocompras.controller;

import com.gestaocompras.dto.AdminOrganizacaoResponseDTO;
import com.gestaocompras.dto.AdminPerfilRequestDTO;
import com.gestaocompras.dto.AdminUsuarioResponseDTO;
import com.gestaocompras.security.UsuarioLogado;
import com.gestaocompras.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @PutMapping("/usuarios/{id}/perfil")
    public ResponseEntity<AdminUsuarioResponseDTO> alterarPerfil(
            @AuthenticationPrincipal UsuarioLogado usuarioLogado, @PathVariable Long id,
            @Valid @RequestBody AdminPerfilRequestDTO request) {
        return ResponseEntity.ok(
                adminService.alterarPerfil(id, request, usuarioLogado.getUsername()));
    }

    @GetMapping("/organizacoes")
    public ResponseEntity<List<AdminOrganizacaoResponseDTO>> listarOrganizacoes() {
        return ResponseEntity.ok(adminService.listarOrganizacoes());
    }
}