package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.LicitacaoRequestDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.Licitacao;
import com.gestaocompras.model.ModalidadeLicitacao;
import com.gestaocompras.model.StatusLicitacao;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicitacaoServiceTest {

    @Mock
    private LicitacaoRepository licitacaoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @InjectMocks
    private LicitacaoService licitacaoService;

    private Licitacao licitacaoAberta;
    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        licitacaoAberta = Licitacao.builder()
                .id(1L)
                .numeroEdital("001/2026")
                .modalidade(ModalidadeLicitacao.PREGAO)
                .objeto("Aquisição de material de escritório")
                .dataAbertura(LocalDate.of(2026, 8, 10))
                .dataEncerramento(LocalDate.of(2026, 8, 30))
                .status(StatusLicitacao.ABERTA)
                .valorEstimado(new BigDecimal("80000.00"))
                .build();
        fornecedor = Fornecedor.builder()
                .id(10L)
                .nome("Papelaria Central LTDA")
                .cnpj("11444777000161")
                .build();
    }

    private LicitacaoRequestDTO request() {
        return new LicitacaoRequestDTO("001/2026", ModalidadeLicitacao.PREGAO,
                "Aquisição de material de escritório", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 30), new BigDecimal("80000.00"));
    }

    @Test
    void criarDeveNascerAbertaSemVencedor() {
        when(licitacaoRepository.existsByNumeroEdital("001/2026")).thenReturn(false);
        when(licitacaoRepository.save(any(Licitacao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = licitacaoService.criar(request());

        assertThat(resposta.status()).isEqualTo("ABERTA");
        assertThat(resposta.vencedor()).isNull();
    }

    @Test
    void criarNaoDeveAceitarNumeroDeEditalDuplicado() {
        when(licitacaoRepository.existsByNumeroEdital("001/2026")).thenReturn(true);

        assertThatThrownBy(() -> licitacaoService.criar(request()))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void criarNaoDeveAceitarEncerramentoAnteriorAAbertura() {
        var invalida = new LicitacaoRequestDTO("002/2026", ModalidadeLicitacao.CONCORRENCIA,
                "Objeto qualquer", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1),
                new BigDecimal("1000.00"));

        assertThatThrownBy(() -> licitacaoService.criar(invalida))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void definirVencedorDeveRegistrarVencedorEEncerrarALicitacao() {
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));
        when(fornecedorRepository.findById(10L)).thenReturn(Optional.of(fornecedor));

        var resposta = licitacaoService.definirVencedor(1L, 10L);

        assertThat(resposta.status()).isEqualTo("ENCERRADA");
        assertThat(resposta.vencedor().id()).isEqualTo(10L);
        assertThat(resposta.vencedor().cnpj()).isEqualTo("11444777000161");
    }

    @Test
    void definirVencedorDeveLancarNotFoundParaFornecedorInexistente() {
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));
        when(fornecedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licitacaoService.definirVencedor(1L, 99L))
                .isInstanceOf(NotFoundException.class);

        assertThat(licitacaoAberta.getStatus()).isEqualTo(StatusLicitacao.ABERTA);
    }

    @Test
    void naoDevePermitirDefinirVencedorEmLicitaçãoHomologada() {
        licitacaoAberta.setStatus(StatusLicitacao.HOMOLOGADA);
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));

        assertThatThrownBy(() -> licitacaoService.definirVencedor(1L, 10L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void naoDevePermitirSubstituirVencedorJaDefinidoEmLicitaçãoEncerrada() {
        licitacaoAberta.setStatus(StatusLicitacao.ENCERRADA);
        licitacaoAberta.setVencedor(fornecedor);
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));

        assertThatThrownBy(() -> licitacaoService.definirVencedor(1L, 10L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(fornecedorRepository, never()).findById(any());
    }

    @Test
    void atualizarDeveBloquearLicitaçãoEncerrada() {
        licitacaoAberta.setStatus(StatusLicitacao.ENCERRADA);
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));

        assertThatThrownBy(() -> licitacaoService.atualizar(1L, request()))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void removerDeveBloquearLicitaçãoEncerrada() {
        licitacaoAberta.setStatus(StatusLicitacao.ENCERRADA);
        licitacaoAberta.setVencedor(fornecedor);
        when(licitacaoRepository.findById(1L)).thenReturn(Optional.of(licitacaoAberta));

        assertThatThrownBy(() -> licitacaoService.remover(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(licitacaoRepository, never()).delete(any(Licitacao.class));
    }
}
