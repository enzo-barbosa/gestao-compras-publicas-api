package com.gestaocompras.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "swagger.ativo", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gestão de Compras Públicas")
                        .description("API para o ciclo de compras públicas: dotação orçamentária, "
                                + "fornecedores, licitações, contratos e empenhos, com multitenancy "
                                + "por organização.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido em POST /api/auth/login. "
                                        + "Use o botão Authorize e informe o cabeçalho X-Org-Id.")));
    }

    @Bean
    public OpenApiCustomizer cabecalhoOrganizacaoGlobal() {
        return openApi -> openApi.getPaths().forEach((caminho, itemCaminho) -> {
            if (caminho.startsWith("/api/auth")) {
                return;
            }
            itemCaminho.readOperations().forEach(operacao -> operacao.addParametersItem(
                    new Parameter()
                            .in("header")
                            .name("X-Org-Id")
                            .required(true)
                            .description("ID da organização em que o usuário autenticado opera "
                                    + "(multitenancy). O token JWT identifica o usuário; este "
                                    + "cabeçalho define a organização de contexto da requisição.")
                            .example("1")
                            .schema(new StringSchema())));
        });
    }
}