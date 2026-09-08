package com.gestaocompras.service;

import com.gestaocompras.dto.FornecedorRequestDTO;
import com.gestaocompras.dto.FornecedorResponseDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.exception.RegistroDuplicadoException;
import com.gestaocompras.model.Fornecedor;
import com.gestaocompras.model.StatusContrato;
import com.gestaocompras.repository.ContratoRepository;
import com.gestaocompras.repository.FornecedorRepository;
import com.gestaocompras.repository.LicitacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.util.CnpjUtil;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final ContratoRepository contratoRepository;
    private final LicitacaoRepository licitacaoRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository,
            ContratoRepository contratoRepository,
            LicitacaoRepository licitacaoRepository,
            OrganizacaoRepository organizacaoRepository) {
        this.fornecedorRepository = fornecedorRepository;
        this.contratoRepository = contratoRepository;
        this.licitacaoRepository = licitacaoRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    @Transactional
    public FornecedorResponseDTO criar(Long organizacaoId, FornecedorRequestDTO request) {
        String cnpj = validarENormalizarCnpj(request.cnpj());
        if (fornecedorRepository.existsByCnpjAndOrganizacaoId(cnpj, organizacaoId)) {
            throw new RegistroDuplicadoException("Já existe um fornecedor cadastrado com este CNPJ.");
        }
        return FornecedorResponseDTO.from(fornecedorRepository.save(Fornecedor.builder()
                .nome(request.nome())
                .cnpj(cnpj)
                .email(request.email())
                .telefone(request.telefone())
                .endereco(request.endereco())
                .organizacao(organizacaoRepository.getReferenceById(organizacaoId))
                .build()));
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponseDTO> listar(Long organizacaoId, String nome, Pageable pageable) {
        Page<Fornecedor> pagina = nome == null || nome.isBlank()
                ? fornecedorRepository.findByOrganizacaoId(organizacaoId, pageable)
                : fornecedorRepository.findByNomeContainingIgnoreCaseAndOrganizacaoId(
                        nome, organizacaoId, pageable);
        return pagina.map(FornecedorResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long organizacaoId, Long id) {
        return FornecedorResponseDTO.from(buscarEntidade(organizacaoId, id));
    }

    @Transactional
    public FornecedorResponseDTO atualizar(Long organizacaoId, Long id,
            FornecedorRequestDTO request) {
        Fornecedor fornecedor = buscarEntidade(organizacaoId, id);
        String cnpj = validarENormalizarCnpj(request.cnpj());
        fornecedorRepository.findByCnpjAndOrganizacaoId(cnpj, organizacaoId)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegistroDuplicadoException(
                            "Já existe um fornecedor cadastrado com este CNPJ.");
                });
        fornecedor.setNome(request.nome());
        fornecedor.setCnpj(cnpj);
        fornecedor.setEmail(request.email());
        fornecedor.setTelefone(request.telefone());
        fornecedor.setEndereco(request.endereco());
        return FornecedorResponseDTO.from(fornecedor);
    }

    @Transactional
    public void remover(Long organizacaoId, Long id) {
        Fornecedor fornecedor = buscarEntidade(organizacaoId, id);
        if (contratoRepository.existsByFornecedorIdAndOrganizacaoIdAndStatusIn(
                fornecedor.getId(), organizacaoId, List.of(StatusContrato.VIGENTE))) {
            throw new OperacaoNaoPermitidaException(
                    "O fornecedor %s possui contratos vigentes e não pode ser removido."
                            .formatted(fornecedor.getNome()));
        }
        if (licitacaoRepository.existsByVencedorIdAndOrganizacaoId(
                fornecedor.getId(), organizacaoId)) {
            throw new OperacaoNaoPermitidaException(
                    "O fornecedor %s venceu licitações e não pode ser removido."
                            .formatted(fornecedor.getNome()));
        }
        fornecedorRepository.delete(fornecedor);
    }

    private String validarENormalizarCnpj(String cnpj) {
        String digitos = CnpjUtil.limpar(cnpj);
        if (!CnpjUtil.valido(digitos)) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }
        return digitos;
    }

    private Fornecedor buscarEntidade(Long organizacaoId, Long id) {
        return fornecedorRepository.findByIdAndOrganizacaoId(id, organizacaoId)
                .orElseThrow(() -> new NotFoundException("Fornecedor", id));
    }
}