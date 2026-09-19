package com.meridiantrust.sentinel.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** OpenAPI/Swagger metadata and HTTP Basic security scheme for the API docs. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sentinel AML API",
                version = "v1",
                description = "Real-time transaction monitoring: ingestion, rule-based detection, "
                        + "risk-scored alerts and case management for compliance analysts.",
                license = @License(name = "Prototype")),
        security = @SecurityRequirement(name = "basicAuth"))
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic")
public class OpenApiConfig {
}
