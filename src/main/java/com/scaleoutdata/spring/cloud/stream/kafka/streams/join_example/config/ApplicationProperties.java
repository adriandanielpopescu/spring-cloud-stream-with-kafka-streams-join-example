package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    private String pagesTopicName;
    private String visitsTopicName;
    private String kafkaBrokerAddress;
    private String kafkaSchemaRegistryURL;
}
