package com.payflow.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OperationCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI payFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayFlow API")
                        .version("v1")
                        .description("API de demonstração para contas e transferências com valores fictícios."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    OperationCustomizer correlationIdHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(
                new io.swagger.v3.oas.models.parameters.Parameter()
                        .in("header")
                        .name(CorrelationIdFilter.HEADER_NAME)
                        .required(false)
                        .description("UUID opcional para correlacionar requisição, resposta e logs")
                        .schema(new io.swagger.v3.oas.models.media.StringSchema().format("uuid"))
        );
    }
}
