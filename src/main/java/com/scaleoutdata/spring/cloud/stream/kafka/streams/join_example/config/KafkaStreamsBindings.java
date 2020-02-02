package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.apache.kafka.streams.kstream.KStream;

public interface KafkaStreamsBindings {
    public static final String INPUT_PAGE = "input-page";
    public static final String INPUT_VISIT = "input-visit";
    public static final String OUTPUT_PAGE_VISIT = "output-page-visit";

    @Input(INPUT_PAGE)
    KStream<?, ?> inputPage();

    @Input(INPUT_VISIT)
    KStream<?, ?> inputVisit();

    @Output(OUTPUT_PAGE_VISIT)
    KStream<?, ?> outputPageVisit();
}