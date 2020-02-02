package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.controller;

import com.scaleoutdata.kafka.types.Page;
import com.scaleoutdata.kafka.types.PageVisit;
import com.scaleoutdata.kafka.types.Visit;
import com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config.KafkaStreamsBindings;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowBytesStoreSupplier;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Duration;

@Controller
@Slf4j
public class KafkaStreamsController {
    private final long WINDOW = 5;

    @StreamListener
    @SendTo(KafkaStreamsBindings.OUTPUT_PAGE_VISIT)
    private KStream<String, PageVisit> process(
            @Input(KafkaStreamsBindings.INPUT_PAGE) KStream<String, Page> pageKStream,
            @Input(KafkaStreamsBindings.INPUT_VISIT) KStream<String, Visit> visitKStream) {

        WindowBytesStoreSupplier storeSupplier = Stores.inMemoryWindowStore("join-main",
                Duration.ofSeconds(86400), Duration.ofMinutes(2 * WINDOW), true);
        WindowBytesStoreSupplier otherSupplier = Stores.inMemoryWindowStore("join-other",
                Duration.ofSeconds(86400), Duration.ofMinutes(2 * WINDOW), true);

        return pageKStream
                .map((key, value) -> new KeyValue<String, Page>(value.getPageId(), value))

                .selectKey((key, value) -> value.getPageId(), Named.as("select-key"))

                .join(
                        visitKStream
                                .map((key, value) -> {
                            return new KeyValue<String, Visit>(value.getPageId(), value);
                        })
                                .selectKey((key, value) -> value.getPageId(), Named.as("select-key-orders")),

                        (page, visit) -> new PageVisit(page.getPageId(), page, visit),
                            JoinWindows.of(Duration.ofMinutes(WINDOW)),
                            StreamJoined.<String, Page, Visit>with(storeSupplier, otherSupplier)
                )

                .peek((key, value) -> log.info("Joined PageVisit, key: {}, page: {} visit: {} ",
                        key, value.getPage(), value.getVisit())
                )
                ;
    }

}
