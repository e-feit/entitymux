package dev.feit.entitymux.experiment.hibernate.synthetic;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class SyntheticDocumentProvider {

    private final AtomicInteger generationCount = new AtomicInteger();

    Optional<SyntheticDocumentData> generate(Long id) {
        SyntheticDocumentData data = null;
        if (Long.valueOf(1000L).equals(id)) {
            data = new SyntheticDocumentData(
                    1000L, "Synthetic Architecture Brief", 1L);
        } else if (Long.valueOf(1001L).equals(id)) {
            data = new SyntheticDocumentData(
                    1001L, "Synthetic Hibernate Brief", 2L);
        }
        if (data != null) {
            generationCount.incrementAndGet();
        }
        return Optional.ofNullable(data);
    }

    int generationCount() {
        return generationCount.get();
    }

    void resetGenerationCount() {
        generationCount.set(0);
    }
}
