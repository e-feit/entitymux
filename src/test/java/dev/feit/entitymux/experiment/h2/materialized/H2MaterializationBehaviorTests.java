package dev.feit.entitymux.experiment.h2.materialized;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(H2MaterializationExperimentConfiguration.class)
@Transactional(readOnly = true)
class H2MaterializationBehaviorTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    void findLoadsTheMaterializedDocumentAsAManagedEntity() {
        Document document = entityManager.find(Document.class, 1000L);

        assertThat(document.getTitle()).isEqualTo("Materialized Architecture Brief");
        assertThat(entityManager.contains(document)).isTrue();
    }

    @Test
    void repeatedFindPreservesIdentityForMaterializedDocuments() {
        Document first = entityManager.find(Document.class, 1000L);
        Document second = entityManager.find(Document.class, 1000L);

        assertThat(second).isSameAs(first);
    }

    @Test
    void jpqlIncludesMaterializedDocuments() {
        List<Document> documents = entityManager.createQuery(
                        "select d from Document d order by d.id", Document.class)
                .getResultList();

        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly(10L, 11L, 20L, 1000L, 1001L);
    }

    @Test
    void criteriaApiIncludesMaterializedDocumentsForTheirOwner() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> query = builder.createQuery(Document.class);
        Root<Document> document = query.from(Document.class);
        query.select(document)
                .where(builder.equal(document.get("owner").get("id"), 1L))
                .orderBy(builder.asc(document.get("id")));

        List<Document> documents = entityManager.createQuery(query).getResultList();

        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly(10L, 11L, 1000L);
    }

    @Test
    void countAndPaginationIncludeMaterializedDocuments() {
        Long count = entityManager.createQuery(
                        "select count(d) from Document d", Long.class)
                .getSingleResult();
        List<Document> page = entityManager.createQuery(
                        "select d from Document d order by d.id", Document.class)
                .setFirstResult(3)
                .setMaxResults(1)
                .getResultList();

        assertThat(count).isEqualTo(5L);
        assertThat(page)
                .extracting(Document::getTitle)
                .containsExactly("Materialized Architecture Brief");
    }

    @Test
    void materializedDocumentLoadsItsOwnerLazily() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();
        Document document = entityManager.find(Document.class, 1000L);

        assertThat(persistence.isLoaded(document, "owner")).isFalse();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
        assertThat(persistence.isLoaded(document, "owner")).isTrue();
    }

    @Test
    void primaryUserLazyCollectionIncludesMaterializedDocuments() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();
        User user = entityManager.find(User.class, 1L);

        assertThat(persistence.isLoaded(user, "documents")).isFalse();
        assertThat(user.getDocuments())
                .extracting(Document::getId)
                .containsExactlyInAnyOrder(10L, 11L, 1000L);
        assertThat(persistence.isLoaded(user, "documents")).isTrue();
    }

    @Test
    void joinFetchLoadsTheOwnerOfAMaterializedDocument() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();

        Document document = entityManager.createQuery("""
                        select d
                        from Document d
                        join fetch d.owner
                        where d.id = :id
                        """, Document.class)
                .setParameter("id", 1000L)
                .getSingleResult();

        assertThat(persistence.isLoaded(document, "owner")).isTrue();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
    }
}
