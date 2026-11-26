package com.royaltyhouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    @Bean
    public Clock relojSistema() {
        return Clock.systemDefaultZone();
    }
}
