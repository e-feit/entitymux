package dev.feit.entitymux.experiment.h2.materialized;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;

final class H2DocumentMaterializer
        implements ApplicationListener<ApplicationReadyEvent> {

    private final JdbcTemplate jdbcTemplate;
    private final MaterializedDocumentProvider provider;

    H2DocumentMaterializer(
            JdbcTemplate jdbcTemplate,
            MaterializedDocumentProvider provider) {
        this.jdbcTemplate = jdbcTemplate;
        this.provider = provider;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (MaterializedDocumentData document : provider.documents()) {
            jdbcTemplate.update(
                    "insert into documents (id, title, owner_id) values (?, ?, ?)",
                    document.id(),
                    document.title(),
                    document.ownerId());
        }
    }
}
