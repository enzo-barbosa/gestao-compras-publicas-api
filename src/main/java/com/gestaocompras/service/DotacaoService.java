package com.gestaocompras.service;

import com.gestaocompras.dto.DotacaoRequestDTO;
import com.gestaocompras.dto.DotacaoResponseDTO;
import com.gestaocompras.dto.MovimentacaoResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.MovimentacaoDotacao;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.MovimentacaoDotacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DotacaoService {

    private final DotacaoRepository dotacaoRepository;
    private final MovimentacaoDotacaoRepository movimentacaoRepository;

    public DotacaoService(DotacaoRepository dotacaoRepository,
            MovimentacaoDotacaoRepository movimentacaoRepository) {
        this.dotacaoRepository = dotacaoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public DotacaoResponseDTO criar(DotacaoRequestDTO request) {
        validarCodigoDisponivel(request.codigo(), null);
        DotacaoOrcamentaria dotacao = dotacaoRepository.save(DotacaoOrcamentaria.builder()
                .codigo(request.codigo())
                .descricao(request.descricao())
                .saldoInicial(request.saldoInicial())
                .saldoAtual(request.saldoInicial())
                .anoExercicio(request.anoExercicio())
                .build());
        registrarMovimentacao(dotacao, TipoMovimentacao.CREDITO, request.saldoInicial(), "Saldo inicial");
        return DotacaoResponseDTO.from(dotacao);
    }

    @Transactional(readOnly = true)
    public Page<DotacaoResponseDTO> listar(Integer anoExercicio, Pageable pageable) {
        Page<DotacaoOrcamentaria> pagina = anoExercicio != null
                ? dotacaoRepository.findByAnoExercicio(anoExercicio, pageable)
                : dotacaoRepository.findAll(pageable);
        return pagina.map(DotacaoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public DotacaoResponseDTO buscarPorId(Long id) {
        return DotacaoResponseDTO.from(buscarEntidade(id));
    }

    @Transactional
    public DotacaoResponseDTO atualizar(Long id, DotacaoRequestDTO request) {
        DotacaoOrcamentaria dotacao = buscarEntidade(id);
        validarCodigoDisponivel(request.codigo(), id);
        if (dotacao.getSaldoInicial().compareTo(request.saldoInicial()) != 0) {
            throw new IllegalArgumentException(
                    "O saldo inicial não pode ser alterado após a criação da dotação.");
        }
        dotacao.setCodigo(request.codigo());
        dotacao.setDescricao(request.descricao());
        dotacao.setAnoExercicio(request.anoExercicio());
        return DotacaoResponseDTO.from(dotacao);
    }

    @Transactional
    public void remover(Long id) {
        DotacaoOrcamentaria dotacao = buscarEntidade(id);
        if (movimentacaoRepository.countByDotacaoId(id) > 1) {
            throw new OperacaoNaoPermitidaException(
                    "A dotação %s possui movimentações registradas e não pode ser removida."
                            .formatted(dotacao.getCodigo()));
        }
        movimentacaoRepository.deleteAll(movimentacaoRepository.findAllByDotacaoId(id));
        dotacaoRepository.delete(dotacao);
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(Long id) {
        return buscarEntidade(id).getSaldoAtual();
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponseDTO> listarMovimentacoes(Long id, Pageable pageable) {
        buscarEntidade(id);
        return movimentacaoRepository.findByDotacaoId(id, pageable).map(MovimentacaoResponseDTO::from);
    }

    @Transactional
    public void debitar(Long dotacaoId, BigDecimal valor, String descricao) {
        DotacaoOrcamentaria dotacao = buscarEntidade(dotacaoId);
        dotacao.debitar(valor);
        registrarMovimentacao(dotacao, TipoMovimentacao.DEBITO, valor, descricao);
    }

    @Transactional
    public void creditar(Long dotacaoId, BigDecimal valor, String descricao) {
        creditar(dotacaoId, valor, descricao, TipoMovimentacao.CREDITO_SUPLEMENTAR);
    }

    @Transactional
    public void creditar(Long dotacaoId, BigDecimal valor, String descricao,
            TipoMovimentacao tipo) {
        DotacaoOrcamentaria dotacao = buscarEntidade(dotacaoId);
        dotacao.creditar(valor);
        registrarMovimentacao(dotacao, tipo, valor, descricao);
    }

    private void registrarMovimentacao(DotacaoOrcamentaria dotacao, TipoMovimentacao tipo,
            BigDecimal valor, String descricao) {
        movimentacaoRepository.save(MovimentacaoDotacao.builder()
                .dotacao(dotacao)
                .tipo(tipo)
                .valor(valor)
                .descricao(descricao)
                .dataHora(LocalDateTime.now())
                .build());
    }

    private DotacaoOrcamentaria buscarEntidade(Long id) {
        return dotacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária", id));
    }

    private void validarCodigoDisponivel(String codigo, Long idAtual) {
        boolean duplicado = idAtual == null
                ? dotacaoRepository.existsByCodigo(codigo)
                : dotacaoRepository.findByCodigo(codigo)
                        .filter(encontrada -> !encontrada.getId().equals(idAtual))
                        .isPresent();
        if (duplicado) {
            throw new RegistroDuplicadoException("Já existe uma dotação com o código %s.".formatted(codigo));
        }
    }
}
