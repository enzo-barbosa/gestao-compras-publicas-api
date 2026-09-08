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
import com.gestaocompras.repository.OrganizacaoRepository;
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
    private final OrganizacaoRepository organizacaoRepository;

    public LicitacaoService(LicitacaoRepository licitacaoRepository,
            FornecedorRepository fornecedorRepository,
            OrganizacaoRepository organizacaoRepository) {
        this.licitacaoRepository = licitacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    @Transactional
    public LicitacaoResponseDTO criar(Long organizacaoId, LicitacaoRequestDTO request) {
        validarDatas(request.dataAbertura(), request.dataEncerramento());
        if (licitacaoRepository.existsByNumeroEditalAndOrganizacaoId(
                request.numeroEdital(), organizacaoId)) {
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
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .build()));
    }

    @Transactional(readOnly = true)
    public Page<LicitacaoResponseDTO> listar(Long organizacaoId, StatusLicitacao status,
            ModalidadeLicitacao modalidade, Pageable pageable) {
        return licitacaoRepository
                .findAll(construirFiltro(organizacaoId, status, modalidade), pageable)
                .map(LicitacaoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public LicitacaoResponseDTO buscarPorId(Long organizacaoId, Long id) {
        return LicitacaoResponseDTO.from(buscarEntidade(organizacaoId, id));
    }

    @Transactional
    public LicitacaoResponseDTO atualizar(Long organizacaoId, Long id,
            LicitacaoRequestDTO request) {
        Licitacao licitacao = buscarEntidade(organizacaoId, id);
        exigirEditavel(licitacao, "alterada");
        validarDatas(request.dataAbertura(), request.dataEncerramento());
        licitacaoRepository.findByNumeroEditalAndOrganizacaoId(
                        request.numeroEdital(), organizacaoId)
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
    public void remover(Long organizacaoId, Long id) {
        Licitacao licitacao = buscarEntidade(organizacaoId, id);
        exigirEditavel(licitacao, "removida");
        licitacaoRepository.delete(licitacao);
    }

    @Transactional
    public LicitacaoResponseDTO definirVencedor(Long organizacaoId, Long id, Long fornecedorId) {
        Licitacao licitacao = licitacaoRepository.findByIdComLock(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Licitação", id));
        if (licitacao.getStatus() == StatusLicitacao.HOMOLOGADA
                || licitacao.getStatus() == StatusLicitacao.CANCELADA) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s está %s e não permite definir vencedor."
                            .formatted(licitacao.getNumeroEdital(), licitacao.getStatus()));
        }
        if (licitacao.getVencedor() != null && licitacao.getStatus() == StatusLicitacao.ENCERRADA) {
            throw new OperacaoNaoPermitidaException(
                    "A licitação %s já possui vencedor definido e não pode ser substituído."
                            .formatted(licitacao.getNumeroEdital()));
        }
        Fornecedor vencedor = fornecedorRepository.findByIdAndOrganizacaoId(fornecedorId,
                        organizacaoId)
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

    private Specification<Licitacao> construirFiltro(Long organizacaoId, StatusLicitacao status,
            ModalidadeLicitacao modalidade) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("organizacao").get("id"), organizacaoId));
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (modalidade != null) {
                predicados.add(cb.equal(root.get("modalidade"), modalidade));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private Licitacao buscarEntidade(Long organizacaoId, Long id) {
        return licitacaoRepository.findByIdAndOrganizacaoId(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Licitação", id));
    }
}