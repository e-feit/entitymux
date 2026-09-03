package dev.feit.entitymux.experiment.h2.materialized;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration(proxyBeanMethods = false)
class H2MaterializationExperimentConfiguration {

    @Bean
    MaterializedDocumentProvider materializedDocumentProvider() {
        return new MaterializedDocumentProvider();
    }

    @Bean
    H2DocumentMaterializer h2DocumentMaterializer(
            JdbcTemplate jdbcTemplate,
            MaterializedDocumentProvider provider) {
        return new H2DocumentMaterializer(jdbcTemplate, provider);
    }
}
