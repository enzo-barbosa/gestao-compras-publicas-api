package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.CreditoSuplementarRequestDTO;
import com.gestaocompras.dto.CreditoSuplementarResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.SaldoInsuficienteException;
import com.gestaocompras.model.CreditoSuplementar;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.repository.CreditoSuplementarRepository;
import com.gestaocompras.repository.DotacaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditoSuplementarServiceTest {

    @Mock
    private DotacaoService dotacaoService;

    @Mock
    private DotacaoRepository dotacaoRepository;

    @Mock
    private CreditoSuplementarRepository creditoSuplementarRepository;

    @InjectMocks
    private CreditoSuplementarService creditoSuplementarService;

    private DotacaoOrcamentaria origem;
    private DotacaoOrcamentaria destino;

    @BeforeEach
    void setUp() {
        origem = DotacaoOrcamentaria.builder()
                .id(1L)
                .codigo("8.2.2.09.001")
                .descricao("Manutencao")
                .saldoInicial(new BigDecimal("50000.00"))
                .saldoAtual(new BigDecimal("50000.00"))
                .anoExercicio(2026)
                .build();
        destino = DotacaoOrcamentaria.builder()
                .id(2L)
                .codigo("8.2.2.09.002")
                .descricao("Iluminacao")
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoAtual(new BigDecimal("2000.00"))
                .anoExercicio(2026)
                .build();
    }

    private CreditoSuplementarRequestDTO request() {
        return new CreditoSuplementarRequestDTO(1L, 2L, new BigDecimal("10000.00"),
                "Reforco de dotação", null);
    }

    @Test
    void realizarDeveDebitarOrigemCreditarDestinoERegistrarORemaniejamento() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(origem));
        when(dotacaoRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(creditoSuplementarRepository.save(any(CreditoSuplementar.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        CreditoSuplementarResponseDTO resposta = creditoSuplementarService.realizar(request());

        verify(dotacaoService).debitar(1L, new BigDecimal("10000.00"), "Reforco de dotação");
        verify(dotacaoService).creditar(2L, new BigDecimal("10000.00"), "Reforco de dotação");
        assertThat(resposta.dotacaoOrigemCodigo()).isEqualTo("8.2.2.09.001");
        assertThat(resposta.dotacaoDestinoCodigo()).isEqualTo("8.2.2.09.002");
        assertThat(resposta.data()).isNotNull();
    }

    @Test
    void realizarNaoDevePermitirOrigemIgualAoDestino() {
        CreditoSuplementarRequestDTO invalida = new CreditoSuplementarRequestDTO(1L, 1L,
                new BigDecimal("1000.00"), null, null);

        assertThatThrownBy(() -> creditoSuplementarService.realizar(invalida))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verifyNoInteractions(dotacaoService, creditoSuplementarRepository);
    }

    @Test
    void realizarDeveLancarNotFoundQuandoOrigemNaoExistir() {
        when(dotacaoRepository.findById(99L)).thenReturn(Optional.empty());
        CreditoSuplementarRequestDTO invalida = new CreditoSuplementarRequestDTO(99L, 2L,
                new BigDecimal("1000.00"), null, null);

        assertThatThrownBy(() -> creditoSuplementarService.realizar(invalida))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void realizarDevePropagarSaldoInsuficienteESemRegistrarRemanejamento() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(origem));
        when(dotacaoRepository.findById(2L)).thenReturn(Optional.of(destino));
        doThrow(new SaldoInsuficienteException("Dotação orçamentária", origem.getSaldoAtual(),
                new BigDecimal("999999.00")))
                .when(dotacaoService).debitar(1L, new BigDecimal("999999.00"), "Crédito suplementar");
        CreditoSuplementarRequestDTO invalida = new CreditoSuplementarRequestDTO(1L, 2L,
                new BigDecimal("999999.00"), null, null);

        assertThatThrownBy(() -> creditoSuplementarService.realizar(invalida))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(creditoSuplementarRepository, never()).save(any(CreditoSuplementar.class));
    }
}
