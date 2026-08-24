package com.gestaocompras.service;

import com.gestaocompras.dto.CreditoSuplementarRequestDTO;
import com.gestaocompras.dto.CreditoSuplementarResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.model.CreditoSuplementar;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.repository.CreditoSuplementarRepository;
import com.gestaocompras.repository.DotacaoRepository;
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
    private final DotacaoRepository dotacaoRepository;
    private final CreditoSuplementarRepository creditoSuplementarRepository;

    public CreditoSuplementarService(DotacaoService dotacaoService,
            DotacaoRepository dotacaoRepository,
            CreditoSuplementarRepository creditoSuplementarRepository) {
        this.dotacaoService = dotacaoService;
        this.dotacaoRepository = dotacaoRepository;
        this.creditoSuplementarRepository = creditoSuplementarRepository;
    }

    @Transactional
    public CreditoSuplementarResponseDTO realizar(CreditoSuplementarRequestDTO request) {
        if (request.dotacaoOrigemId().equals(request.dotacaoDestinoId())) {
            throw new OperacaoNaoPermitidaException("A dotação de origem e a de destino devem ser diferentes.");
        }
        DotacaoOrcamentaria origem = buscarDotacao(request.dotacaoOrigemId());
        DotacaoOrcamentaria destino = buscarDotacao(request.dotacaoDestinoId());
        String descricao = request.descricao() == null || request.descricao().isBlank()
                ? "Crédito suplementar"
                : request.descricao();
        dotacaoService.debitar(origem.getId(), request.valor(), descricao);
        dotacaoService.creditar(destino.getId(), request.valor(), descricao);
        CreditoSuplementar registro = creditoSuplementarRepository.save(CreditoSuplementar.builder()
                .dotacaoOrigem(origem)
                .dotacaoDestino(destino)
                .valor(request.valor())
                .descricao(descricao)
                .data(request.data() != null ? request.data() : LocalDate.now())
                .build());
        return CreditoSuplementarResponseDTO.from(registro);
    }

    @Transactional(readOnly = true)
    public Page<CreditoSuplementarResponseDTO> listar(Long dotacaoId, LocalDate dataInicio,
            LocalDate dataFim, Pageable pageable) {
        return creditoSuplementarRepository
                .findAll(construirFiltro(dotacaoId, dataInicio, dataFim), pageable)
                .map(CreditoSuplementarResponseDTO::from);
    }

    private Specification<CreditoSuplementar> construirFiltro(Long dotacaoId, LocalDate dataInicio,
            LocalDate dataFim) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
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

    private DotacaoOrcamentaria buscarDotacao(Long id) {
        return dotacaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária", id));
    }
}
