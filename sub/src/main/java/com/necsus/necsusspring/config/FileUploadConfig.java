package com.necsus.necsusspring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configura o diretório de uploads para servir arquivos estáticos
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
