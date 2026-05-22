package io.aether.processing.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("reading_flag")
public record ReadingFlagEntity(
        Long readingId,
        OffsetDateTime observedAt,
        String flag
) {}
