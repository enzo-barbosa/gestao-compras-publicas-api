package com.gestaocompras.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membros_organizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembroOrganizacao {

    @EmbeddedId
    private Id id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelOrganizacao papel;

    @Column(nullable = false)
    private LocalDateTime desde;

    @Getter
    public static class Id implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "organizacao_id", nullable = false)
        private Organizacao organizacao;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "usuario_id", nullable = false)
        private Usuario usuario;

        public Id() {
        }

        public Id(Organizacao organizacao, Usuario usuario) {
            this.organizacao = organizacao;
            this.usuario = usuario;
        }
    }
}