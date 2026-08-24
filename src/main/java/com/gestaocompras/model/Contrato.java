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
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contratos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(nullable = false, length = 300)
    private String objeto;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "duracao_meses", nullable = false)
    private Integer duracaoMeses;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContrato status;

    @Column(name = "saldo_restante", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoRestante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dotacao_id", nullable = false)
    private DotacaoOrcamentaria dotacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licitacao_id")
    private Licitacao licitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    public BigDecimal calcularValorMensal() {
        return valorTotal.divide(BigDecimal.valueOf(duracaoMeses), 2, RoundingMode.HALF_UP);
    }

    public LocalDate calcularDataFimPrevista() {
        return dataInicio.plusMonths(duracaoMeses).minusDays(1);
    }
}
