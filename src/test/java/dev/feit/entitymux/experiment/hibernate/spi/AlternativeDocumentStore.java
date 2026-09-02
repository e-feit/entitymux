package dev.feit.entitymux.experiment.hibernate.spi;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.SchemaToolingSettings;

import java.util.concurrent.atomic.AtomicInteger;

final class AlternativeDocumentStore implements AutoCloseable {

    private static final String JDBC_URL = """
            jdbc:h2:mem:entitymux-spi-alternative;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
            """.strip();

    private final SessionFactory sessionFactory;
    private final AtomicInteger loadCount = new AtomicInteger();

    private AlternativeDocumentStore(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    static AlternativeDocumentStore create() {
        SessionFactory sessionFactory = new Configuration()
                .addAnnotatedClasses(User.class, Document.class)
                .setJdbcUrl(JDBC_URL)
                .setCredentials("sa", "")
                .setProperty(SchemaToolingSettings.HBM2DDL_AUTO, "create-drop")
                .buildSessionFactory();
        seed(sessionFactory);
        return new AlternativeDocumentStore(sessionFactory);
    }

    private static void seed(SessionFactory sessionFactory) {
        sessionFactory.inTransaction(session -> {
            session.createNativeMutationQuery(
                            "insert into users (id, username) values (1, 'alice-spi')")
                    .executeUpdate();
            session.createNativeMutationQuery(
                            "insert into users (id, username) values (2, 'bob-spi')")
                    .executeUpdate();
            session.createNativeMutationQuery("""
                            insert into documents (id, title, owner_id)
                            values (10, 'SPI Architecture Notes', 1)
                            """)
                    .executeUpdate();
            session.createNativeMutationQuery("""
                            insert into documents (id, title, owner_id)
                            values (11, 'SPI Hibernate Findings', 1)
                            """)
                    .executeUpdate();
            session.createNativeMutationQuery("""
                            insert into documents (id, title, owner_id)
                            values (20, 'SPI JDBC Findings', 2)
                            """)
                    .executeUpdate();
        });
    }

    Document find(Long id) {
        loadCount.incrementAndGet();
        return sessionFactory.fromSession(session -> {
            session.setDefaultReadOnly(true);
            return session.find(Document.class, id);
        });
    }

    int loadCount() {
        return loadCount.get();
    }

    void resetLoadCount() {
        loadCount.set(0);
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
