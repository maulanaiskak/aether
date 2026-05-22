package io.aether.processing.repository;

import io.aether.processing.entity.ReadingFlagEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

public interface ReadingFlagRepository extends ReactiveCrudRepository<ReadingFlagEntity, Long> {

    Flux<ReadingFlagEntity> findByReadingIdAndObservedAt(Long readingId, OffsetDateTime observedAt);
}
