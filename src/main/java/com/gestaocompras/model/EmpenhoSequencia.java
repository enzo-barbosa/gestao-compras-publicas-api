package com.gestaocompras.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empenhos_sequencias")
@IdClass(EmpenhoSequencia.EmpenhoSequenciaId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmpenhoSequencia {

    @Id
    @Column(name = "organizacao_id", nullable = false)
    private Long organizacaoId;

    @Id
    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(nullable = false)
    private Integer ultimo;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpenhoSequenciaId implements Serializable {

        private Long organizacaoId;
        private Integer anoReferencia;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EmpenhoSequenciaId that)) {
                return false;
            }
            return Objects.equals(organizacaoId, that.organizacaoId)
                    && Objects.equals(anoReferencia, that.anoReferencia);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organizacaoId, anoReferencia);
        }
    }
}