package com.gestaocompras.dto;

import com.gestaocompras.model.Usuario;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String perfil
) {

    public static UsuarioResponseDTO from(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil().name());
    }
}
