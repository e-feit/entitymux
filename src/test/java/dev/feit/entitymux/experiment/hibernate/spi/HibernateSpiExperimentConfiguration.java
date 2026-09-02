package dev.feit.entitymux.experiment.hibernate.spi;

import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
class HibernateSpiExperimentConfiguration {

    @Bean(destroyMethod = "close")
    AlternativeDocumentStore alternativeDocumentStore() {
        return AlternativeDocumentStore.create();
    }

    @Bean
    HibernatePropertiesCustomizer hibernateLoadRouting(
            AlternativeDocumentStore alternativeStore) {
        IntegratorProvider provider = () -> List.of(
                new HibernateLoadRoutingIntegrator(alternativeStore));
        return properties -> properties.put(JpaSettings.INTEGRATOR_PROVIDER, provider);
    }
}
