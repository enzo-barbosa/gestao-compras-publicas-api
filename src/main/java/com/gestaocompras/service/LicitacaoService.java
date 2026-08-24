package com.gestaocompras.service;

import com.gestaocompras.dto.LicitacaoRequestDTO;
import com.gestaocompras.dto.LicitacaoResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.Licitacao;
import com.gestaocompras.model.ModalidadeLicitacao;
import com.gestaocompras.model.StatusLicitacao;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicitacaoService {

    private final LicitacaoRepository licitacaoRepository;
    private final FornecedorRepository fornecedorRepository;

    public LicitacaoService(LicitacaoRepository licitacaoRepository,
            FornecedorRepository fornecedorRepository) {
        this.licitacaoRepository = licitacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public LicitacaoResponseDTO criar(LicitacaoRequestDTO request) {
        validarDatas(request.dataAbertura(), request.dataEncerramento());
        if (licitacaoRepository.existsByNumeroEdital(request.numeroEdital())) {
            throw new RegistroDuplicadoException(
                    "Já existe uma licitação com o edital %s.".formatted(request.numeroEdital()));
        }
        return LicitacaoResponseDTO.from(licitacaoRepository.save(Licitacao.builder()
                .numeroEdital(request.numeroEdital())
                .modalidade(request.modalidade())
                .objeto(request.objeto())
                .dataAbertura(request.dataAbertura())
                .dataEncerramento(request.dataEncerramento())
                .status(StatusLicitacao.ABERTA)
                .valorEstimado(request.valorEstimado())
                .build()));
    }

    @Transactional(readOnly = true)
    public Page<LicitacaoResponseDTO> listar(StatusLicitacao status, ModalidadeLicitacao modalidade,
            Pageable pageable) {
        return licitacaoRepository.findAll(construirFiltro(status, modalidade), pageable)
                .map(LicitacaoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public LicitacaoResponseDTO buscarPorId(Long id) {
        return LicitacaoResponseDTO.from(buscarEntidade(id));
    }

    @Transactional
    public LicitacaoResponseDTO atualizar(Long id, LicitacaoRequestDTO request) {
        Licitacao licitacao = buscarEntidade(id);
        exigirEditavel(licitacao, "alterada");
        validarDatas(request.dataAbertura(), request.dataEncerramento());
        licitacaoRepository.findByNumeroEdital(request.numeroEdital())
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(outra -> {
                    throw new RegistroDuplicadoException(
                            "Já existe uma licitação com o edital %s.".formatted(request.numeroEdital()));
                });
        licitacao.setNumeroEdital(request.numeroEdital());
        licitacao.setModalidade(request.modalidade());
        licitacao.setObjeto(request.objeto());
        licitacao.setDataAbertura(request.dataAbertura());
        licitacao.setDataEncerramento(request.dataEncerramento());
        licitacao.setValorEstimado(request.valorEstimado());
        return LicitacaoResponseDTO.from(licitacao);
    }

    @Transactional
    public void remover(Long id) {
        Licitacao licitacao = buscarEntidade(id);
        exigirEditavel(licitacao, "removida");
        licitacaoRepository.delete(licitacao);
    }

    @Transactional
    public LicitacaoResponseDTO definirVencedor(Long id, Long fornecedorId) {
        Licitacao licitacao = buscarEntidade(id);
        if (licitacao.getStatus() == StatusLicitacao.HOMOLOGADA
                || licitacao.getStatus() == StatusLicitacao.CANCELADA) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s está %s e não permite definir vencedor."
                            .formatted(licitacao.getNumeroEdital(), licitacao.getStatus()));
        }
        Fornecedor vencedor = fornecedorRepository.findById(fornecedorId)
                .orElseThrow(() -> new NotFoundException("Fornecedor", fornecedorId));
        licitacao.setVencedor(vencedor);
        licitacao.setStatus(StatusLicitacao.ENCERRADA);
        return LicitacaoResponseDTO.from(licitacao);
    }

    private void validarDatas(LocalDate abertura, LocalDate encerramento) {
        if (encerramento != null && encerramento.isBefore(abertura)) {
            throw new IllegalArgumentException(
                    "A data de encerramento não pode ser anterior à data de abertura.");
        }
    }

    private void exigirEditavel(Licitacao licitacao, String acao) {
        if (!licitacao.isEditavel()) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s está %s e não pode ser %s."
                            .formatted(licitacao.getNumeroEdital(), licitacao.getStatus(), acao));
        }
    }

    private Specification<Licitacao> construirFiltro(StatusLicitacao status,
            ModalidadeLicitacao modalidade) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (modalidade != null) {
                predicados.add(cb.equal(root.get("modalidade"), modalidade));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private Licitacao buscarEntidade(Long id) {
        return licitacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Licitação", id));
    }
}
