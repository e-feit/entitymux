package dev.feit.entitymux.experiment.hibernate.synthetic;

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

final class HibernateSyntheticDocumentIntegrator implements Integrator {

    private final SyntheticDocumentProvider provider;

    HibernateSyntheticDocumentIntegrator(SyntheticDocumentProvider provider) {
        this.provider = provider;
    }

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        EventListenerRegistry registry = sessionFactory.getEventListenerRegistry();
        EventListenerGroup<LoadEventListener> listeners =
                registry.getEventListenerGroup(EventType.LOAD);
        List<LoadEventListener> delegates = copyListeners(listeners);
        registry.setListeners(
                EventType.LOAD,
                new SyntheticDocumentLoadEventListener(provider, delegates));
    }

    @SuppressWarnings("deprecation")
    private static List<LoadEventListener> copyListeners(
            EventListenerGroup<LoadEventListener> listeners) {
        return StreamSupport.stream(listeners.listeners().spliterator(), false)
                .toList();
    }
}
