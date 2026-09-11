package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.EmpenhoRequestDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.exception.SaldoInsuficienteException;
import com.gestaocompras.model.Contrato;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.Empenho;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.model.StatusEmpenho;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.EmpenhoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EmpenhoServiceTest {

    private static final long ORGANIZACAO_ID = 10L;
    private static final int ANO = 2026;
    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC);
    private static final YearMonth MES_CORRENTE = YearMonth.now(RELOGIO);
    private static final YearMonth MES_ANTERIOR = MES_CORRENTE.minusMonths(1);

    @Mock
    private EmpenhoRepository empenhoRepository;

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private DotacaoRepository dotacaoRepository;

    @Mock
    private DotacaoService dotacaoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    private EmpenhoService empenhoService;

    private DotacaoOrcamentaria dotacao;
    private Contrato contratoVigente;

    @BeforeEach
    void setUp() {
        empenhoService = new EmpenhoService(empenhoRepository, contratoRepository,
                dotacaoRepository, dotacaoService, usuarioRepository, organizacaoRepository,
                RELOGIO);
        Organizacao organizacao = Organizacao.builder().id(ORGANIZACAO_ID).nome("Prefeitura").build();
        lenient().when(organizacaoRepository.getReferenceById(ORGANIZACAO_ID)).thenReturn(organizacao);
        dotacao = DotacaoOrcamentaria.builder()
                .id(1L)
                .codigo("3.3.90.30")
                .descricao("Material de consumo")
                .anoExercicio(ANO)
                .saldoInicial(new BigDecimal("25000.00"))
                .saldoAtual(new BigDecimal("25000.00"))
                .organizacao(organizacao)
                .build();
        contratoVigente = Contrato.builder()
                .id(30L)
                .numero("014/2026")
                .objeto("Fornecimento de material de escritório")
                .valorTotal(new BigDecimal("60000.00"))
                .duracaoMeses(6)
                .dataInicio(MES_ANTERIOR.atDay(1))
                .status(StatusContrato.VIGENTE)
                .saldoRestante(new BigDecimal("60000.00"))
                .dotacao(dotacao)
                .fornecedor(Fornecedor.builder().id(2L).nome("Papelaria Central LTDA")
                        .cnpj("11444777000161").build())
                .organizacao(organizacao)
                .build();
    }

    private EmpenhoRequestDTO request(Integer mes, Integer ano) {
        return new EmpenhoRequestDTO(30L, mes, ano);
    }

    private void contratoEncontrado() {
        when(contratoRepository.findByIdComLock(30L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(contratoVigente));
    }

    private void dotacaoEncontrada() {
        when(dotacaoRepository.findByIdComLock(1L, ORGANIZACAO_ID)).thenReturn(Optional.of(dotacao));
    }

    private void competenciaNaoDuplicada(Integer mes) {
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ANO, mes,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(false);
    }

    private void competenciaAnteriorEmpenhada(Integer mesAnterior) {
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ANO, mesAnterior,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(true);
    }

    @Test
    void gerarDeveCriarEmpenhoDoMesAnteriorPendenteEDebitarOsDoisSaldos() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_ANTERIOR.getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
        assertThat(resposta.status()).isEqualTo(StatusEmpenho.EMPENHADO.name());
        verify(dotacaoService).debitar(eq(ORGANIZACAO_ID), eq(1L),
                eq(new BigDecimal("10000.00")),
                contains("%02d/%d".formatted(MES_ANTERIOR.getMonthValue(), ANO)));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("50000.00");
    }

    @Test
    void gerarDeveCriarEmpenhoDaCompetenciaCorrente() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
        assertThat(resposta.status()).isEqualTo(StatusEmpenho.EMPENHADO.name());
        verify(dotacaoService).debitar(eq(ORGANIZACAO_ID), eq(1L),
                eq(new BigDecimal("10000.00")),
                contains("%02d/%d".formatted(MES_CORRENTE.getMonthValue(), ANO)));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("50000.00");
    }

    @Test
    void gerarDeveUsarValorMensalArredondadoQuandoDivisaoNaoExata() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(MES_ANTERIOR.minusMonths(1).atDay(1));
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_ANTERIOR.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.minusMonths(1).getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("3333.33");
    }

    @Test
    void gerarDeveAbsorverResiduoNaUltimaCompetencia() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(MES_ANTERIOR.minusMonths(1).atDay(1));
        contratoVigente.setSaldoRestante(new BigDecimal("3333.34"));
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("3333.34");
        verify(dotacaoService).debitar(eq(ORGANIZACAO_ID), eq(1L),
                eq(new BigDecimal("3333.34")),
                contains("%02d/%d".formatted(MES_CORRENTE.getMonthValue(), ANO)));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("0.00");
    }

    @Test
    void somaDasParcelasDeveFecharOValorTotalAoDecorrerAsCompetencias() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(MES_ANTERIOR.minusMonths(1).atDay(1));
        contratoVigente.setSaldoRestante(new BigDecimal("10000.00"));
        contratoEncontrado();
        dotacaoEncontrada();
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        competenciaNaoDuplicada(MES_ANTERIOR.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.minusMonths(1).getMonthValue());
        var pendente = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO));
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        var corrente = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO));

        assertThat(pendente.valor()).isEqualByComparingTo("3333.33");
        assertThat(corrente.valor()).isEqualByComparingTo("3333.34");
        assertThat(pendente.valor().add(corrente.valor()))
                .isEqualByComparingTo("6666.67");
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("3333.33");
    }

    @Test
    void naoDeveEmitirCompetenciaFutura() {
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.plusMonths(1).getMonthValue(), ANO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("competência corrente");

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveEmitirCompetenciaRetroaticaAlemDeUmMes() {
        contratoVigente.setDataInicio(MES_ANTERIOR.minusMonths(1).atDay(1));
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.minusMonths(1).getMonthValue(), ANO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("competência corrente");

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarCompetenciaForaDaVigenciaDoContrato() {
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.plusMonths(6).getMonthValue(), ANO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarComMesInvalido() {
        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(13, ANO)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(empenhoRepository, never()).save(any(Empenho.class));
    }

    @Test
    void naoDeveGerarCompetenciaDuplicada() {
        contratoEncontrado();
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ANO, MES_ANTERIOR.getMonthValue(),
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(true);

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO)))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarComSaldoInsuficienteNaDotacao() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        dotacao.setSaldoAtual(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO)))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("dotação");

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
        verify(empenhoRepository, never()).save(any(Empenho.class));
    }

    @Test
    void naoDeveGerarComSaldoRestanteInsuficienteNoContrato() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        contratoVigente.setSaldoRestante(new BigDecimal("9000.00"));

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO)))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("contrato");

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarParaContratoNaoVigente() {
        contratoVigente.setStatus(StatusContrato.RESCINDIDO);
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDevePularMes() {
        contratoEncontrado();
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ANO, MES_ANTERIOR.getMonthValue(),
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(false);

        assertThatThrownBy(() -> empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_CORRENTE.getMonthValue(), ANO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("%02d/%d"
                        .formatted(MES_ANTERIOR.getMonthValue(), ANO));

        verify(dotacaoService, never()).debitar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void primeiroMesDaVigenciaNaoExigeCompetenciaAnterior() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_ANTERIOR.getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
    }

    @Test
    void devePermitirRecriarEmpenhoAposAnulacao() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_ANTERIOR.getMonthValue());
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                MES_ANTERIOR.getMonthValue(), ANO));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
        assertThat(resposta.status()).isEqualTo(StatusEmpenho.EMPENHADO.name());
    }

    @Test
    void anularDeveEstornarOSaldoDaDotacaoEDoContrato() {
        Empenho empenho = Empenho.builder()
                .id(40L)
                .contrato(contratoVigente)
                .mesReferencia(MES_ANTERIOR.getMonthValue())
                .anoReferencia(ANO)
                .valor(new BigDecimal("10000.00"))
                .status(StatusEmpenho.EMPENHADO)
                .dataEmissao(LocalDate.now(RELOGIO))
                .build();
        contratoVigente.setSaldoRestante(new BigDecimal("50000.00"));
        when(empenhoRepository.findByIdComLock(40L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(empenho));
        contratoEncontrado();

        var resposta = empenhoService.anular(ORGANIZACAO_ID, 40L);

        assertThat(resposta.status()).isEqualTo(StatusEmpenho.ANULADO.name());
        verify(empenhoRepository).findByIdComLock(40L, ORGANIZACAO_ID);
        verify(dotacaoService).creditar(eq(ORGANIZACAO_ID), eq(1L),
                eq(new BigDecimal("10000.00")), contains("anulação"),
                eq(TipoMovimentacao.ESTORNO));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("60000.00");
    }

    @Test
    void naoDeveAnularEmpenhoLiquidado() {
        Empenho empenho = Empenho.builder()
                .id(41L)
                .contrato(contratoVigente)
                .mesReferencia(MES_ANTERIOR.getMonthValue())
                .anoReferencia(ANO)
                .valor(new BigDecimal("10000.00"))
                .status(StatusEmpenho.LIQUIDADO)
                .dataEmissao(LocalDate.now(RELOGIO))
                .build();
        when(empenhoRepository.findByIdComLock(41L, ORGANIZACAO_ID))
                .thenReturn(Optional.of(empenho));

        assertThatThrownBy(() -> empenhoService.anular(ORGANIZACAO_ID, 41L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).creditar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void naoDeveAnularEmpenhoInexistente() {
        when(empenhoRepository.findByIdComLock(999L, ORGANIZACAO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> empenhoService.anular(ORGANIZACAO_ID, 999L))
                .isInstanceOf(NotFoundException.class);

        verify(dotacaoService, never()).creditar(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void gerarDevePreencherUsuarioAutenticadoNoEmpenho() {
        contratoEncontrado();
        dotacaoEncontrada();
        competenciaNaoDuplicada(MES_CORRENTE.getMonthValue());
        competenciaAnteriorEmpenhada(MES_ANTERIOR.getMonthValue());
        Usuario usuario = Usuario.builder()
                .id(5L)
                .nome("João")
                .email("joao@gestao.com")
                .perfil(Perfil.USUARIO)
                .build();
        when(usuarioRepository.findByEmail("joao@gestao.com")).thenReturn(Optional.of(usuario));
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("joao@gestao.com", null, List.of()));
        try {
            var resposta = empenhoService.gerar(ORGANIZACAO_ID, request(
                    MES_CORRENTE.getMonthValue(), ANO));

            assertThat(resposta.usuarioId()).isEqualTo(5L);
            verify(empenhoRepository).save(argThat((Empenho salvo) ->
                    salvo.getUsuario() != null && salvo.getUsuario().getId().equals(5L)));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void listarDeveEncaminharPaginacaoAoRepositorioComFiltros() {
        when(empenhoRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(Page.empty());

        empenhoService.listar(ORGANIZACAO_ID, 30L, null, 2, ANO, null, null,
                PageRequest.of(0, 10));

        verify(empenhoRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }
}