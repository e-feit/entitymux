package dev.feit.entitymux.experiment.hibernate.synthetic;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
class HibernateSyntheticDocumentExperimentConfiguration {

    @Bean
    SyntheticDocumentProvider syntheticDocumentProvider() {
        return new SyntheticDocumentProvider();
    }

    @Bean
    RecordingStatementInspector recordingStatementInspector() {
        return new RecordingStatementInspector();
    }

    @Bean
    HibernatePropertiesCustomizer syntheticDocumentRouting(
            SyntheticDocumentProvider provider,
            RecordingStatementInspector statementInspector) {
        IntegratorProvider integratorProvider = () -> List.of(
                new HibernateSyntheticDocumentIntegrator(provider));
        return properties -> {
            properties.put(JpaSettings.INTEGRATOR_PROVIDER, integratorProvider);
            properties.put(JdbcSettings.STATEMENT_INSPECTOR, statementInspector);
        };
    }
}
