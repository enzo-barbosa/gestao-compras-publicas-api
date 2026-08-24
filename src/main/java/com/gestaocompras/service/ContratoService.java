package com.gestaocompras.service;

import com.gestaocompras.dto.ContratoRequestDTO;
import com.gestaocompras.dto.ContratoResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Contrato;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.Licitacao;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.model.StatusLicitacao;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final DotacaoRepository dotacaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final LicitacaoRepository licitacaoRepository;

    public ContratoService(ContratoRepository contratoRepository,
            DotacaoRepository dotacaoRepository,
            FornecedorRepository fornecedorRepository,
            LicitacaoRepository licitacaoRepository) {
        this.contratoRepository = contratoRepository;
        this.dotacaoRepository = dotacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.licitacaoRepository = licitacaoRepository;
    }

    @Transactional
    public ContratoResponseDTO criar(ContratoRequestDTO request) {
        validarCamposFinanceiros(request.valorTotal(), request.duracaoMeses());
        if (contratoRepository.existsByNumero(request.numero())) {
            throw new RegistroDuplicadoException(
                    "Já existe um contrato com o número %s.".formatted(request.numero()));
        }
        DotacaoOrcamentaria dotacao = dotacaoRepository.findById(request.dotacaoId())
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária", request.dotacaoId()));
        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
                .orElseThrow(() -> new NotFoundException("Fornecedor", request.fornecedorId()));
        Licitacao licitacao = null;
        if (request.licitacaoId() != null) {
            licitacao = licitacaoRepository.findById(request.licitacaoId())
                    .orElseThrow(() -> new NotFoundException("Licitação", request.licitacaoId()));
            validarVinculoLicitacao(licitacao, fornecedor);
        }
        return ContratoResponseDTO.from(contratoRepository.save(Contrato.builder()
                .numero(request.numero())
                .objeto(request.objeto())
                .valorTotal(request.valorTotal())
                .duracaoMeses(request.duracaoMeses())
                .dataInicio(request.dataInicio())
                .status(StatusContrato.VIGENTE)
                .saldoRestante(request.valorTotal())
                .dotacao(dotacao)
                .licitacao(licitacao)
                .fornecedor(fornecedor)
                .build()));
    }

    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> listar(Long dotacaoId, Long fornecedorId,
            StatusContrato status, Pageable pageable) {
        return contratoRepository.findAll(construirFiltro(dotacaoId, fornecedorId, status), pageable)
                .map(ContratoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Long id) {
        return ContratoResponseDTO.from(buscarEntidade(id));
    }

    @Transactional
    public ContratoResponseDTO atualizar(Long id, ContratoRequestDTO request) {
        Contrato contrato = buscarEntidade(id);
        validarCamposFinanceiros(request.valorTotal(), request.duracaoMeses());
        if (request.valorTotal().compareTo(contrato.getValorTotal()) != 0
                || !request.duracaoMeses().equals(contrato.getDuracaoMeses())) {
            throw new IllegalArgumentException(
                    "O valor total e a duração não podem ser alterados após a criação do contrato.");
        }
        if (!contrato.getDotacao().getId().equals(request.dotacaoId())) {
            throw new IllegalArgumentException(
                    "A dotação orçamentária vinculada não pode ser alterada após a criação do contrato.");
        }
        if (!contrato.getFornecedor().getId().equals(request.fornecedorId())) {
            throw new IllegalArgumentException(
                    "O fornecedor do contrato não pode ser alterado após a sua criação.");
        }
        if (!Objects.equals(idVinculoLicitacao(contrato), request.licitacaoId())) {
            throw new IllegalArgumentException(
                    "A licitação vinculada não pode ser alterada após a criação do contrato.");
        }
        contratoRepository.findByNumero(request.numero())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegistroDuplicadoException(
                            "Já existe um contrato com o número %s.".formatted(request.numero()));
                });
        contrato.setNumero(request.numero());
        contrato.setObjeto(request.objeto());
        contrato.setDataInicio(request.dataInicio());
        return ContratoResponseDTO.from(contrato);
    }

    @Transactional
    public void remover(Long id) {
        contratoRepository.delete(buscarEntidade(id));
    }

    private void validarCamposFinanceiros(BigDecimal valorTotal, Integer duracaoMeses) {
        if (valorTotal == null || valorTotal.signum() <= 0) {
            throw new IllegalArgumentException("O valor total deve ser positivo.");
        }
        if (duracaoMeses == null || duracaoMeses < 1) {
            throw new IllegalArgumentException("A duração deve ser de pelo menos 1 mês.");
        }
    }

    private void validarVinculoLicitacao(Licitacao licitacao, Fornecedor fornecedor) {
        String numeroEdital = licitacao.getNumeroEdital();
        if (licitacao.getStatus() != StatusLicitacao.ENCERRADA
                && licitacao.getStatus() != StatusLicitacao.HOMOLOGADA) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s está %s; o contrato exige uma licitação encerrada ou homologada."
                            .formatted(numeroEdital, licitacao.getStatus()));
        }
        if (licitacao.getVencedor() == null
                || !licitacao.getVencedor().getId().equals(fornecedor.getId())) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s não foi vencida pelo fornecedor informado."
                            .formatted(numeroEdital));
        }
    }

    private Specification<Contrato> construirFiltro(Long dotacaoId, Long fornecedorId,
            StatusContrato status) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (dotacaoId != null) {
                predicados.add(cb.equal(root.get("dotacao").get("id"), dotacaoId));
            }
            if (fornecedorId != null) {
                predicados.add(cb.equal(root.get("fornecedor").get("id"), fornecedorId));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private Long idVinculoLicitacao(Contrato contrato) {
        return contrato.getLicitacao() == null ? null : contrato.getLicitacao().getId();
    }

    private Contrato buscarEntidade(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contrato", id));
    }
}
