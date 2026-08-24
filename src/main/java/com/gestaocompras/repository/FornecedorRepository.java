package com.gestaocompras.repository;

import com.gestaocompras.model.Fornecedor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    boolean existsByCnpj(String cnpj);

    Optional<Fornecedor> findByCnpj(String cnpj);

    Page<Fornecedor> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
