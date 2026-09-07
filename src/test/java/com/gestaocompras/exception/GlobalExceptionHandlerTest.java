package com.gestaocompras.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CenariosController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void recursoNaoEncontradoRetorna404ComMensagem() throws Exception {
        mockMvc.perform(get("/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Dotação 999 não encontrada."));
    }

    @Test
    void conflitoDeConcorrenciaRetorna409() throws Exception {
        mockMvc.perform(get("/conflito-concorrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Conflito de concorrência"))
                .andExpect(jsonPath("$.mensagem", containsString("Outra operação alterou")));
    }

    @Test
    void saldoInsuficienteRetorna400() throws Exception {
        mockMvc.perform(get("/saldo-insuficiente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Regra de negócio violada"));
    }

    @Test
    void registroDuplicadoRetorna409() throws Exception {
        mockMvc.perform(get("/registro-duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Registro duplicado"))
                .andExpect(jsonPath("$.mensagem").value("CNPJ já cadastrado."));
    }

    @Test
    void operacaoNaoPermitidaRetorna409() throws Exception {
        mockMvc.perform(get("/operacao-nao-permitida"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Operação não permitida"));
    }

    @Test
    void argumentoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/argumento-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A duração mínima é de 1 mês."));
    }

    @Test
    void erroInternoRetorna500SemVazarDetalhes() throws Exception {
        mockMvc.perform(get("/erro-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.erro").value("Erro interno"))
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.mensagem", not(containsString("segredo"))));
    }

    @Test
    void metodoNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(post("/nao-encontrado"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.erro").value("Método não permitido"));
    }

    @Test
    void parametroAusenteRetorna400ComDetalhe() throws Exception {
        mockMvc.perform(get("/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("'ano'")));
    }

    @Test
    void tipoDeParametroInvalidoRetorna400ComDetalhe() throws Exception {
        mockMvc.perform(get("/parametro").param("ano", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("'ano'")));
    }

    @Test
    void corpoMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição malformado ou ausente."));
    }

    @Test
    void mediaTypeNaoSuportadaRetorna415() throws Exception {
        mockMvc.perform(post("/validacao")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("corpo de texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.erro").value("Tipo de mídia não suportado"));
    }

    @Test
    void validacaoRetorna400ComDetalhesDosCampos() throws Exception {
        mockMvc.perform(post("/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("nome", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.detalhes", hasSize(2)))
                .andExpect(jsonPath("$.detalhes", hasItem(startsWith("nome:"))))
                .andExpect(jsonPath("$.detalhes", hasItem(startsWith("quantidade:"))));
    }

    @RestController
    static class CenariosController {

        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new NotFoundException("Dotação 999 não encontrada.");
        }

        @GetMapping("/conflito-concorrencia")
        void conflitoConcorrencia() {
            throw new ObjectOptimisticLockingFailureException("Dotacao", 123L);
        }

        @GetMapping("/saldo-insuficiente")
        void saldoInsuficiente() {
            throw new SaldoInsuficienteException("Saldo insuficiente para o contrato 015/2026.");
        }

        @GetMapping("/registro-duplicado")
        void registroDuplicado() {
            throw new RegistroDuplicadoException("CNPJ já cadastrado.");
        }

        @GetMapping("/operacao-nao-permitida")
        void operacaoNaoPermitida() {
            throw new OperacaoNaoPermitidaException("A licitação já está encerrada.");
        }

        @GetMapping("/argumento-invalido")
        void argumentoInvalido() {
            throw new IllegalArgumentException("A duração mínima é de 1 mês.");
        }

        @GetMapping("/erro-interno")
        void erroInterno() {
            throw new IllegalStateException("segredo interno que não deve vazar");
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam Integer ano) {
            // apenas para acionar os handlers de parâmetro
        }

        @PostMapping("/validacao")
        void validacao(@Valid @RequestBody ObjetoValidacao corpo) {
            // apenas para acionar o handler de validação
        }
    }

    static class ObjetoValidacao {

        @NotBlank
        String nome;

        @NotNull
        @Min(1)
        Integer quantidade;
    }
}