package com.gestaocompras.dto;

import com.gestaocompras.model.ModalidadeLicitacao;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LicitacaoRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 30) String numeroEdital,
        @NotNull(message = "obrigatória") ModalidadeLicitacao modalidade,
        @NotBlank(message = "obrigatório") @Size(max = 300) String objeto,
        @NotNull(message = "obrigatória") LocalDate dataAbertura,
        LocalDate dataEncerramento,
        @NotNull(message = "obrigatório") @PositiveOrZero(message = "não pode ser negativo")
        @Digits(integer = 15, fraction = 2) BigDecimal valorEstimado
) {
}
