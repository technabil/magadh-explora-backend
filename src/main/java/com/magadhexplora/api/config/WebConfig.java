package com.magadhexplora.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public WebConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        String prefix = uploadProperties.getUrlPrefix();
        if (!prefix.endsWith("/")) prefix = prefix + "/";

        registry.addResourceHandler(prefix + "**")
                .addResourceLocations(location);
    }
}
