package com.gestaocompras.dto;

import com.gestaocompras.model.Organizacao;
import java.time.LocalDateTime;

public record AdminOrganizacaoResponseDTO(
        Long id,
        String nome,
        LocalDateTime criadoEm,
        Long totalMembros
) {

    public static AdminOrganizacaoResponseDTO from(Organizacao organizacao, long totalMembros) {
        return new AdminOrganizacaoResponseDTO(organizacao.getId(), organizacao.getNome(),
                organizacao.getCriadoEm(), totalMembros);
    }
}