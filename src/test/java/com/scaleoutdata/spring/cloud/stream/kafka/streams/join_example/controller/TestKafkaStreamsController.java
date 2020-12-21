package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.controller;
import com.scaleoutdata.kafka.types.Page;
import com.scaleoutdata.kafka.types.PageVisit;
import com.scaleoutdata.kafka.types.Visit;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.*;

import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;

import static com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config.KafkaStreamsBindings.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import com.bakdata.schemaregistrymock.SchemaRegistryMock;

/**
 * Example of TopologyTestDriver:
 * - https://www.confluent.io/blog/test-kafka-streams-with-topologytestdriver/
 * - https://github.com/spring-cloud/spring-cloud-stream-samples/blob/master/kafka-streams-samples/kafka-streams-word-count/src/test/java/kafka/streams/word/count/WordCountProcessorApplicationTests.java
 *
 * Lib that mocks the Kafka Schema Registry:
 * - https://github.com/bakdata/fluent-kafka-streams-tests/tree/master/schema-registry-mock
 * and blog:
 * - https://medium.com/bakdata/transparent-schema-registry-for-kafka-streams-6b43a3e7a15c
 *
 */
@Slf4j
public class TestKafkaStreamsController {
    private SchemaRegistryMock schemaRegistryMock;
    private TopologyTestDriver testDriver;

    private TestInputTopic<String, Page> testInputPage;
    private TestInputTopic<String, Visit> testInputVisit;
    private TestOutputTopic<String, PageVisit> testOutputTopic;

    public TestKafkaStreamsController() {
        this.schemaRegistryMock = new SchemaRegistryMock();
    }

    @Before
    public void setup() throws Exception {
        // start schemaRegistryMock
        this.schemaRegistryMock.start();

        // create TopologyTestDriver
        SpecificAvroSerde specificAvroSerde = createSpecificAvroSerde();
        testDriver = createTopologyDriver(specificAvroSerde);

        // create the input/output topics
        testInputPage = testDriver.createInputTopic(INPUT_PAGE, Serdes.String().serializer(), specificAvroSerde.serializer());
        testInputVisit = testDriver.createInputTopic(INPUT_VISIT, Serdes.String().serializer(), specificAvroSerde.serializer());
        testOutputTopic = testDriver.createOutputTopic(OUTPUT_PAGE_VISIT, Serdes.String().deserializer(), specificAvroSerde.deserializer());
    }

    private SpecificAvroSerde createSpecificAvroSerde() {
        SchemaRegistryClient mockSchemaRegistryClient = new MockSchemaRegistryClient();

        Map<String, String> config = new HashMap<>();
        config.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryMock.getUrl());

        SpecificAvroSerde specificAvroSerde = new SpecificAvroSerde(mockSchemaRegistryClient);
        specificAvroSerde.configure(config, false);
        return specificAvroSerde;
    }

    private Properties getStreamsConfiguration(String url) {
        final Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "TopologyTestDriver");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, url);

        return streamsConfiguration;
    }

    private TopologyTestDriver createTopologyDriver(SpecificAvroSerde specificAvroSerde) {
        // create the TopologyTestDriver
        Properties streamsConfig = getStreamsConfiguration(schemaRegistryMock.getUrl());
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Page> inputPage = builder.stream(INPUT_PAGE, Consumed.with(Serdes.String(), specificAvroSerde));
        KStream<String, Visit> inputVisit = builder.stream(INPUT_VISIT, Consumed.with(Serdes.String(), specificAvroSerde));
        KafkaStreamsController kafkaStreamsController = new KafkaStreamsController();
        KStream<String, PageVisit> output = kafkaStreamsController.process(inputPage, inputVisit);
        output.to(OUTPUT_PAGE_VISIT, Produced.with(Serdes.String(), specificAvroSerde));
        testDriver = new TopologyTestDriver(builder.build(), streamsConfig);
        return testDriver;
    }

    @After
    public void tearDown() {
        testDriver.close();
        this.schemaRegistryMock.stop();
    }

    @Test
    public void testJoinMatch() {
        testInputPage.pipeInput("key", new Page("p1", "address", "owner"));
        testInputPage.advanceTime(Duration.ofSeconds(1));
        testInputVisit.pipeInput("key", new Visit("v1", "p1", "user", 10l, 10l));
        testInputVisit.advanceTime(Duration.ofSeconds(1));
        KeyValue<String, PageVisit> output = testOutputTopic.readKeyValue();
        assertThat(output).isNotNull();
        assertThat(output.value).isNotNull();
        assertThat(output.value.getPage().getPageId()).isEqualTo("p1");
        assertThat(output.value.getVisit().getPageId()).isEqualTo("p1");
        assertThat(output.value.getVisit().getVisitId()).isEqualTo("v1");
    }

    @Test
    public void testJoinNoMatch() {
        testInputPage.pipeInput("key", new Page("pageid", "address", "owner"));
        testInputPage.advanceTime(Duration.ofSeconds(1));

        testInputVisit.pipeInput("key", new Visit("visitid", "pageid-2", "user", 10l, 10l));
        testInputVisit.advanceTime(Duration.ofSeconds(1));

        assertThat(testOutputTopic.isEmpty()).isTrue();
    }

    @Test
    public void testStateStores() throws Exception {
        testJoinMatch();

        Map<String, StateStore> allStateStores = testDriver.getAllStateStores();
        assertThat(allStateStores).isNotNull();
        assertThat(allStateStores.size()).isEqualTo(2);
        for (StateStore stateStore : allStateStores.values()) {
            log.info("Store name: {} info: {}", stateStore.name(), stateStore);
        }

        WindowStore<String, Page> stateStore = testDriver.getWindowStore("join-main");
        assertThat(stateStore).isNotNull();
        KeyValueIterator<Windowed<String>, Page> keyValueIterator = stateStore.all();
        assertThat(keyValueIterator).isNotNull();
        assertThat(size(keyValueIterator)).isEqualTo(1);
    }

    private long size(KeyValueIterator<Windowed<String>, Page> keyValueIterator) {
        int size = 0;
        while (keyValueIterator.hasNext()) {
            KeyValue<Windowed<String>, Page> keyValue = keyValueIterator.next();
            log.info("Next key={} value={}", keyValue.key, keyValue.value);
            size++;
        }
        keyValueIterator.close();
        return size;
    }

}

