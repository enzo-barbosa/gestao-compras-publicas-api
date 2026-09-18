package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gestaocompras.config.RateLimitProperties;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    private static final String CHAVE = "login|1.2.3.4";

    @Mock
    private Clock clock;

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(clock, propriedades(10000, 60));
    }

    private RateLimitProperties propriedades(int maximoChaves, long janelaSegundos) {
        RateLimitProperties.Limite limite = new RateLimitProperties.Limite();
        limite.setRequisicoes(3);
        limite.setJanelaSegundos(janelaSegundos);
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaximoChaves(maximoChaves);
        properties.setLimites(new LinkedHashMap<>(Map.of("login", limite)));
        return properties;
    }

    private void em(long millis) {
        when(clock.millis()).thenReturn(millis);
    }

    @Test
    void permiteAteOLimiteEBloqueiaAposExceder() {
        em(0);
        assertThat(service.verificar(CHAVE, 3, 60).permitido()).isTrue();
        assertThat(service.verificar(CHAVE, 3, 60).permitido()).isTrue();
        assertThat(service.verificar(CHAVE, 3, 60).permitido()).isTrue();

        RateLimitService.Decisao decisao = service.verificar(CHAVE, 3, 60);
        assertThat(decisao.permitido()).isFalse();
        assertThat(decisao.retryAfterSegundos()).isEqualTo(60);
    }

    @Test
    void liberaNovamenteAposJanelaExpirar() {
        em(0);
        service.verificar(CHAVE, 3, 60);
        service.verificar(CHAVE, 3, 60);
        service.verificar(CHAVE, 3, 60);
        assertThat(service.verificar(CHAVE, 3, 60).permitido()).isFalse();

        em(60000);
        assertThat(service.verificar(CHAVE, 3, 60).permitido()).isTrue();
    }

    @Test
    void retryAfterDiminuiConformeAJanelaAvanca() {
        em(0);
        service.verificar(CHAVE, 3, 60);
        service.verificar(CHAVE, 3, 60);
        service.verificar(CHAVE, 3, 60);

        em(45000);
        RateLimitService.Decisao decisao = service.verificar(CHAVE, 3, 60);
        assertThat(decisao.permitido()).isFalse();
        assertThat(decisao.retryAfterSegundos()).isEqualTo(15);
    }

    @Test
    void isolaContadoresPorChave() {
        em(0);
        service.verificar("login|1.1.1.1", 3, 60);
        service.verificar("login|1.1.1.1", 3, 60);
        service.verificar("login|1.1.1.1", 3, 60);
        assertThat(service.verificar("login|1.1.1.1", 3, 60).permitido()).isFalse();

        assertThat(service.verificar("login|2.2.2.2", 3, 60).permitido()).isTrue();
    }

    @Test
    void limiteInvalidoLiberaSemContar() {
        assertThat(service.verificar(CHAVE, 0, 60).permitido()).isTrue();
        assertThat(service.verificar(CHAVE, 5, 0).permitido()).isTrue();
        assertThat(service.chavesRastreadas()).isZero();
    }

    @Test
    void podaChavesExpiradasQuandoMapaPassaDoMaximo() {
        RateLimitService limitado = new RateLimitService(clock, propriedades(1, 60));

        em(0);
        limitado.verificar("login|1.1.1.1", 5, 60);
        assertThat(limitado.chavesRastreadas()).isEqualTo(1);

        em(60000);
        limitado.verificar("login|2.2.2.2", 5, 60);
        assertThat(limitado.chavesRastreadas()).isEqualTo(1);
    }
}
