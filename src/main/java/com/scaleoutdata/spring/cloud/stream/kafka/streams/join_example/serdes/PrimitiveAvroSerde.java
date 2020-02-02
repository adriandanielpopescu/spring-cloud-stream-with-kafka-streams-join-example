package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.serdes;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Collections;
import java.util.Map;

public class PrimitiveAvroSerde<T> implements Serde<T> {
    private final Serde<Object> inner;

    public PrimitiveAvroSerde() {
        inner = Serdes.serdeFrom(new KafkaAvroSerializer(), new KafkaAvroDeserializer());
    }

    public PrimitiveAvroSerde(SchemaRegistryClient schemaRegistryClient) {
        this(schemaRegistryClient, Collections.emptyMap());
    }

    public PrimitiveAvroSerde(SchemaRegistryClient client, Map<String, ?> props) {
        inner = Serdes.serdeFrom(new KafkaAvroSerializer(client), new KafkaAvroDeserializer(client, props));
    }

    @Override
    public void configure(final Map<String, ?> serdeConfig, final boolean isSerdeForRecordKeys) {
        inner.serializer().configure(serdeConfig, isSerdeForRecordKeys);
        inner.deserializer().configure(serdeConfig, isSerdeForRecordKeys);
    }

    @Override
    public void close() {
        inner.serializer().close();
        inner.deserializer().close();
    }


    @SuppressWarnings("unchecked")
    @Override
    public Serializer<T> serializer() {
        Object obj = inner.serializer();
        return (Serializer<T>) obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Deserializer<T> deserializer() {
        Object obj = inner.deserializer();
        return (Deserializer<T>) obj;
    }

}

