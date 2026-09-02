package dev.feit.entitymux.experiment.hibernate.spi;

import dev.feit.entitymux.experiment.model.Document;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;

final class RoutingLoadEventListener implements LoadEventListener {

    private final AlternativeDocumentStore alternativeStore;
    private final List<LoadEventListener> delegates;

    RoutingLoadEventListener(
            AlternativeDocumentStore alternativeStore,
            List<LoadEventListener> delegates) {
        this.alternativeStore = alternativeStore;
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
        if (loadType == GET
                && isDocument(event)
                && event.getEntityId() instanceof Long id) {
            event.setResult(alternativeStore.find(id));
            return;
        }

        delegates.forEach(delegate -> delegate.onLoad(event, loadType));
    }

    private boolean isDocument(LoadEvent event) {
        EntityPersister persister = event.getFactory()
                .getMappingMetamodel()
                .findEntityDescriptor(event.getEntityClassName());
        return persister != null && persister.getMappedClass() == Document.class;
    }
}
