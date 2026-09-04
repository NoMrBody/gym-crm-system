package config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI gymCrmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gym CRM REST API")
                        .version("1.0")
                        .description("REST API for the Gym CRM system: trainee/trainer registration, "
                                + "profile management, trainings and training types. "
                                + "Registration and login are public and return a JWT; every other "
                                + "endpoint requires an 'Authorization: Bearer <token>' header."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }

    // Add logout endpoint manually since it's handled by Spring Security, not a controller.
    @Bean
    public OpenApiCustomizer logoutEndpointCustomizer() {
        return openApi -> {
            Operation logout = new Operation()
                    .tags(List.of("Authentication"))
                    .summary("Logout")
                    .description("Revokes the presented bearer token so it can no longer be used.")
                    .responses(new ApiResponses()
                            .addApiResponse("204", new ApiResponse().description("Token revoked"))
                            .addApiResponse("401", new ApiResponse().description("No valid bearer token presented")))
                    .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));

            openApi.getPaths().addPathItem(SecurityConfig.LOGOUT_PATH, new PathItem().post(logout));
        };
    }
}
