package com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.controller;

import com.scaleoutdata.kafka.types.Page;
import com.scaleoutdata.kafka.types.PageVisit;
import com.scaleoutdata.kafka.types.Visit;
import com.scaleoutdata.spring.cloud.stream.kafka.streams.join_example.config.KafkaStreamsBindings;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowBytesStoreSupplier;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Duration;

@Controller
@Slf4j
public class AggregateJoinWithCustomStateStoreController {
    @StreamListener
    @SendTo(KafkaStreamsBindings.OUTPUT_PAGE_VISIT)
    public KStream<String, Long> aggregationPipeline(
            @Input(KafkaStreamsBindings.INPUT_PAGE) KStream<String, Page> pageKStream) {

        return aggregationWithCustomInMemoryStateStore(pageKStream);
    }

    private KStream<String, Long> aggregationWithDefaultStateStore(KStream<String, Page> pageKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        return pageKStream
                .filter((key, value) -> value.getPageId() != null)
                .groupByKey()
                .windowedBy(TimeWindows.of(windowDuration).advanceBy(windowDuration).grace(windowDuration))
                .count().toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                ;
    }

    private KStream<String, Long> aggregationWithCustomPersistentStateStore(KStream<String, Page> pageKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        WindowBytesStoreSupplier timestampedWindowsStore = Stores.persistentTimestampedWindowStore("storeName", windowDuration, windowDuration, true);
        Materialized<String, Long, WindowStore<Bytes, byte[]>> materializedCustomStore = Materialized.as(timestampedWindowsStore);

        return pageKStream
                .filter((key, value) -> value.getPageId() != null)
                .groupByKey()
                .windowedBy(TimeWindows.of(windowDuration).advanceBy(windowDuration).grace(windowDuration))
                .count(materializedCustomStore).toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                ;
    }

    private KStream<String, Long> aggregationWithCustomInMemoryStateStore(KStream<String, Page> pageKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        WindowBytesStoreSupplier timestampedWindowsStore = Stores.inMemoryWindowStore("storeName", windowDuration, windowDuration, true);
        Materialized<String, Long, WindowStore<Bytes, byte[]>> materializedCustomStore = Materialized.as(timestampedWindowsStore);

        return pageKStream
                .filter((key, value) -> value.getPageId() != null)
                .groupByKey()
                .windowedBy(TimeWindows.of(windowDuration).advanceBy(windowDuration).grace(windowDuration))
                .count(materializedCustomStore).toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                ;
    }

    private KStream<String, Long> aggregationWithCustomPersistentStoreAndDisabledLogs(KStream<String, Page> pageKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        WindowBytesStoreSupplier timestampedWindowsStore = Stores.persistentTimestampedWindowStore("storeName", windowDuration, windowDuration, true);
        Materialized<String, Long, WindowStore<Bytes, byte[]>> materializedCustomStore = Materialized.as(timestampedWindowsStore);
        materializedCustomStore.withLoggingDisabled();

        return pageKStream
                .filter((key, value) -> value.getPageId() != null)
                .groupByKey()
                .windowedBy(TimeWindows.of(windowDuration).advanceBy(windowDuration).grace(windowDuration))
                .count(materializedCustomStore).toStream()
                .map((key, value) -> new KeyValue<>(key.key(), value))
                ;
    }

    @StreamListener
    @SendTo(KafkaStreamsBindings.OUTPUT_PAGE_VISIT)
    public KStream<String, PageVisit> joinPipeline(
            @Input(KafkaStreamsBindings.INPUT_PAGE) KStream<String, Page> pageKStream,
            @Input(KafkaStreamsBindings.INPUT_VISIT) KStream<String, Visit> visitKStream) {
        return joinWithPersistentStateStore(pageKStream, visitKStream);
    }

    private KStream<String, PageVisit> joinWithDefaultStateStore(KStream<String, Page> pageKStream,  KStream<String, Visit> visitKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        JoinWindows joinWindows = JoinWindows.of(windowDuration.dividedBy(2));

        return pageKStream
                .map((key, value) -> new KeyValue<String, Page>(value.getPageId(), value))
                .selectKey((key, value) -> value.getPageId())
                .join(
                        visitKStream
                                .map((key, value) -> new KeyValue<String, Visit>(value.getPageId(), value))
                                .selectKey((key, value) -> value.getPageId()),
                        (page, visit) -> new PageVisit(page.getPageId(), page, visit),
                        joinWindows
                );
    }

    private KStream<String, PageVisit> joinWithPersistentStateStore(KStream<String, Page> pageKStream,  KStream<String, Visit> visitKStream) {
        Duration windowDuration = Duration.ofMinutes(10);
        Duration graceDuration = Duration.ofMinutes(10);

        WindowBytesStoreSupplier storeSupplier = Stores.persistentWindowStore("join-main",
                windowDuration.plus(graceDuration), windowDuration, true);
        WindowBytesStoreSupplier otherSupplier = Stores.persistentWindowStore("join-other",
                windowDuration.plus(graceDuration), windowDuration, true);
        JoinWindows joinWindows = JoinWindows.of(windowDuration.dividedBy(2)).grace(graceDuration);

        return pageKStream
                .map((key, value) -> new KeyValue<String, Page>(value.getPageId(), value))
                .selectKey((key, value) -> value.getPageId())
                .join(
                        visitKStream
                                .map((key, value) -> new KeyValue<String, Visit>(value.getPageId(), value))
                                .selectKey((key, value) -> value.getPageId()),

                        (page, visit) -> new PageVisit(page.getPageId(), page, visit),
                        joinWindows,
                        StreamJoined.<String, Page, Visit>with(storeSupplier, otherSupplier)
                );
    }


}
