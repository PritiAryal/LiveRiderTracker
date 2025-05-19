# LiveRiderTracker-SpringBoot-and-Kafka

# Spring Boot Kafka Microservices with Docker

This project demonstrates the integration of **Apache Kafka** with **Spring Boot**, covering core concepts like producers, consumers, message serialization, partitioning, consumer groups, and advanced messaging patterns using **Spring Cloud Stream**. Everything is containerized using **Docker**, enabling quick setup and scalability.

---

## Tech Stack

- Java 17+
- Spring Boot
- Apache Kafka
- Spring Cloud Stream
- Docker
- Postman (for testing REST APIs)

---

## Project Structure

- `producer`: Sends string messages and serialized Java objects to Kafka topics.
- `consumer`: Listens to Kafka topics and processes messages using various strategies.
- `docker`: Runs Zookeeper and Kafka using Confluent images.

---

## Kafka Setup with Docker

### 1. Run Zookeeper

```bash
docker run -d \
  --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:7.5.0
  ```

### 2. Run Kafka

```bash
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  --link zookeeper \
  confluentinc/cp-kafka:7.5.0
```

### 3. CLI Access to Kafka

```bash
docker exec -it kafka bash
```

---

## Kafka Operations

### Create Topics

```bash
kafka-topics --create --topic my-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### List & Describe Topics

```bash
kafka-topics --list --bootstrap-server localhost:9092
kafka-topics --describe --topic my-topic --bootstrap-server localhost:9092
```

### Console Producer & Consumer

```bash
# Producer
kafka-console-producer --topic my-topic --bootstrap-server localhost:9092

# Consumer (default)
kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092 --from-beginning

# Consumer with group
kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092 --group my-group --from-beginning
```

### Message with Key for Partitioning

```bash
kafka-console-producer --topic my-topic --bootstrap-server localhost:9092 \
  --property "parse.key=true" --property "key.separator=:"
```

---

## Spring Boot Kafka Producer

### REST API Endpoint

```java
@PostMapping("/send")
public String sendMessage(@RequestParam String message) {
    kafkaTemplate.send("my-topic", message);
    return "Message sent: " + message;
}
```

### Configuration

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

---

## Spring Boot Kafka Consumer

```java
@KafkaListener(topics = "my-topic", groupId = "my-group")
public void listen(String message) {
    System.out.println("Received: " + message);
}
```

---

## Object Serialization & Deserialization

### `RiderLocation` POJO

```java
public class RiderLocation {
    private String riderId;
    private double latitude;
    private double longitude;
    // Constructors, getters, setters...
}
```

### Producer (Object)

```java
@PostMapping("/send")
public String sendMessage(@RequestParam String message){
    RiderLocation riderLocation = new RiderLocation("rider1", 12.9716, 77.5946);
    kafkaTemplate.send("my-new-topic-2", riderLocation);
    return "Message sent: " + riderLocation.getRiderId();
}
```

### Consumer Configuration for Object

```yaml
spring:
  kafka:
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.trusted.packages: '*'
        spring.json.value.default.type: com.priti.consumer.model.RiderLocation
```

---

## Spring Cloud Stream Integration

### What is Spring Cloud Stream?

Spring Cloud Stream allows us to create **event-driven microservices** that are **broker-agnostic**. You don’t write Kafka-specific code; instead, you declare functions and bind them to Kafka topics. This abstraction makes it easier to switch to other brokers like RabbitMQ.

---

### Spring Cloud Function (Functional Bean)

```java
@Configuration
public class FunctionsClass {
    @Bean
    public Function<String, String> uppercase() {
        return value -> value.toLowerCase();
    }
}
```

```yaml
spring:
  cloud:
    function:
      definition: uppercase
    function.web.path: /uppercase
```

---

## Streaming with Supplier (Real-time)

```java
@Bean
public Supplier<RiderLocation> sendRiderLocation() {
    return () -> new RiderLocation("rider-174", 12.9716, 77.5946);
}
```

```yaml
spring:
  cloud:
    function:
      definition: sendRiderLocation
    stream:
      function:
        definition: sendRiderLocation
      bindings:
        sendRiderLocation-out-0:
          destination: my-new-topic-2
          content-type: application/json
      poller:
        fixed-delay: 5000
      kafka:
        binder:
          brokers: localhost:9092
```

> This will produce a new rider location every 5 seconds to Kafka — perfect for simulating live data feeds.

---

## Multi-topic Messaging

```java
@Bean
public Supplier<Message<String>> sendRiderStatus() {
    return () -> {
        String riderId = "rider" + new Random().nextInt(20);
        String status = new Random().nextBoolean() ? "Ride Started" : "Ride Completed";
        return MessageBuilder.withPayload(riderId + " : " + status)
            .setHeader(KafkaHeaders.KEY, riderId.getBytes())
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
            .build();
    };
}
```

```yaml
spring:
  cloud:
    function:
      definition: sendRiderLocation;sendRiderStatus
    stream:
      bindings:
        sendRiderLocation-out-0:
          destination: my-topic
        sendRiderStatus-out-0:
          destination: priti-topic
```

---

## Multi-topic Consumer

```java
@Bean
public Consumer<RiderLocation> processRiderLocation() {
    return riderLocation -> {
        System.out.println("Received Rider Location: " + riderLocation.getRiderId());
    };
}

@Bean
public Consumer<String> processRiderStatus() {
    return status -> {
        System.out.println("Received Rider Status: " + status);
    };
}
```

```yaml
spring:
  cloud:
    function:
      definition: processRiderLocation;processRiderStatus
    stream:
      bindings:
        processRiderLocation-in-0:
          destination: my-new-topic-2
          content-type: application/json
          group: rider-location-group
        processRiderStatus-in-0:
          destination: priti-topic
          content-type: text/plain
          group: rider-status-group
```

---

## Partition Routing Logic

Kafka uses the **message key** to hash and assign a partition.

```java
@Bean
public Supplier<RiderLocation> sendRiderLocation() {
    Random random = new Random();
    return () -> {
        String riderId = "rider" + random.nextInt(20);
        return new RiderLocation(riderId, 12.9716, 77.5946);
    };
}
```

> Different keys result in different partitions.

---

## Postman Testing

Use POST:

```http
http://localhost:8080/api/send?message=HelloKafka
```

✅ Status `200 OK`  
Message appears in Kafka consumer.

---

## Consumer Group Behavior

- Same group → Kafka distributes messages across consumers.
- Different groups → All consumers receive the message.

---

## Cleanup

```bash
docker stop kafka zookeeper
docker rm kafka zookeeper
```

---

## Conclusion

This project demonstrates a complete event-driven microservices architecture using:

- Kafka + Docker
- Spring Boot Producer & Consumer
- CLI tooling
- Object serialization
- Partition-aware routing
- Spring Cloud Stream for decoupling from Kafka
- Spring Cloud Function for functional endpoints
- Multi-topic producers & consumers
- Consumer group strategies

This project can be extended to support other brokers like **RabbitMQ** by changing the dependencies and configs.