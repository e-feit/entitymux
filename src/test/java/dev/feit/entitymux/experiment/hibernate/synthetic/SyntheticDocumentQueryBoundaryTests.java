package dev.feit.entitymux.experiment.hibernate.synthetic;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(HibernateSyntheticDocumentExperimentConfiguration.class)
@Transactional(readOnly = true)
class SyntheticDocumentQueryBoundaryTests {

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
    void jpqlDoesNotIncludeSyntheticDocuments() {
        List<Document> documents = entityManager.createQuery(
                        "select d from Document d order by d.id", Document.class)
                .getResultList();

        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly(10L, 11L, 20L);
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }

    @Test
    void criteriaApiDoesNotIncludeSyntheticDocuments() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> query = builder.createQuery(Document.class);
        Root<Document> document = query.from(Document.class);
        query.select(document).where(builder.equal(document.get("id"), 1000L));

        List<Document> documents = entityManager.createQuery(query).getResultList();

        assertThat(documents).isEmpty();
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }

    @Test
    void countAndPaginationRemainLimitedToPrimaryDocuments() {
        Long count = entityManager.createQuery(
                        "select count(d) from Document d", Long.class)
                .getSingleResult();
        List<Document> page = entityManager.createQuery(
                        "select d from Document d order by d.id", Document.class)
                .setFirstResult(1)
                .setMaxResults(1)
                .getResultList();

        assertThat(count).isEqualTo(3L);
        assertThat(page)
                .extracting(Document::getTitle)
                .containsExactly("Hibernate Findings");
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }

    @Test
    void lazyCollectionDoesNotIncludeSyntheticDocuments() {
        User user = entityManager.find(User.class, 1L);

        assertThat(user.getDocuments())
                .extracting(Document::getTitle)
                .containsExactlyInAnyOrder("Architecture Notes", "Hibernate Findings");
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }

    @Test
    void joinFetchDoesNotIncludeSyntheticDocuments() {
        List<Document> documents = entityManager.createQuery("""
                        select d
                        from Document d
                        join fetch d.owner
                        where d.id = :id
                        """, Document.class)
                .setParameter("id", 1000L)
                .getResultList();

        assertThat(documents).isEmpty();
        assertThat(provider.generationCount()).isZero();
        assertThat(statementInspector.statements()).isNotEmpty();
    }
}
