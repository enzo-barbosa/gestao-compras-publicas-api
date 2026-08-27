package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.model.StatusEmpenho;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.EmpenhoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EmpenhoServiceTest {

    @Mock
    private EmpenhoRepository empenhoRepository;

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private DotacaoService dotacaoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EmpenhoService empenhoService;

    private DotacaoOrcamentaria dotacao;
    private Contrato contratoVigente;

    @BeforeEach
    void setUp() {
        dotacao = DotacaoOrcamentaria.builder()
                .id(1L)
                .codigo("3.3.90.30")
                .descricao("Material de consumo")
                .anoExercicio(2026)
                .saldoInicial(new BigDecimal("25000.00"))
                .saldoAtual(new BigDecimal("25000.00"))
                .build();
        contratoVigente = Contrato.builder()
                .id(30L)
                .numero("014/2026")
                .objeto("Fornecimento de material de escritório")
                .valorTotal(new BigDecimal("60000.00"))
                .duracaoMeses(6)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .status(StatusContrato.VIGENTE)
                .saldoRestante(new BigDecimal("60000.00"))
                .dotacao(dotacao)
                .fornecedor(Fornecedor.builder().id(2L).nome("Papelaria Central LTDA")
                        .cnpj("11444777000161").build())
                .build();
    }

    private EmpenhoRequestDTO request(Integer mes, Integer ano) {
        return new EmpenhoRequestDTO(30L, mes, ano);
    }

    private void contratoEncontrado() {
        when(contratoRepository.findById(30L)).thenReturn(Optional.of(contratoVigente));
    }

    private void competenciaNaoDuplicada(Integer mes, Integer ano) {
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ano, mes,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(false);
    }

    private void competenciaAtivaAnterior(Integer mesAnterior, Integer ano) {
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, ano, mesAnterior,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(true);
    }

    @Test
    void gerarDeveCriarEmpenhoComValorMensalEDebitarOsDoisSaldos() {
        contratoEncontrado();
        competenciaNaoDuplicada(1, 2026);
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(request(1, 2026));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
        assertThat(resposta.status()).isEqualTo(StatusEmpenho.EMPENHADO.name());
        verify(dotacaoService).debitar(eq(1L), eq(new BigDecimal("10000.00")),
                contains("01/2026"));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("50000.00");
    }

    @Test
    void gerarDeveUsarValorMensalArredondadoQuandoDivisaoNaoExata() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(LocalDate.of(2026, 1, 1));
        contratoEncontrado();
        competenciaNaoDuplicada(1, 2026);
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(request(1, 2026));

        assertThat(resposta.valor()).isEqualByComparingTo("3333.33");
    }

    @Test
    void gerarDeveAbsorverResiduoNaUltimaCompetencia() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(LocalDate.of(2026, 1, 1));
        contratoVigente.setSaldoRestante(new BigDecimal("3333.34"));
        contratoEncontrado();
        competenciaNaoDuplicada(3, 2026);
        competenciaAtivaAnterior(2, 2026);
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(request(3, 2026));

        assertThat(resposta.valor()).isEqualByComparingTo("3333.34");
        verify(dotacaoService).debitar(eq(1L), eq(new BigDecimal("3333.34")),
                contains("03/2026"));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("0.00");
    }

    @Test
    void somaDasParcelasDeveFecharOValorTotal() {
        contratoVigente.setValorTotal(new BigDecimal("10000.00"));
        contratoVigente.setDuracaoMeses(3);
        contratoVigente.setDataInicio(LocalDate.of(2026, 1, 1));
        contratoVigente.setSaldoRestante(new BigDecimal("10000.00"));
        contratoEncontrado();
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        competenciaNaoDuplicada(1, 2026);
        var janeiro = empenhoService.gerar(request(1, 2026));
        competenciaNaoDuplicada(2, 2026);
        competenciaAtivaAnterior(1, 2026);
        var fevereiro = empenhoService.gerar(request(2, 2026));
        competenciaNaoDuplicada(3, 2026);
        competenciaAtivaAnterior(2, 2026);
        var marco = empenhoService.gerar(request(3, 2026));

        assertThat(janeiro.valor()).isEqualByComparingTo("3333.33");
        assertThat(fevereiro.valor()).isEqualByComparingTo("3333.33");
        assertThat(marco.valor()).isEqualByComparingTo("3333.34");
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("0.00");
    }

    @Test
    void naoDeveGerarCompetenciaForaDaVigenciaDoContrato() {
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(request(7, 2026)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarComMesInvalido() {
        assertThatThrownBy(() -> empenhoService.gerar(request(13, 2026)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(empenhoRepository, never()).save(any(Empenho.class));
    }

    @Test
    void naoDeveGerarCompetenciaDuplicada() {
        contratoEncontrado();
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, 2026, 1,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(true);

        assertThatThrownBy(() -> empenhoService.gerar(request(1, 2026)))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarComSaldoInsuficienteNaDotacao() {
        contratoEncontrado();
        competenciaNaoDuplicada(2, 2026);
        competenciaAtivaAnterior(1, 2026);
        dotacao.setSaldoAtual(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> empenhoService.gerar(request(2, 2026)))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("dotação");

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
        verify(empenhoRepository, never()).save(any(Empenho.class));
    }

    @Test
    void naoDeveGerarComSaldoRestanteInsuficienteNoContrato() {
        contratoEncontrado();
        competenciaNaoDuplicada(5, 2026);
        competenciaAtivaAnterior(4, 2026);
        contratoVigente.setSaldoRestante(new BigDecimal("9000.00"));

        assertThatThrownBy(() -> empenhoService.gerar(request(5, 2026)))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("contrato");

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
    }

    @Test
    void naoDeveGerarParaContratoNaoVigente() {
        contratoVigente.setStatus(StatusContrato.RESCINDIDO);
        contratoEncontrado();

        assertThatThrownBy(() -> empenhoService.gerar(request(1, 2026)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
    }

    @Test
    void naoDevePularMes() {
        contratoEncontrado();
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, 2026, 2,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(false);

        assertThatThrownBy(() -> empenhoService.gerar(request(3, 2026)))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("02/2026");

        verify(dotacaoService, never()).debitar(anyLong(), any(), anyString());
    }

    @Test
    void primeiroMesDaVigenciaNaoExigeCompetenciaAnterior() {
        contratoEncontrado();
        competenciaNaoDuplicada(1, 2026);
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(request(1, 2026));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
    }

    @Test
    void devePermitirRecriarEmpenhoAposAnulacao() {
        contratoEncontrado();
        when(empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        30L, 2026, 1,
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO)))
                .thenReturn(false);
        when(empenhoRepository.save(any(Empenho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = empenhoService.gerar(request(1, 2026));

        assertThat(resposta.valor()).isEqualByComparingTo("10000.00");
        assertThat(resposta.status()).isEqualTo(StatusEmpenho.EMPENHADO.name());
    }

    @Test
    void anularDeveEstornarOSaldoDaDotacaoEDoContrato() {
        Empenho empenho = Empenho.builder()
                .id(40L)
                .contrato(contratoVigente)
                .mesReferencia(1)
                .anoReferencia(2026)
                .valor(new BigDecimal("10000.00"))
                .status(StatusEmpenho.EMPENHADO)
                .dataEmissao(LocalDate.now())
                .build();
        contratoVigente.setSaldoRestante(new BigDecimal("50000.00"));
        when(empenhoRepository.findById(40L)).thenReturn(Optional.of(empenho));

        var resposta = empenhoService.anular(40L);

        assertThat(resposta.status()).isEqualTo(StatusEmpenho.ANULADO.name());
        verify(dotacaoService).creditar(eq(1L), eq(new BigDecimal("10000.00")),
                contains("anulação"), eq(TipoMovimentacao.ESTORNO));
        assertThat(contratoVigente.getSaldoRestante()).isEqualByComparingTo("60000.00");
    }

    @Test
    void naoDeveAnularEmpenhoLiquidado() {
        Empenho empenho = Empenho.builder()
                .id(41L)
                .contrato(contratoVigente)
                .mesReferencia(1)
                .anoReferencia(2026)
                .valor(new BigDecimal("10000.00"))
                .status(StatusEmpenho.LIQUIDADO)
                .dataEmissao(LocalDate.now())
                .build();
        when(empenhoRepository.findById(41L)).thenReturn(Optional.of(empenho));

        assertThatThrownBy(() -> empenhoService.anular(41L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(dotacaoService, never()).creditar(anyLong(), any(), anyString());
    }

    @Test
    void naoDeveAnularEmpenhoInexistente() {
        when(empenhoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empenhoService.anular(999L))
                .isInstanceOf(NotFoundException.class);

        verify(dotacaoService, never()).creditar(anyLong(), any(), anyString());
    }

    @Test
    void gerarDevePreencherUsuarioAutenticadoNoEmpenho() {
        contratoEncontrado();
        competenciaNaoDuplicada(3, 2026);
        competenciaAtivaAnterior(2, 2026);
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
            var resposta = empenhoService.gerar(request(3, 2026));

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

        empenhoService.listar(30L, null, 2, 2026, null, null, PageRequest.of(0, 10));

        verify(empenhoRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 10)));
    }
}
