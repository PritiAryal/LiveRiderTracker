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

## Serialization/Deserialization of Java Objects

Create `RiderLocation` POJO.

Use JSON serializers on producer and consumer sides.

### Producer:

```java
@PostMapping("/send")
public String sendRiderLocation() {
    RiderLocation loc = new RiderLocation("rider1", 12.9716, 77.5946);
    kafkaTemplate.send("my-new-topic-2", loc);
    return "Sent!";
}
```

### Consumer Configuration:

```yaml
spring:
  kafka:
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.value.default.type: com.priti.consumer.model.RiderLocation
        spring.json.trusted.packages: '*'
```

---

## Spring Cloud Stream Integration

### Producer Using `Supplier<RiderLocation>`

```java
@Bean
public Supplier<RiderLocation> sendRiderLocation() {
    return () -> new RiderLocation("rider1", 12.9716, 77.5946);
}
```

```yaml
spring:
  cloud:
    function:
      definition: sendRiderLocation
    stream:
      bindings:
        sendRiderLocation-out-0:
          destination: my-new-topic-2
          content-type: application/json
```

---

### Consumer Using `Consumer<RiderLocation>`

```java
@Bean
public Consumer<RiderLocation> processRiderLocation() {
    return riderLocation -> {
        System.out.println("Received Rider Location: " + riderLocation.getRiderId());
    };
}
```

```yaml
spring:
  cloud:
    function:
      definition: processRiderLocation
    stream:
      bindings:
        processRiderLocation-in-0:
          destination: my-new-topic-2
          content-type: application/json
```

---

## Multi-topic Messaging

You can define multiple suppliers:

```java
@Bean
public Supplier<Message<String>> sendRiderStatus() {
    return () -> MessageBuilder.withPayload("status update")
        .setHeader(KafkaHeaders.KEY, "rider-id".getBytes())
        .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
        .build();
}
```

### Update bindings:

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

## Testing with Postman

Send POST request to:

```http
http://localhost:8080/api/send?message=HelloKafka
```

---

## Kafka Partitioning Logic

Kafka uses **message key hashing** to determine which partition to use. You can use unique keys (like `riderId`) to ensure proper distribution.

---

## Consumer Groups

- **Same groupId** → Kafka balances load across consumers.
- **Different groupIds** → All consumers receive the message (fan-out).

---

## Cleanup

```bash
docker stop kafka zookeeper
docker rm kafka zookeeper
```

---

## Conclusion

This project provides a complete blueprint for setting up event-driven microservices using **Kafka** and **Spring Boot** with **Docker**. It includes:

- Topic creation and management
- Producer and consumer setup
- Java object serialization
- Spring Cloud Stream functions
- Partition-based routing and consumer group behavior

This project can be extended to support other brokers like **RabbitMQ** by changing the dependencies and configs.

