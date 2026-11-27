package com.royaltyhouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ClienteSesionInterceptor clienteSesionInterceptor;
    private final RolInterceptor rolInterceptor;

    public WebConfig(ClienteSesionInterceptor clienteSesionInterceptor, RolInterceptor rolInterceptor) {
        this.clienteSesionInterceptor = clienteSesionInterceptor;
        this.rolInterceptor = rolInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clienteSesionInterceptor)
                .addPathPatterns("/cliente/**");
        registry.addInterceptor(rolInterceptor)
                .addPathPatterns("/recepcion/**", "/admin/**");
    }
}
