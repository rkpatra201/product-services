package com.example.ps.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI productServicesOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Server URL in Development environment");

        Contact contact = new Contact();
        contact.setEmail("support@productservices.com");
        contact.setName("Product Services Team");
        contact.setUrl("https://github.com/productservices");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
            .title("Product Services API")
            .version("1.0.0")
            .contact(contact)
            .description("A comprehensive Spring Boot application demonstrating clean architecture, advanced caching strategies, and modern Java development practices. The service provides product metadata management with intelligent recommendation engine and multiple LRU caching implementations.")
            .termsOfService("https://productservices.com/terms")
            .license(mitLicense);

        return new OpenAPI().info(info).servers(List.of(devServer));
    }
}
