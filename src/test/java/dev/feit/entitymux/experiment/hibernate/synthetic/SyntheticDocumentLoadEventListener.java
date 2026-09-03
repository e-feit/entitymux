package dev.feit.entitymux.experiment.hibernate.synthetic;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class SyntheticDocumentLoadEventListener implements LoadEventListener {

    private final SyntheticDocumentProvider provider;
    private final List<LoadEventListener> delegates;

    SyntheticDocumentLoadEventListener(
            SyntheticDocumentProvider provider,
            List<LoadEventListener> delegates) {
        this.provider = provider;
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
        if (loadType == GET && event.getEntityId() instanceof Long id) {
            EntityPersister persister = event.getFactory()
                    .getMappingMetamodel()
                    .findEntityDescriptor(event.getEntityClassName());
            if (persister != null && persister.getMappedClass() == Document.class) {
                Optional<SyntheticDocumentData> generated = provider.generate(id);
                if (generated.isPresent()) {
                    event.setResult(instantiate(generated.orElseThrow(), persister, event));
                    return;
                }
            }
        }

        delegates.forEach(delegate -> delegate.onLoad(event, loadType));
    }

    private Document instantiate(
            SyntheticDocumentData data,
            EntityPersister persister,
            LoadEvent event) {
        Document document =
                (Document) persister.instantiate(data.id(), event.getSession());
        setProperty(persister, document, "title", data.title());
        User owner = event.getSession().getReference(User.class, data.ownerId());
        setProperty(persister, document, "owner", owner);
        return document;
    }

    private static void setProperty(
            EntityPersister persister,
            Object entity,
            String name,
            Object value) {
        int index = Arrays.asList(persister.getPropertyNames()).indexOf(name);
        if (index < 0) {
            throw new HibernateException(
                    "Missing mapped property " + name + " on " + persister.getEntityName());
        }
        persister.setValue(entity, index, value);
    }
}
