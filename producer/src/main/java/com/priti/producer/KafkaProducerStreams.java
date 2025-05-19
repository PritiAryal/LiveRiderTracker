package com.priti.producer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import java.util.Random;
import java.util.function.Supplier;

@Configuration
public class KafkaProducerStreams {
    @Bean
    public Supplier<RiderLocation> sendRiderLocation() {
        Random random = new Random();
        return () -> {
            String riderId = "rider" + random.nextInt(20);
            RiderLocation riderLocation = new RiderLocation(riderId, 12.9716, 77.5946);
            System.out.println("Sending Rider Location: " + riderLocation.getRiderId());
            return riderLocation;
        };
    }

    @Bean
    public Supplier<Message<String>> sendRiderStatus() {
        Random random = new Random();
        return () -> {
            String riderId = "rider" + random.nextInt(20);
            String status = random.nextBoolean() ? "Ride Started" : "Ride Completed";
            System.out.println("Sending Status: " + status);
            return MessageBuilder.withPayload(riderId+" : " + status)
                    .setHeader(KafkaHeaders.KEY, riderId.getBytes())
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
                    .build();
        };
    }
}
