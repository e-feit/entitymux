package dev.feit.entitymux.experiment.hibernate.spi;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(HibernateSpiExperimentConfiguration.class)
@Transactional(readOnly = true)
class HibernateSpiFindBehaviorTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AlternativeDocumentStore alternativeStore;

    @BeforeEach
    void resetLoadCount() {
        alternativeStore.resetLoadCount();
    }

    @Test
    void findRoutesDocumentToTheAlternativeInMemoryStore() {
        Document document = entityManager.find(Document.class, 10L);

        assertThat(document.getTitle()).isEqualTo("SPI Architecture Notes");
        assertThat(alternativeStore.loadCount()).isEqualTo(1);
    }

    @Test
    void findKeepsUserOnThePrimaryInMemoryStore() {
        User user = entityManager.find(User.class, 1L);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(alternativeStore.loadCount()).isZero();
    }

    @Test
    void routedDocumentIsNotManagedByThePrimaryPersistenceContext() {
        Document document = entityManager.find(Document.class, 10L);

        assertThat(entityManager.contains(document)).isFalse();
        assertThat(alternativeStore.loadCount()).isEqualTo(1);
    }

    @Test
    void repeatedFindDoesNotPreserveIdentityForRoutedDocuments() {
        Document first = entityManager.find(Document.class, 10L);
        Document second = entityManager.find(Document.class, 10L);

        assertThat(second).isNotSameAs(first);
        assertThat(alternativeStore.loadCount()).isEqualTo(2);
    }

    @Test
    void lazyToOneCannotInitializeAfterTheAlternativeSessionWasClosed() {
        Document document = entityManager.find(Document.class, 10L);

        assertThatThrownBy(() -> document.getOwner().getUsername())
                .isInstanceOf(LazyInitializationException.class);
        assertThat(alternativeStore.loadCount()).isEqualTo(1);
    }
}
