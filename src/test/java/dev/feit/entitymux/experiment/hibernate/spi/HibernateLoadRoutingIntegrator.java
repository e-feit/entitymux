package dev.feit.entitymux.experiment.hibernate.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.integrator.spi.Integrator;

import java.util.List;
import java.util.stream.StreamSupport;

final class HibernateLoadRoutingIntegrator implements Integrator {

    private final AlternativeDocumentStore alternativeStore;

    HibernateLoadRoutingIntegrator(AlternativeDocumentStore alternativeStore) {
        this.alternativeStore = alternativeStore;
    }

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        EventListenerRegistry registry = sessionFactory.getEventListenerRegistry();
        EventListenerGroup<LoadEventListener> loadListeners =
                registry.getEventListenerGroup(EventType.LOAD);
        List<LoadEventListener> delegates = copyListeners(loadListeners);

        registry.setListeners(
                EventType.LOAD,
                new RoutingLoadEventListener(alternativeStore, delegates));
    }

    @SuppressWarnings("deprecation")
    private static List<LoadEventListener> copyListeners(
            EventListenerGroup<LoadEventListener> listeners) {
        return StreamSupport.stream(listeners.listeners().spliterator(), false)
                .toList();
    }
}
