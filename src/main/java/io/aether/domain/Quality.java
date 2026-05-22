package io.aether.domain;

import java.util.Set;

public record Quality(QualityStatus status, Set<QualityFlag> flags) {

    public static Quality ok() {
        return new Quality(QualityStatus.OK, Set.of());
    }

    public static Quality suspect(Set<QualityFlag> flags) {
        return new Quality(QualityStatus.SUSPECT, flags);
    }

    public static Quality rejected(Set<QualityFlag> flags) {
        return new Quality(QualityStatus.REJECTED, flags);
    }
}
