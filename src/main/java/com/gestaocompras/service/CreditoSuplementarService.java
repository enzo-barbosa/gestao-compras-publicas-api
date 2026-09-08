package com.gestaocompras.service;

import com.gestaocompras.dto.CreditoSuplementarRequestDTO;
import com.gestaocompras.dto.CreditoSuplementarResponseDTO;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.model.CreditoSuplementar;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.repository.CreditoSuplementarRepository;
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
public class CreditoSuplementarService {

    private final DotacaoService dotacaoService;
    private final CreditoSuplementarRepository creditoSuplementarRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public CreditoSuplementarService(DotacaoService dotacaoService,
            CreditoSuplementarRepository creditoSuplementarRepository,
            OrganizacaoRepository organizacaoRepository) {
        this.dotacaoService = dotacaoService;
        this.creditoSuplementarRepository = creditoSuplementarRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    @Transactional
    public CreditoSuplementarResponseDTO realizar(Long organizacaoId,
            CreditoSuplementarRequestDTO request) {
        if (request.dotacaoOrigemId().equals(request.dotacaoDestinoId())) {
            throw new OperacaoNaoPermitidaException("A dotação de origem e a de destino devem ser diferentes.");
        }
        String descricao = request.descricao() == null || request.descricao().isBlank()
                ? "Crédito suplementar"
                : request.descricao();
        DotacaoOrcamentaria origem = dotacaoService.debitar(
                organizacaoId, request.dotacaoOrigemId(), request.valor(), descricao);
        DotacaoOrcamentaria destino = dotacaoService.creditar(
                organizacaoId, request.dotacaoDestinoId(), request.valor(), descricao);
        CreditoSuplementar registro = creditoSuplementarRepository.save(CreditoSuplementar.builder()
                .dotacaoOrigem(origem)
                .dotacaoDestino(destino)
                .valor(request.valor())
                .descricao(descricao)
                .data(request.data() != null ? request.data() : LocalDate.now())
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .build());
        return CreditoSuplementarResponseDTO.from(registro);
    }

    @Transactional(readOnly = true)
    public Page<CreditoSuplementarResponseDTO> listar(Long organizacaoId, Long dotacaoId,
            LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        return creditoSuplementarRepository
                .findAll(construirFiltro(organizacaoId, dotacaoId, dataInicio, dataFim), pageable)
                .map(CreditoSuplementarResponseDTO::from);
    }

    private Specification<CreditoSuplementar> construirFiltro(Long organizacaoId, Long dotacaoId,
            LocalDate dataInicio, LocalDate dataFim) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("organizacao").get("id"), organizacaoId));
            if (dotacaoId != null) {
                predicados.add(cb.or(
                        cb.equal(root.get("dotacaoOrigem").get("id"), dotacaoId),
                        cb.equal(root.get("dotacaoDestino").get("id"), dotacaoId)));
            }
            if (dataInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("data"), dataInicio));
            }
            if (dataFim != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("data"), dataFim));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}