package com.priti.producer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class FunctionsClass {
    @Bean
    public Function<String, String> upperCase() {
        return value -> value.toUpperCase();
    }

}
