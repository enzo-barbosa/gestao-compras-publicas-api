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

    public FornecedorService(FornecedorRepository fornecedorRepository,
            ContratoRepository contratoRepository,
            LicitacaoRepository licitacaoRepository) {
        this.fornecedorRepository = fornecedorRepository;
        this.contratoRepository = contratoRepository;
        this.licitacaoRepository = licitacaoRepository;
    }

    @Transactional
    public FornecedorResponseDTO criar(FornecedorRequestDTO request) {
        String cnpj = validarENormalizarCnpj(request.cnpj());
        if (fornecedorRepository.existsByCnpj(cnpj)) {
            throw new RegistroDuplicadoException("Já existe um fornecedor cadastrado com este CNPJ.");
        }
        return FornecedorResponseDTO.from(fornecedorRepository.save(Fornecedor.builder()
                .nome(request.nome())
                .cnpj(cnpj)
                .email(request.email())
                .telefone(request.telefone())
                .endereco(request.endereco())
                .build()));
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponseDTO> listar(String nome, Pageable pageable) {
        Page<Fornecedor> pagina = nome == null || nome.isBlank()
                ? fornecedorRepository.findAll(pageable)
                : fornecedorRepository.findByNomeContainingIgnoreCase(nome, pageable);
        return pagina.map(FornecedorResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long id) {
        return FornecedorResponseDTO.from(buscarEntidade(id));
    }

    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorRequestDTO request) {
        Fornecedor fornecedor = buscarEntidade(id);
        String cnpj = validarENormalizarCnpj(request.cnpj());
        fornecedorRepository.findByCnpj(cnpj)
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
    public void remover(Long id) {
        Fornecedor fornecedor = buscarEntidade(id);
        if (contratoRepository.existsByFornecedorIdAndStatusIn(fornecedor.getId(),
                List.of(StatusContrato.VIGENTE))) {
            throw new OperacaoNaoPermitidaException(
                    "O fornecedor %s possui contratos vigentes e não pode ser removido."
                            .formatted(fornecedor.getNome()));
        }
        if (licitacaoRepository.existsByVencedorId(fornecedor.getId())) {
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

    private Fornecedor buscarEntidade(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fornecedor", id));
    }
}
