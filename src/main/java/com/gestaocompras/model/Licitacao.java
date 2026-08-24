package com.gestaocompras.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "licitacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Licitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroEdital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModalidadeLicitacao modalidade;

    @Column(nullable = false, length = 300)
    private String objeto;

    @Column(name = "data_abertura", nullable = false)
    private LocalDate dataAbertura;

    @Column(name = "data_encerramento")
    private LocalDate dataEncerramento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusLicitacao status;

    @Column(name = "valor_estimado", precision = 19, scale = 2)
    private BigDecimal valorEstimado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_vencedor_id")
    private Fornecedor vencedor;

    public boolean isEditavel() {
        return status == StatusLicitacao.ABERTA && vencedor == null;
    }
}
