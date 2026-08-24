package com.gestaocompras.repository;

import com.gestaocompras.model.Contrato;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContratoRepository
        extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {

    boolean existsByNumero(String numero);

    Optional<Contrato> findByNumero(String numero);
}
