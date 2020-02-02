package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example;

import com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config.KafkaStreamsBindings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.schema.client.EnableSchemaRegistryClient;

@SpringBootApplication
@EnableBinding(KafkaStreamsBindings.class)
@EnableSchemaRegistryClient
@EnableCaching
@Slf4j
public class JoinExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoinExampleApplication.class, args);
    }

}
