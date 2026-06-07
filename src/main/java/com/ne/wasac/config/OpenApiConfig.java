package com.ne.wasac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wasacOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080").description("Local WASAC API"))
                .info(new Info()
                        .title("WASAC Utility Billing API")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste JWT token (without Bearer prefix)")))
                .tags(List.of(
                        new Tag().name("Authentication")
                                .description("Signup, login, change/reset password (OTP recovery)"),
                        new Tag().name("Profile").description("Current user profile — all roles"),
                        new Tag().name("Users")
                                .description("Create staff, list, role change, activate/deactivate (ADMIN)"),
                        new Tag().name("Customers")
                                .description("Admin create (temp password) or self-signup — profile CRUD"),
                        new Tag().name("Meters").description("Meter assignment + search"),
                        new Tag().name("Meter Readings").description("Capture readings — OPERATOR"),
                        new Tag().name("Tariffs").description("Pricing rules — create: ADMIN"),
                        new Tag().name("Bills").description("Bills + search — approve: FINANCE"),
                        new Tag().name("Payments").description("Payments + search — FINANCE"),
                        new Tag().name("Notifications").description("Customer notifications"),
                        new Tag().name("Audit Logs").description("Audit trail — ADMIN")));
    }

    @Bean
    public OpenApiCustomizer tagSorter() {
        List<String> order = List.of(
                "Authentication", "Profile", "Users", "Customers", "Meters",
                "Meter Readings", "Tariffs", "Bills", "Payments", "Notifications",
                "Audit Logs");
        return openApi -> openApi.getTags().sort(Comparator.comparingInt(
                tag -> {
                    int idx = order.indexOf(tag.getName());
                    return idx >= 0 ? idx : order.size();
                }));
    }
}
