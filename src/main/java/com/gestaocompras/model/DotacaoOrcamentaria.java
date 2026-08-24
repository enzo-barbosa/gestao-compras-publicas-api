package com.gestaocompras.model;

import com.gestaocompras.exception.SaldoInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dotacoes_orcamentarias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DotacaoOrcamentaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoInicial;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoAtual;

    @Column(name = "ano_exercicio", nullable = false)
    private Integer anoExercicio;

    @Builder.Default
    @OneToMany(mappedBy = "dotacao")
    private List<MovimentacaoDotacao> movimentacoes = new ArrayList<>();

    public void debitar(BigDecimal valor) {
        exigirValorPositivo(valor);
        if (saldoAtual.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Dotação orçamentária", saldoAtual, valor);
        }
        saldoAtual = saldoAtual.subtract(valor);
    }

    public void creditar(BigDecimal valor) {
        exigirValorPositivo(valor);
        saldoAtual = saldoAtual.add(valor);
    }

    public void adicionarMovimentacao(MovimentacaoDotacao movimentacao) {
        movimentacoes.add(movimentacao);
        movimentacao.setDotacao(this);
    }

    private void exigirValorPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da movimentação deve ser positivo.");
        }
    }
}
