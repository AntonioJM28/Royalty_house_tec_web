package com.royaltyhouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ClienteSesionInterceptor clienteSesionInterceptor;

    public WebConfig(ClienteSesionInterceptor clienteSesionInterceptor) {
        this.clienteSesionInterceptor = clienteSesionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clienteSesionInterceptor)
                .addPathPatterns("/cliente/**");
    }
}
