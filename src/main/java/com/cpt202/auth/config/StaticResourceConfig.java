package com.cpt202.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps uploaded files into the public resource handler.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    /**
     * Exposes the local upload directory under the uploads path.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
