package com.gestaocompras.dto;

import com.gestaocompras.model.MembroOrganizacao;
import java.time.LocalDateTime;

public record MembroResponseDTO(
        Long usuarioId,
        String nome,
        String email,
        String papel,
        LocalDateTime desde
) {

    public static MembroResponseDTO from(MembroOrganizacao membro) {
        var usuario = membro.getId().getUsuario();
        return new MembroResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                membro.getPapel().name(), membro.getDesde());
    }
}