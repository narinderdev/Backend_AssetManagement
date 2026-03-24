package com.example.eam.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    private static final List<String> COMPANY_ID_EXCLUDED_PREFIXES = List.of(
            "/api/companies",
            "/api/permissions",
            "/api/roles",
            "/api/security-dashboard",
            "/api/mfa"
    );

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("EAM API")
                        .version("v1")
                        .description("EAM API documentation"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer companyIdQueryParamCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (!requiresCompanyId(path) || pathItem == null) {
                    return;
                }

                for (PathItem.HttpMethod method : PathItem.HttpMethod.values()) {
                    var operation = pathItem.readOperationsMap().get(method);
                    if (operation == null) {
                        continue;
                    }
                    boolean alreadyPresent = operation.getParameters() != null
                            && operation.getParameters().stream()
                            .anyMatch(p -> "companyId".equalsIgnoreCase(p.getName()) && "query".equalsIgnoreCase(p.getIn()));
                    if (!alreadyPresent) {
                        operation.addParametersItem(buildCompanyIdParameter());
                    }
                }
            });
        };
    }

    private boolean requiresCompanyId(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }
        return COMPANY_ID_EXCLUDED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    private Parameter buildCompanyIdParameter() {
        return new Parameter()
                .name("companyId")
                .in("query")
                .required(true)
                .description("Company context ID. Must be mapped to the logged-in user.")
                .schema(new IntegerSchema().format("int64").minimum(BigDecimal.ONE));
    }
}
