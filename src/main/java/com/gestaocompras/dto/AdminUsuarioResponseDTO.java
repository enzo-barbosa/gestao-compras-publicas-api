package com.gestaocompras.dto;

import com.gestaocompras.model.Usuario;

public record AdminUsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String perfil
) {

    public static AdminUsuarioResponseDTO from(Usuario usuario) {
        return new AdminUsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil().name());
    }
}