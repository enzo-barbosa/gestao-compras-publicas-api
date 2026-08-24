package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.ContratoRequestDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Contrato;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.Licitacao;
import com.gestaocompras.model.ModalidadeLicitacao;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.model.StatusLicitacao;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private DotacaoRepository dotacaoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private LicitacaoRepository licitacaoRepository;

    @InjectMocks
    private ContratoService contratoService;

    private DotacaoOrcamentaria dotacao;
    private Fornecedor fornecedor;
    private Licitacao licitacaoEncerradaComVencedor;

    @BeforeEach
    void setUp() {
        dotacao = DotacaoOrcamentaria.builder()
                .id(1L)
                .codigo("3.3.90.30")
                .descricao("Material de consumo")
                .anoExercicio(2026)
                .saldoInicial(new BigDecimal("500000.00"))
                .saldoAtual(new BigDecimal("500000.00"))
                .build();
        fornecedor = Fornecedor.builder()
                .id(10L)
                .nome("Papelaria Central LTDA")
                .cnpj("11444777000161")
                .build();
        licitacaoEncerradaComVencedor = Licitacao.builder()
                .id(20L)
                .numeroEdital("001/2026")
                .modalidade(ModalidadeLicitacao.PREGAO)
                .objeto("Aquisição de material de escritório")
                .dataAbertura(LocalDate.of(2026, 8, 10))
                .dataEncerramento(LocalDate.of(2026, 8, 30))
                .status(StatusLicitacao.ENCERRADA)
                .valorEstimado(new BigDecimal("120000.00"))
                .vencedor(fornecedor)
                .build();
    }

    private ContratoRequestDTO request(BigDecimal valorTotal, Integer duracaoMeses) {
        return new ContratoRequestDTO("012/2026", "Fornecimento de material de escritório",
                valorTotal, duracaoMeses, LocalDate.of(2026, 9, 1), 1L, null, 10L);
    }

    private ContratoRequestDTO requestComLicitacao(Long licitacaoId) {
        return new ContratoRequestDTO("013/2026", "Fornecimento de material de escritório",
                new BigDecimal("120000.00"), 12, LocalDate.of(2026, 9, 1),
                1L, licitacaoId, 10L);
    }

    @Test
    void criarDeveDefinirSaldoRestanteIgualAoValorTotalEStatusVigente() {
        when(contratoRepository.existsByNumero("012/2026")).thenReturn(false);
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        when(fornecedorRepository.findById(10L)).thenReturn(Optional.of(fornecedor));
        when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = contratoService.criar(request(new BigDecimal("120000.00"), 12));

        assertThat(resposta.saldoRestante()).isEqualByComparingTo("120000.00");
        assertThat(resposta.status()).isEqualTo(StatusContrato.VIGENTE.name());
        assertThat(resposta.valorMensal()).isEqualByComparingTo("10000.00");
        assertThat(resposta.dataFimPrevista()).isEqualTo(LocalDate.of(2027, 8, 31));
    }

    @Test
    void calcularValorMensalDeveArredondarComHalfUpQuandoNaoExato() {
        var contrato = Contrato.builder()
                .valorTotal(new BigDecimal("10000.00"))
                .duracaoMeses(3)
                .build();

        assertThat(contrato.calcularValorMensal()).isEqualByComparingTo("3333.33");
    }

    @Test
    void calcularValorCompetenciaDeveAbsorverResiduoNaUltimaParcela() {
        var contrato = Contrato.builder()
                .valorTotal(new BigDecimal("10000.00"))
                .duracaoMeses(3)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .build();

        assertThat(contrato.calcularValorCompetencia(YearMonth.of(2026, 1)))
                .isEqualByComparingTo("3333.33");
        assertThat(contrato.calcularValorCompetencia(YearMonth.of(2026, 2)))
                .isEqualByComparingTo("3333.33");
        assertThat(contrato.calcularValorCompetencia(YearMonth.of(2026, 3)))
                .isEqualByComparingTo("3333.34");
    }

    @Test
    void calcularValorCompetenciaDeveRetornarValorTotalQuandoDuracaoForUmMes() {
        var contrato = Contrato.builder()
                .valorTotal(new BigDecimal("7777.77"))
                .duracaoMeses(1)
                .dataInicio(LocalDate.of(2026, 5, 15))
                .build();

        assertThat(contrato.calcularValorCompetencia(YearMonth.of(2026, 5)))
                .isEqualByComparingTo("7777.77");
    }

    @Test
    void calcularValorCompetenciaDeveRejeitarCompetenciaForaDaVigencia() {
        var contrato = Contrato.builder()
                .valorTotal(new BigDecimal("10000.00"))
                .duracaoMeses(3)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .build();

        assertThatThrownBy(() -> contrato.calcularValorCompetencia(YearMonth.of(2026, 4)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criarNaoDeveAceitarNumeroDuplicado() {
        when(contratoRepository.existsByNumero("012/2026")).thenReturn(true);

        assertThatThrownBy(() -> contratoService.criar(request(new BigDecimal("120000.00"), 12)))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void criarDeveLancarNotFoundParaDotacaoInexistente() {
        when(contratoRepository.existsByNumero("012/2026")).thenReturn(false);
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.criar(request(new BigDecimal("120000.00"), 12)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void criarNaoDeveVincularLicitacaoVencidaPorOutroFornecedor() {
        Fornecedor outro = Fornecedor.builder().id(99L).nome("Outro LTDA").cnpj("45723174000110").build();
        licitacaoEncerradaComVencedor.setVencedor(outro);
        when(contratoRepository.existsByNumero("013/2026")).thenReturn(false);
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        when(fornecedorRepository.findById(10L)).thenReturn(Optional.of(fornecedor));
        when(licitacaoRepository.findById(20L))
                .thenReturn(Optional.of(licitacaoEncerradaComVencedor));

        assertThatThrownBy(() -> contratoService.criar(requestComLicitacao(20L)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void criarNaoDeveVincularLicitacaoAindaAberta() {
        licitacaoEncerradaComVencedor.setStatus(StatusLicitacao.ABERTA);
        licitacaoEncerradaComVencedor.setVencedor(null);
        when(contratoRepository.existsByNumero("013/2026")).thenReturn(false);
        when(dotacaoRepository.findById(1L)).thenReturn(Optional.of(dotacao));
        when(fornecedorRepository.findById(10L)).thenReturn(Optional.of(fornecedor));
        when(licitacaoRepository.findById(20L))
                .thenReturn(Optional.of(licitacaoEncerradaComVencedor));

        assertThatThrownBy(() -> contratoService.criar(requestComLicitacao(20L)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void atualizarNaoDeveAlterarValorTotalEDuracao() {
        Contrato contrato = Contrato.builder()
                .id(30L)
                .numero("012/2026")
                .valorTotal(new BigDecimal("120000.00"))
                .duracaoMeses(12)
                .dotacao(dotacao)
                .fornecedor(fornecedor)
                .build();
        when(contratoRepository.findById(30L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService
                .atualizar(30L, request(new BigDecimal("90000.00"), 12)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizarDeveAlterarObjetoMantendoSaldos() {
        Contrato contrato = Contrato.builder()
                .id(30L)
                .numero("012/2026")
                .objeto("Objeto original")
                .valorTotal(new BigDecimal("120000.00"))
                .duracaoMeses(12)
                .dataInicio(LocalDate.of(2026, 9, 1))
                .status(StatusContrato.VIGENTE)
                .saldoRestante(new BigDecimal("80000.00"))
                .dotacao(dotacao)
                .fornecedor(fornecedor)
                .build();
        when(contratoRepository.findById(30L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.findByNumero("012/2026")).thenReturn(Optional.empty());

        var resposta = contratoService.atualizar(30L,
                request(new BigDecimal("120000.00"), 12));

        assertThat(resposta.objeto()).isEqualTo("Fornecimento de material de escritório");
        assertThat(resposta.saldoRestante()).isEqualByComparingTo("80000.00");
    }
}
