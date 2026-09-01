package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.FornecedorRequestDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    private static final String CNPJ_VALIDO = "11444777000161";

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private LicitacaoRepository licitacaoRepository;

    @InjectMocks
    private FornecedorService fornecedorService;

    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        fornecedor = Fornecedor.builder()
                .id(1L)
                .nome("Construtora Exemplo LTDA")
                .cnpj(CNPJ_VALIDO)
                .email("contato@exemplo.com.br")
                .telefone("(62) 99999-0000")
                .endereco("Rua das Obras, 100")
                .build();
    }

    private FornecedorRequestDTO request() {
        return new FornecedorRequestDTO("Construtora Exemplo LTDA", "11.444.777/0001-61",
                "contato@exemplo.com.br", "(62) 99999-0000", "Rua das Obras, 100");
    }

    @Test
    void criarDeveNormalizarCnpjRemovendoMascara() {
        when(fornecedorRepository.existsByCnpj(CNPJ_VALIDO)).thenReturn(false);
        when(fornecedorRepository.save(any(Fornecedor.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = fornecedorService.criar(request());

        assertThat(resposta.cnpj()).isEqualTo(CNPJ_VALIDO);
    }

    @Test
    void criarNaoDeveAceitarCnpjComDigitoVerificadorInvalido() {
        var invalido = new FornecedorRequestDTO("Empresa Teste", "11.444.777/0001-00", null, null, null);

        assertThatThrownBy(() -> fornecedorService.criar(invalido))
                .isInstanceOf(IllegalArgumentException.class);

        verify(fornecedorRepository, never()).save(any(Fornecedor.class));
    }

    @Test
    void criarNaoDeveAceitarCnpjDuplicado() {
        when(fornecedorRepository.existsByCnpj(CNPJ_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.criar(request()))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void atualizarNaoDeveUsarCnpjDeOutroFornecedor() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(fornecedorRepository.findByCnpj(CNPJ_VALIDO))
                .thenReturn(Optional.of(Fornecedor.builder().id(2L).cnpj(CNPJ_VALIDO).build()));
        var requisicao = new FornecedorRequestDTO("Outro Nome", "11.444.777/0001-61", null, null, null);

        assertThatThrownBy(() -> fornecedorService.atualizar(1L, requisicao))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void atualizarDeveManterProprioCnpjEAlterarDados() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(fornecedorRepository.findByCnpj(CNPJ_VALIDO)).thenReturn(Optional.of(fornecedor));
        var requisicao = new FornecedorRequestDTO("Razao Social Atualizada", CNPJ_VALIDO,
                "novo@exemplo.com.br", null, null);

        var resposta = fornecedorService.atualizar(1L, requisicao);

        assertThat(resposta.nome()).isEqualTo("Razao Social Atualizada");
        assertThat(resposta.email()).isEqualTo("novo@exemplo.com.br");
        assertThat(resposta.cnpj()).isEqualTo(CNPJ_VALIDO);
    }

    @Test
    void buscarPorIdDeveLancarNotFoundQuandoNaoExistir() {
        when(fornecedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fornecedorService.buscarPorId(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removerDeveBloquearFornecedorComContratoVigente() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(contratoRepository.existsByFornecedorIdAndStatusIn(1L,
                List.of(StatusContrato.VIGENTE))).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.remover(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(fornecedorRepository, never()).delete(any(Fornecedor.class));
    }

    @Test
    void removerDeveBloquearFornecedorQueVenceuLicitacao() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(contratoRepository.existsByFornecedorIdAndStatusIn(1L,
                List.of(StatusContrato.VIGENTE))).thenReturn(false);
        when(licitacaoRepository.existsByVencedorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.remover(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verify(fornecedorRepository, never()).delete(any(Fornecedor.class));
    }

    @Test
    void removerDevePermitirFornecedorSemVinculos() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(contratoRepository.existsByFornecedorIdAndStatusIn(1L,
                List.of(StatusContrato.VIGENTE))).thenReturn(false);
        when(licitacaoRepository.existsByVencedorId(1L)).thenReturn(false);

        fornecedorService.remover(1L);

        verify(fornecedorRepository).delete(fornecedor);
    }
}
