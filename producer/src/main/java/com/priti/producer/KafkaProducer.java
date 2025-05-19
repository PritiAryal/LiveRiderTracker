package com.priti.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class KafkaProducer {
    private final KafkaTemplate<String, RiderLocation> kafkaTemplate;
    public KafkaProducer(KafkaTemplate<String, RiderLocation> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam String message){
        RiderLocation riderLocation = new RiderLocation("rider1", 12.9716, 77.5946);
        kafkaTemplate.send("my-new-topic-2", riderLocation);
        return "Message sent: "+ riderLocation.getRiderId();
        //Simulate scenario: producer is application that is sending rider location to kafka topic and there is consumer listening to rider location
        // and consumer drawing that location and displaying it realtime
    }
}
// kafkaTemplate is used to send messages to Kafka topics. It uses key value pair.
// The key is used to determine the partition to which the message will be sent, and the value is the actual message content.
// The KafkaTemplate is a Spring abstraction that simplifies the process of sending messages to Kafka topics.
//KafkaTemplate is a high-level abstraction for sending messages to Kafka topics.
//It provides a simple and convenient way to interact with Kafka.

// The send method is used to send a message to a specific topic. It takes the topic name and the message as parameters.
// Request Param is used to extract the value of the parameter from the URL
// For example, if the URL is /api/send?message=Hello, this will extract "Hello"
// Logic to send message to Kafka topic
// For example, using KafkaTemplate
// Producer produces messages and send it to my-topic topic.
// The message is sent to the topic using the KafkaTemplate.
