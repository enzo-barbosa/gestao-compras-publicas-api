package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.DotacaoRequestDTO;
import com.gestaocompras.dto.DotacaoResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.exception.SaldoInsuficienteException;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.MovimentacaoDotacao;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.MovimentacaoDotacaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DotacaoServiceTest {

    @Mock
    private DotacaoRepository dotacaoRepository;

    @Mock
    private MovimentacaoDotacaoRepository movimentacaoRepository;

    @InjectMocks
    private DotacaoService dotacaoService;

    private DotacaoOrcamentaria dotacao;

    @BeforeEach
    void setUp() {
        dotacao = DotacaoOrcamentaria.builder()
                .id(1L)
                .codigo("8.2.2.09.001")
                .descricao("Manutencao de areas publicas")
                .saldoInicial(new BigDecimal("100000.00"))
                .saldoAtual(new BigDecimal("100000.00"))
                .anoExercicio(2026)
                .build();
    }

    private DotacaoRequestDTO request() {
        return new DotacaoRequestDTO("8.2.2.09.001", "Manutencao de areas publicas",
                new BigDecimal("100000.00"), 2026);
    }

    @Test
    void criarDeveDefinirSaldoAtualIgualAoInicialERegistrarMovimentacaoInicial() {
        when(dotacaoRepository.existsByCodigo("8.2.2.09.001")).thenReturn(false);
        when(dotacaoRepository.save(any(DotacaoOrcamentaria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        DotacaoResponseDTO resposta = dotacaoService.criar(request());

        assertThat(resposta.saldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
        ArgumentCaptor<MovimentacaoDotacao> captor = ArgumentCaptor.forClass(MovimentacaoDotacao.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.CREDITO);
        assertThat(captor.getValue().getDescricao()).isEqualTo("Saldo inicial");
    }

    @Test
    void criarNaoDeveAceitarCodigoDuplicado() {
        when(dotacaoRepository.existsByCodigo("8.2.2.09.001")).thenReturn(true);

        assertThatThrownBy(() -> dotacaoService.criar(request()))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(dotacaoRepository, never()).save(any(DotacaoOrcamentaria.class));
    }

    @Test
    void debitarDeveReduzirSaldoERegistrarMovimentacaoQuandoHouverSaldoSuficiente() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));

        dotacaoService.debitar(1L, new BigDecimal("8000.00"), "Empenho mensal");

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("92000.00"));
        verify(movimentacaoRepository).save(any(MovimentacaoDotacao.class));
    }

    @Test
    void debitarNaoDeveAceitarValorAcimaDoSaldoDisponivel() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));

        assertThatThrownBy(() -> dotacaoService.debitar(1L, new BigDecimal("150000.00"), "Empenho"))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
        verify(movimentacaoRepository, never()).save(any(MovimentacaoDotacao.class));
    }

    @Test
    void creditarDeveAumentarSaldoERegistrarMovimentacaoSuplementar() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));

        dotacaoService.creditar(1L, new BigDecimal("25000.00"), "Remanejamento");

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("125000.00"));
        ArgumentCaptor<MovimentacaoDotacao> captor = ArgumentCaptor.forClass(MovimentacaoDotacao.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.CREDITO_SUPLEMENTAR);
    }

    @Test
    void atualizarNaoDevePermitirAlterarSaldoInicial() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        DotacaoRequestDTO requisicao = new DotacaoRequestDTO("8.2.2.09.001", "Descricao nova",
                new BigDecimal("500000.00"), 2027);

        assertThatThrownBy(() -> dotacaoService.atualizar(1L, requisicao))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(dotacao.getSaldoInicial()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void atualizarDeveAlterarDadosCadastraisSemMexerNosSaldos() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        DotacaoRequestDTO requisicao = new DotacaoRequestDTO("8.2.2.09.002", "Iluminacao publica",
                new BigDecimal("100000.00"), 2026);

        DotacaoResponseDTO resposta = dotacaoService.atualizar(1L, requisicao);

        assertThat(resposta.descricao()).isEqualTo("Iluminacao publica");
        assertThat(resposta.saldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void removerNaoDevePermitirExcluirDotacaoComMovimentacoesRegistradas() {
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        when(movimentacaoRepository.countByDotacaoId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> dotacaoService.remover(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoRepository, never()).delete(any(DotacaoOrcamentaria.class));
    }

    @Test
    void buscarPorIdDeveLancarNotFoundQuandoNaoExistir() {
        when(dotacaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dotacaoService.buscarPorId(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
