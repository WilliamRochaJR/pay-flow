package com.payflow.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI payFlowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PayFlow API")
                .version("v1")
                .description("API de demonstração para contas e transferências com valores fictícios."));
    }
}
