package com.gestaocompras.dto;

import com.gestaocompras.model.ConviteOrganizacao;
import java.time.LocalDateTime;

public record ConviteResponseDTO(
        Long id,
        Long organizacaoId,
        String organizacaoNome,
        String email,
        String codigo,
        String papel,
        LocalDateTime criadoEm
) {

    public static ConviteResponseDTO from(ConviteOrganizacao convite) {
        return new ConviteResponseDTO(convite.getId(), convite.getOrganizacao().getId(),
                convite.getOrganizacao().getNome(), convite.getEmail(), convite.getCodigo(),
                convite.getPapel().name(), convite.getCriadoEm());
    }
}