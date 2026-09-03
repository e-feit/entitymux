package dev.feit.entitymux.experiment.hibernate.synthetic;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(HibernateSyntheticDocumentExperimentConfiguration.class)
@Transactional(readOnly = true)
class SyntheticDocumentFindBehaviorTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SyntheticDocumentProvider provider;

    @Autowired
    private RecordingStatementInspector statementInspector;

    @BeforeEach
    void resetObservations() {
        provider.resetGenerationCount();
        statementInspector.reset();
    }

    @Test
    void findGeneratesSyntheticDocumentsWithoutExecutingSql() {
        Document architecture = entityManager.find(Document.class, 1000L);
        Document hibernate = entityManager.find(Document.class, 1001L);

        assertThat(architecture.getId()).isEqualTo(1000L);
        assertThat(architecture.getTitle()).isEqualTo("Synthetic Architecture Brief");
        assertThat(hibernate.getId()).isEqualTo(1001L);
        assertThat(hibernate.getTitle()).isEqualTo("Synthetic Hibernate Brief");
        assertThat(provider.generationCount()).isEqualTo(2);
        assertThat(statementInspector.statements()).isEmpty();
    }

    @Test
    void findKeepsPrimaryEntitiesAndNonSyntheticDocumentsInH2() {
        User user = entityManager.find(User.class, 1L);
        Document document = entityManager.find(Document.class, 10L);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(document.getTitle()).isEqualTo("Architecture Notes");
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }

    @Test
    void syntheticDocumentIsNotManagedByThePrimaryPersistenceContext() {
        Document document = entityManager.find(Document.class, 1000L);

        assertThat(entityManager.contains(document)).isFalse();
        assertThat(provider.generationCount()).isEqualTo(1);
        assertThat(statementInspector.statements()).isEmpty();
    }

    @Test
    void repeatedSyntheticFindDoesNotPreserveIdentity() {
        Document first = entityManager.find(Document.class, 1000L);
        Document second = entityManager.find(Document.class, 1000L);

        assertThat(second).isNotSameAs(first);
        assertThat(provider.generationCount()).isEqualTo(2);
        assertThat(statementInspector.statements()).isEmpty();
    }

    @Test
    void syntheticDocumentCanResolveItsOwnerFromThePrimaryStore() {
        Document document = entityManager.find(Document.class, 1000L);

        assertThat(statementInspector.statements()).isEmpty();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
        assertThat(entityManager.contains(document.getOwner())).isTrue();
        assertThat(statementInspector.statements()).isNotEmpty();
        assertThat(provider.generationCount()).isEqualTo(1);
    }
}
