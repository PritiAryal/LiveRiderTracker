package com.priti.consumer;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class KafkaConsumerStreams {
    @Bean
    public Consumer<RiderLocation> processRiderLocation() {
        return riderLocation -> {
            System.out.println("Received Rider Location: " + riderLocation.getRiderId() + " : " + riderLocation.getLatitude() + " : " + riderLocation.getLongitude());
            // Process the rider location data as needed
        };
    }

    @Bean
    public Consumer<String> processRiderStatus() {
        return status -> {
            System.out.println("Received Rider Status: " + status);
            // Process the rider location data as needed
        };
    }
}
