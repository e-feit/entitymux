package dev.feit.entitymux.experiment.h2.materialized;

import java.util.List;

final class MaterializedDocumentProvider {

    List<MaterializedDocumentData> documents() {
        return List.of(
                new MaterializedDocumentData(
                        1000L, "Materialized Architecture Brief", 1L),
                new MaterializedDocumentData(
                        1001L, "Materialized Hibernate Brief", 2L));
    }
}
