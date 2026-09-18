package com.gestaocompras.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean habilitado = true;
    private int maximoChaves = 10000;
    private Map<String, Limite> limites = new LinkedHashMap<>();

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public int getMaximoChaves() {
        return maximoChaves;
    }

    public void setMaximoChaves(int maximoChaves) {
        this.maximoChaves = maximoChaves;
    }

    public Map<String, Limite> getLimites() {
        return limites;
    }

    public void setLimites(Map<String, Limite> limites) {
        this.limites = limites;
    }

    public static class Limite {

        private int requisicoes;
        private long janelaSegundos = 60;

        public int getRequisicoes() {
            return requisicoes;
        }

        public void setRequisicoes(int requisicoes) {
            this.requisicoes = requisicoes;
        }

        public long getJanelaSegundos() {
            return janelaSegundos;
        }

        public void setJanelaSegundos(long janelaSegundos) {
            this.janelaSegundos = janelaSegundos;
        }
    }
}
