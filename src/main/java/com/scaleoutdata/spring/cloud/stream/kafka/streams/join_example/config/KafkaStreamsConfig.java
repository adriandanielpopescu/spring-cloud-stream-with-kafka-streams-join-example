package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Component
@AllArgsConstructor
@Slf4j
public class KafkaStreamsConfig {

    KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties;

    @Bean
    public StreamsBuilderFactoryBeanCustomizer streamsBuilderFactoryBeanCustomizer() {
        log.info("Customizing Kafka Streams");
        return factoryBean -> {
            factoryBean.setKafkaStreamsCustomizer(new KafkaStreamsCustomizer() {
                @Override
                public void customize(KafkaStreams kafkaStreams) {
                    // CUSTOMIZATIONS
                }
            });
        };
    }

}
