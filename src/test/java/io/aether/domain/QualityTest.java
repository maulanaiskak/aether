package io.aether.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QualityTest {

    @Test
    void okQualityHasNoFlags() {
        var q = Quality.ok();
        assertThat(q.status()).isEqualTo(QualityStatus.OK);
        assertThat(q.flags()).isEmpty();
    }

    @Test
    void suspectWithMultipleFlags() {
        var flags = Set.of(QualityFlag.MISSING_VALUE, QualityFlag.OUT_OF_RANGE);
        var q = Quality.suspect(flags);
        assertThat(q.status()).isEqualTo(QualityStatus.SUSPECT);
        assertThat(q.flags()).containsExactlyInAnyOrder(QualityFlag.MISSING_VALUE, QualityFlag.OUT_OF_RANGE);
    }

    @Test
    void rejectedWithAnomalousFlag() {
        var q = Quality.rejected(Set.of(QualityFlag.ANOMALOUS));
        assertThat(q.status()).isEqualTo(QualityStatus.REJECTED);
        assertThat(q.flags()).containsExactly(QualityFlag.ANOMALOUS);
    }
}
