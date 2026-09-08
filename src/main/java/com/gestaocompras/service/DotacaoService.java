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
import com.gestaocompras.repository.OrganizacaoRepository;
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
    private final OrganizacaoRepository organizacaoRepository;

    public DotacaoService(DotacaoRepository dotacaoRepository,
            MovimentacaoDotacaoRepository movimentacaoRepository,
            OrganizacaoRepository organizacaoRepository) {
        this.dotacaoRepository = dotacaoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    @Transactional
    public DotacaoResponseDTO criar(Long organizacaoId, DotacaoRequestDTO request) {
        validarCodigoDisponivel(organizacaoId, request.codigo(), null);
        DotacaoOrcamentaria dotacao = dotacaoRepository.save(DotacaoOrcamentaria.builder()
                .codigo(request.codigo())
                .descricao(request.descricao())
                .saldoInicial(request.saldoInicial())
                .saldoAtual(request.saldoInicial())
                .anoExercicio(request.anoExercicio())
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .build());
        registrarMovimentacao(dotacao, TipoMovimentacao.CREDITO, request.saldoInicial(), "Saldo inicial");
        return DotacaoResponseDTO.from(dotacao);
    }

    @Transactional(readOnly = true)
    public Page<DotacaoResponseDTO> listar(Long organizacaoId, Integer anoExercicio,
            Pageable pageable) {
        Page<DotacaoOrcamentaria> pagina = anoExercicio != null
                ? dotacaoRepository.findByAnoExercicioAndOrganizacaoId(
                        anoExercicio, organizacaoId, pageable)
                : dotacaoRepository.findByOrganizacaoId(organizacaoId, pageable);
        return pagina.map(DotacaoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public DotacaoResponseDTO buscarPorId(Long organizacaoId, Long id) {
        return DotacaoResponseDTO.from(buscarEntidade(organizacaoId, id));
    }

    @Transactional
    public DotacaoResponseDTO atualizar(Long organizacaoId, Long id, DotacaoRequestDTO request) {
        DotacaoOrcamentaria dotacao = buscarEntidade(organizacaoId, id);
        validarCodigoDisponivel(organizacaoId, request.codigo(), id);
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
    public void remover(Long organizacaoId, Long id) {
        DotacaoOrcamentaria dotacao = buscarEntidade(organizacaoId, id);
        if (movimentacaoRepository.countByDotacaoId(id) > 1) {
            throw new OperacaoNaoPermitidaException(
                    "A dotação %s possui movimentações registradas e não pode ser removida."
                            .formatted(dotacao.getCodigo()));
        }
        movimentacaoRepository.deleteAll(movimentacaoRepository.findAllByDotacaoId(id));
        dotacaoRepository.delete(dotacao);
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(Long organizacaoId, Long id) {
        return buscarEntidade(organizacaoId, id).getSaldoAtual();
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponseDTO> listarMovimentacoes(Long organizacaoId, Long id,
            Pageable pageable) {
        buscarEntidade(organizacaoId, id);
        return movimentacaoRepository.findByDotacaoId(id, pageable).map(MovimentacaoResponseDTO::from);
    }

    @Transactional
    public DotacaoOrcamentaria debitar(Long organizacaoId, Long dotacaoId, BigDecimal valor,
            String descricao) {
        DotacaoOrcamentaria dotacao = buscarEntidadeComLock(organizacaoId, dotacaoId);
        dotacao.debitar(valor);
        registrarMovimentacao(dotacao, TipoMovimentacao.DEBITO, valor, descricao);
        return dotacao;
    }

    @Transactional
    public DotacaoOrcamentaria creditar(Long organizacaoId, Long dotacaoId, BigDecimal valor,
            String descricao) {
        return creditar(organizacaoId, dotacaoId, valor, descricao,
                TipoMovimentacao.CREDITO_SUPLEMENTAR);
    }

    @Transactional
    public DotacaoOrcamentaria creditar(Long organizacaoId, Long dotacaoId, BigDecimal valor,
            String descricao, TipoMovimentacao tipo) {
        DotacaoOrcamentaria dotacao = buscarEntidadeComLock(organizacaoId, dotacaoId);
        dotacao.creditar(valor);
        registrarMovimentacao(dotacao, tipo, valor, descricao);
        return dotacao;
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

    private DotacaoOrcamentaria buscarEntidade(Long organizacaoId, Long id) {
        return dotacaoRepository.findByIdAndOrganizacaoId(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária", id));
    }

    private DotacaoOrcamentaria buscarEntidadeComLock(Long organizacaoId, Long id) {
        return dotacaoRepository.findByIdComLock(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária", id));
    }

    private void validarCodigoDisponivel(Long organizacaoId, String codigo, Long idAtual) {
        boolean duplicado = idAtual == null
                ? dotacaoRepository.existsByCodigoAndOrganizacaoId(codigo, organizacaoId)
                : dotacaoRepository.findByCodigoAndOrganizacaoId(codigo, organizacaoId)
                        .filter(encontrada -> !encontrada.getId().equals(idAtual))
                        .isPresent();
        if (duplicado) {
            throw new RegistroDuplicadoException("Já existe uma dotação com o código %s.".formatted(codigo));
        }
    }
}