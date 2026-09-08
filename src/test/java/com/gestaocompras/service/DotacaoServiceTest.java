package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.MovimentacaoDotacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DotacaoServiceTest {

    private static final long ORGANIZACAO_ID = 10L;

    @Mock
    private DotacaoRepository dotacaoRepository;

    @Mock
    private MovimentacaoDotacaoRepository movimentacaoRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

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
        Organizacao organizacao = Organizacao.builder().id(ORGANIZACAO_ID).nome("Prefeitura").build();
        lenient().when(organizacaoRepository.getReferenceById(ORGANIZACAO_ID))
                .thenReturn(organizacao);
        dotacao.setOrganizacao(organizacao);
    }

    private DotacaoRequestDTO request() {
        return new DotacaoRequestDTO("8.2.2.09.001", "Manutencao de areas publicas",
                new BigDecimal("100000.00"), 2026);
    }

    @Test
    void criarDeveDefinirSaldoAtualIgualAoInicialERegistrarMovimentacaoInicial() {
        when(dotacaoRepository.existsByCodigoAndOrganizacaoId("8.2.2.09.001", ORGANIZACAO_ID))
                .thenReturn(false);
        when(dotacaoRepository.save(any(DotacaoOrcamentaria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        DotacaoResponseDTO resposta = dotacaoService.criar(ORGANIZACAO_ID, request());

        assertThat(resposta.saldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
        ArgumentCaptor<MovimentacaoDotacao> captor = ArgumentCaptor.forClass(MovimentacaoDotacao.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.CREDITO);
        assertThat(captor.getValue().getDescricao()).isEqualTo("Saldo inicial");
    }

    @Test
    void criarNaoDeveAceitarCodigoDuplicado() {
        when(dotacaoRepository.existsByCodigoAndOrganizacaoId("8.2.2.09.001", ORGANIZACAO_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> dotacaoService.criar(ORGANIZACAO_ID, request()))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(dotacaoRepository, never()).save(any(DotacaoOrcamentaria.class));
    }

    @Test
    void debitarDeveReduzirSaldoERegistrarMovimentacaoQuandoHouverSaldoSuficiente() {
        when(dotacaoRepository.findByIdComLock(1L, ORGANIZACAO_ID)).thenReturn(Optional.of(dotacao));

        dotacaoService.debitar(ORGANIZACAO_ID, 1L, new BigDecimal("8000.00"), "Empenho mensal");

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("92000.00"));
        verify(movimentacaoRepository).save(any(MovimentacaoDotacao.class));
    }

    @Test
    void debitarNaoDeveAceitarValorAcimaDoSaldoDisponivel() {
        when(dotacaoRepository.findByIdComLock(1L, ORGANIZACAO_ID)).thenReturn(Optional.of(dotacao));

        assertThatThrownBy(() -> dotacaoService.debitar(ORGANIZACAO_ID, 1L,
                new BigDecimal("150000.00"), "Empenho"))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
        verify(movimentacaoRepository, never()).save(any(MovimentacaoDotacao.class));
    }

    @Test
    void creditarDeveAumentarSaldoERegistrarMovimentacaoSuplementar() {
        when(dotacaoRepository.findByIdComLock(1L, ORGANIZACAO_ID)).thenReturn(Optional.of(dotacao));

        dotacaoService.creditar(ORGANIZACAO_ID, 1L, new BigDecimal("25000.00"), "Remanejamento");

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo(new BigDecimal("125000.00"));
        ArgumentCaptor<MovimentacaoDotacao> captor = ArgumentCaptor.forClass(MovimentacaoDotacao.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.CREDITO_SUPLEMENTAR);
    }

    @Test
    void atualizarNaoDevePermitirAlterarSaldoInicial() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));
        DotacaoRequestDTO requisicao = new DotacaoRequestDTO("8.2.2.09.001", "Descricao nova",
                new BigDecimal("500000.00"), 2027);

        assertThatThrownBy(() -> dotacaoService.atualizar(ORGANIZACAO_ID, 1L, requisicao))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(dotacao.getSaldoInicial()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void atualizarDeveAlterarDadosCadastraisSemMexerNosSaldos() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));
        when(dotacaoRepository.findByCodigoAndOrganizacaoId("8.2.2.09.002", ORGANIZACAO_ID))
                .thenReturn(Optional.empty());
        DotacaoRequestDTO requisicao = new DotacaoRequestDTO("8.2.2.09.002", "Iluminacao publica",
                new BigDecimal("100000.00"), 2026);

        DotacaoResponseDTO resposta = dotacaoService.atualizar(ORGANIZACAO_ID, 1L, requisicao);

        assertThat(resposta.descricao()).isEqualTo("Iluminacao publica");
        assertThat(resposta.saldoAtual()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void removerNaoDevePermitirExcluirDotacaoComMovimentacoesRegistradas() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));
        when(movimentacaoRepository.countByDotacaoId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> dotacaoService.remover(ORGANIZACAO_ID, 1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoRepository, never()).delete(any(DotacaoOrcamentaria.class));
    }

    @Test
    void buscarPorIdDeveLancarNotFoundQuandoNaoExistir() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(99L, ORGANIZACAO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dotacaoService.buscarPorId(ORGANIZACAO_ID, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removerDeveExcluirDotacaoComApenasMovimentacaoInicial() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));
        when(movimentacaoRepository.countByDotacaoId(1L)).thenReturn(1L);
        when(movimentacaoRepository.findAllByDotacaoId(1L))
                .thenReturn(List.of(new MovimentacaoDotacao()));

        dotacaoService.remover(ORGANIZACAO_ID, 1L);

        verify(movimentacaoRepository).deleteAll(anyList());
        verify(dotacaoRepository).delete(dotacao);
    }

    @Test
    void listarDeveFiltrarPorAnoExercicioQuandoInformado() {
        when(dotacaoRepository.findByAnoExercicioAndOrganizacaoId(eq(2026),
                eq(ORGANIZACAO_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dotacao)));

        var resultado = dotacaoService.listar(ORGANIZACAO_ID, 2026, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        verify(dotacaoRepository, never()).findByOrganizacaoId(any(), any());
    }

    @Test
    void listarDeveRetornarTodasQuandoAnoNaoInformado() {
        when(dotacaoRepository.findByOrganizacaoId(ORGANIZACAO_ID, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(dotacao)));

        var resultado = dotacaoService.listar(ORGANIZACAO_ID, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    void consultarSaldoDeveRetornarSaldoAtualDaDotacao() {
        dotacao.setSaldoAtual(new BigDecimal("87500.00"));
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));

        assertThat(dotacaoService.consultarSaldo(ORGANIZACAO_ID, 1L))
                .isEqualByComparingTo("87500.00");
    }

    @Test
    void consultarSaldoDeveLancarNotFoundQuandoDotacaoInexistente() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(99L, ORGANIZACAO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dotacaoService.consultarSaldo(ORGANIZACAO_ID, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listarMovimentacoesDeveRetornarHistoricoDaDotacao() {
        when(dotacaoRepository.findByIdAndOrganizacaoId(1L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(dotacao));
        MovimentacaoDotacao movimentacao = MovimentacaoDotacao.builder()
                .id(7L)
                .dotacao(dotacao)
                .tipo(TipoMovimentacao.DEBITO)
                .valor(new BigDecimal("8000.00"))
                .descricao("Empenho mensal")
                .dataHora(LocalDateTime.now())
                .build();
        when(movimentacaoRepository.findByDotacaoId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimentacao)));

        var resultado = dotacaoService.listarMovimentacoes(ORGANIZACAO_ID, 1L, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).tipo()).isEqualTo(TipoMovimentacao.DEBITO.name());
        assertThat(resultado.getContent().get(0).valor()).isEqualByComparingTo("8000.00");
    }

    @Test
    void creditarComTipoEspecificoDeveRegistrarMovimentacaoDoTipoInformado() {
        dotacao.setSaldoAtual(new BigDecimal("92000.00"));
        when(dotacaoRepository.findByIdComLock(1L, ORGANIZACAO_ID)).thenReturn(Optional.of(dotacao));

        dotacaoService.creditar(ORGANIZACAO_ID, 1L, new BigDecimal("8000.00"),
                "Estorno de anulação – empenho competência 01/2026", TipoMovimentacao.ESTORNO);

        assertThat(dotacao.getSaldoAtual()).isEqualByComparingTo("100000.00");
        ArgumentCaptor<MovimentacaoDotacao> captor = ArgumentCaptor.forClass(MovimentacaoDotacao.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.ESTORNO);
    }
}