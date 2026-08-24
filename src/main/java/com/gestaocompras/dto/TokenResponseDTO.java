package com.gestaocompras.dto;

public record TokenResponseDTO(
        String token,
        String tipo,
        Long usuarioId,
        String nome,
        String email,
        String perfil
) {

    public static TokenResponseDTO of(String token, com.gestaocompras.model.Usuario usuario) {
        return new TokenResponseDTO(token, "Bearer", usuario.getId(), usuario.getNome(),
                usuario.getEmail(), usuario.getPerfil().name());
    }
}
