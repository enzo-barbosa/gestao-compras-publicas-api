package com.gestaocompras.service;

import com.gestaocompras.config.RateLimitProperties;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    public record Decisao(boolean permitido, long retryAfterSegundos) {

        static Decisao liberado() {
            return new Decisao(true, 0);
        }
    }

    private record Contador(long inicioMillis, int requisicoes) {
    }

    private final Clock clock;
    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();
    private final int maximoChaves;
    private final long janelaMaximaMillis;

    public RateLimitService(Clock clock, RateLimitProperties properties) {
        this.clock = clock;
        this.maximoChaves = properties.getMaximoChaves();
        this.janelaMaximaMillis = properties.getLimites().values().stream()
                .mapToLong(RateLimitProperties.Limite::getJanelaSegundos)
                .max()
                .orElse(3600L) * 1000L;
    }

    public Decisao verificar(String chave, int limite, long janelaSegundos) {
        if (limite <= 0 || janelaSegundos <= 0) {
            return Decisao.liberado();
        }
        long janelaMillis = janelaSegundos * 1000L;
        long agora = clock.millis();
        Contador contador = contadores.compute(chave, (ignorada, atual) -> {
            if (atual == null || agora - atual.inicioMillis() >= janelaMillis) {
                return new Contador(agora, 1);
            }
            return new Contador(atual.inicioMillis(), atual.requisicoes() + 1);
        });
        podar(agora);
        if (contador.requisicoes() <= limite) {
            return Decisao.liberado();
        }
        long restanteMillis = contador.inicioMillis() + janelaMillis - agora;
        long segundos = Math.max(1L, (restanteMillis + 999L) / 1000L);
        return new Decisao(false, segundos);
    }

    private void podar(long agora) {
        if (contadores.size() <= maximoChaves) {
            return;
        }
        contadores.entrySet()
                .removeIf(entrada -> agora - entrada.getValue().inicioMillis() >= janelaMaximaMillis);
    }

    int chavesRastreadas() {
        return contadores.size();
    }
}
