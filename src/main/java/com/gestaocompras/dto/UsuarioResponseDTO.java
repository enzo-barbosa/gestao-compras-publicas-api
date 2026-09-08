package com.gestaocompras.dto;

import com.gestaocompras.model.Usuario;
import java.util.List;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String perfil,
        List<OrganizacaoResponseDTO> organizacoes
) {

    public static UsuarioResponseDTO from(Usuario usuario,
            List<OrganizacaoResponseDTO> organizacoes) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil().name(), organizacoes);
    }
}
