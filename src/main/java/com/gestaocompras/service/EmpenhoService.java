package com.gestaocompras.service;

import com.gestaocompras.dto.EmpenhoRequestDTO;
import com.gestaocompras.dto.EmpenhoResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.exception.SaldoInsuficienteException;
import com.gestaocompras.model.Contrato;
import com.gestaocompras.model.DotacaoOrcamentaria;
import com.gestaocompras.model.Empenho;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.model.StatusEmpenho;
import com.gestaocompras.model.TipoMovimentacao;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.DotacaoRepository;
import com.gestaocompras.repository.EmpenhoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpenhoService {

    private final EmpenhoRepository empenhoRepository;
    private final ContratoRepository contratoRepository;
    private final DotacaoRepository dotacaoRepository;
    private final DotacaoService dotacaoService;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final Clock clock;

    public EmpenhoService(EmpenhoRepository empenhoRepository,
            ContratoRepository contratoRepository,
            DotacaoRepository dotacaoRepository,
            DotacaoService dotacaoService,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            Clock clock) {
        this.empenhoRepository = empenhoRepository;
        this.contratoRepository = contratoRepository;
        this.dotacaoRepository = dotacaoRepository;
        this.dotacaoService = dotacaoService;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Transactional
    public EmpenhoResponseDTO gerar(Long organizacaoId, EmpenhoRequestDTO request) {
        Integer mes = request.mesReferencia();
        Integer ano = request.anoReferencia();
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("O mês de referência deve estar entre 1 e 12.");
        }
        if (ano == null || ano < 1970) {
            throw new IllegalArgumentException("O ano de referência é inválido.");
        }
        Contrato contrato = contratoRepository.findByIdComLock(request.contratoId(), organizacaoId)
                .orElseThrow(() -> new NotFoundException("Contrato", request.contratoId()));
        if (contrato.getStatus() != StatusContrato.VIGENTE) {
            throw new OperacaoNaoPermitidaException(
                    "O contrato %s está %s e não permite novos empenhos."
                            .formatted(contrato.getNumero(), contrato.getStatus()));
        }
        validarCompetenciaNaVigencia(contrato, mes, ano);
        validarCompetenciaEmitivel(mes, ano);
        validarSequencialidade(contrato, mes, ano);
        if (empenhoRepository.existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                contrato.getId(), ano, mes,
                List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO, StatusEmpenho.PAGO))) {
            throw new RegistroDuplicadoException(
                    "Já existe empenho do contrato %s para a competência %02d/%d."
                            .formatted(contrato.getNumero(), mes, ano));
        }
        BigDecimal valorCompetencia = contrato.calcularValorCompetencia(YearMonth.of(ano, mes));
        DotacaoOrcamentaria dotacao = dotacaoRepository
                .findByIdComLock(contrato.getDotacao().getId(), organizacaoId)
                .orElseThrow(() -> new NotFoundException("Dotação orçamentária",
                        contrato.getDotacao().getId()));
        if (dotacao.getSaldoAtual().compareTo(valorCompetencia) < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente na dotação %s: disponível R$ %s, necessário R$ %s."
                            .formatted(dotacao.getCodigo(), dotacao.getSaldoAtual(),
                                    valorCompetencia));
        }
        if (contrato.getSaldoRestante().compareTo(valorCompetencia) < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo restante insuficiente no contrato %s: disponível R$ %s, necessário R$ %s."
                            .formatted(contrato.getNumero(), contrato.getSaldoRestante(),
                                    valorCompetencia));
        }
        Empenho empenho = empenhoRepository.save(Empenho.builder()
                .contrato(contrato)
                .usuario(usuarioAutenticado())
                .mesReferencia(mes)
                .anoReferencia(ano)
                .valor(valorCompetencia)
                .status(StatusEmpenho.EMPENHADO)
                .dataEmissao(LocalDate.now(clock))
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .build());
        dotacaoService.debitar(organizacaoId, dotacao.getId(), valorCompetencia,
                "Empenho competência %02d/%04d – contrato %s".formatted(mes, ano,
                        contrato.getNumero()));
        contrato.setSaldoRestante(contrato.getSaldoRestante().subtract(valorCompetencia));
        return EmpenhoResponseDTO.from(empenho);
    }

    @Transactional
    public EmpenhoResponseDTO anular(Long organizacaoId, Long id) {
        Empenho empenho = empenhoRepository.findByIdComLock(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Empenho", id));
        if (empenho.getStatus() != StatusEmpenho.EMPENHADO) {
            throw new OperacaoNaoPermitidaException(
                    "O empenho da competência %02d/%04d está %s e não pode ser anulado."
                            .formatted(empenho.getMesReferencia(), empenho.getAnoReferencia(),
                                    empenho.getStatus()));
        }
        Contrato contrato = contratoRepository.findByIdComLock(empenho.getContrato().getId(),
                        organizacaoId)
                .orElseThrow(() -> new NotFoundException("Contrato",
                        empenho.getContrato().getId()));
        BigDecimal valor = empenho.getValor();
        dotacaoService.creditar(organizacaoId, contrato.getDotacao().getId(), valor,
                "Estorno de anulação – empenho competência %02d/%04d – contrato %s"
                        .formatted(empenho.getMesReferencia(), empenho.getAnoReferencia(),
                                contrato.getNumero()), TipoMovimentacao.ESTORNO);
        contrato.setSaldoRestante(contrato.getSaldoRestante().add(valor));
        empenho.setStatus(StatusEmpenho.ANULADO);
        return EmpenhoResponseDTO.from(empenho);
    }

    @Transactional(readOnly = true)
    public Page<EmpenhoResponseDTO> listar(Long organizacaoId, Long contratoId, Long dotacaoId,
            Integer mes, Integer ano, LocalDate dataDe, LocalDate dataAte, Pageable pageable) {
        return empenhoRepository
                .findAll(construirFiltro(organizacaoId, contratoId, dotacaoId, mes, ano,
                        dataDe, dataAte), pageable)
                .map(EmpenhoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public EmpenhoResponseDTO buscarPorId(Long organizacaoId, Long id) {
        return EmpenhoResponseDTO.from(buscarEntidade(organizacaoId, id));
    }

    private Usuario usuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return null;
        }
        return usuarioRepository.findByEmail(autenticacao.getName()).orElse(null);
    }

    private void validarCompetenciaNaVigencia(Contrato contrato, Integer mes, Integer ano) {
        YearMonth competencia = YearMonth.of(ano, mes);
        YearMonth inicio = YearMonth.from(contrato.getDataInicio());
        YearMonth fim = inicio.plusMonths(contrato.getDuracaoMeses() - 1L);
        if (competencia.isBefore(inicio) || competencia.isAfter(fim)) {
            throw new OperacaoNaoPermitidaException(
                    "A competência %02d/%d está fora da vigência do contrato %s (%s a %s)."
                            .formatted(mes, ano, contrato.getNumero(),
                                    inicio, fim));
        }
    }

    private void validarCompetenciaEmitivel(Integer mes, Integer ano) {
        YearMonth competencia = YearMonth.of(ano, mes);
        YearMonth corrente = YearMonth.now(clock);
        YearMonth anterior = corrente.minusMonths(1);
        if (!competencia.equals(corrente) && !competencia.equals(anterior)) {
            throw new OperacaoNaoPermitidaException(
                    "A competência %02d/%d não é elegível para empenho: só é permitida a competência corrente (%s) ou, se pendente, a imediatamente anterior (%s)."
                            .formatted(mes, ano, corrente, anterior));
        }
    }

    private void validarSequencialidade(Contrato contrato, Integer mes, Integer ano) {
        YearMonth competencia = YearMonth.of(ano, mes);
        YearMonth inicio = YearMonth.from(contrato.getDataInicio());
        if (competencia.equals(inicio)) {
            return;
        }
        YearMonth anterior = competencia.minusMonths(1);
        boolean anteriorExiste = empenhoRepository
                .existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(
                        contrato.getId(), anterior.getYear(), anterior.getMonthValue(),
                        List.of(StatusEmpenho.EMPENHADO, StatusEmpenho.LIQUIDADO,
                                StatusEmpenho.PAGO));
        if (!anteriorExiste) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível empenhar a competência %02d/%d sem que a competência %02d/%d esteja empenhada."
                            .formatted(mes, ano, anterior.getMonthValue(), anterior.getYear()));
        }
    }

    private Specification<Empenho> construirFiltro(Long organizacaoId, Long contratoId,
            Long dotacaoId, Integer mes, Integer ano, LocalDate dataDe, LocalDate dataAte) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("organizacao").get("id"), organizacaoId));
            if (contratoId != null) {
                predicados.add(cb.equal(root.get("contrato").get("id"), contratoId));
            }
            if (dotacaoId != null) {
                predicados.add(cb.equal(root.get("contrato").get("dotacao").get("id"), dotacaoId));
            }
            if (mes != null) {
                predicados.add(cb.equal(root.get("mesReferencia"), mes));
            }
            if (ano != null) {
                predicados.add(cb.equal(root.get("anoReferencia"), ano));
            }
            if (dataDe != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("dataEmissao"), dataDe));
            }
            if (dataAte != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("dataEmissao"), dataAte));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private Empenho buscarEntidade(Long organizacaoId, Long id) {
        return empenhoRepository.findByIdAndOrganizacaoId(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Empenho", id));
    }
}