package dev.feit.entitymux.experiment.baseline;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(readOnly = true)
class JpaBaselineBehaviorTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    void findLoadsDocumentFromTheConfiguredInMemoryDatabase() {
        Document document = entityManager.find(Document.class, 10L);

        assertThat(document.getTitle()).isEqualTo("Architecture Notes");
    }

    @Test
    void repeatedFindReturnsTheSameManagedInstance() {
        Document first = entityManager.find(Document.class, 10L);
        Document second = entityManager.find(Document.class, 10L);

        assertThat(second).isSameAs(first);
    }

    @Test
    void jpqlFiltersDocuments() {
        List<Document> documents = entityManager.createQuery("""
                        select d
                        from Document d
                        where d.title like :suffix
                        order by d.id
                        """, Document.class)
                .setParameter("suffix", "%Findings")
                .getResultList();

        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly(11L, 20L);
    }

    @Test
    void criteriaApiFiltersDocuments() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> query = builder.createQuery(Document.class);
        Root<Document> document = query.from(Document.class);
        query.select(document)
                .where(builder.equal(document.get("owner").get("id"), 1L))
                .orderBy(builder.asc(document.get("id")));

        List<Document> documents = entityManager.createQuery(query).getResultList();

        assertThat(documents)
                .extracting(Document::getId)
                .containsExactly(10L, 11L);
    }

    @Test
    void countAndPaginationAreHandledByTheDatabase() {
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
                .extracting(Document::getId)
                .containsExactly(11L);
    }

    @Test
    void toOneAssociationIsLoadedLazily() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();
        Document document = entityManager.find(Document.class, 10L);

        assertThat(persistence.isLoaded(document, "owner")).isFalse();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
        assertThat(persistence.isLoaded(document, "owner")).isTrue();
    }

    @Test
    void toManyAssociationIsLoadedLazily() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();
        User user = entityManager.find(User.class, 1L);

        assertThat(persistence.isLoaded(user, "documents")).isFalse();
        assertThat(user.getDocuments())
                .extracting(Document::getId)
                .containsExactlyInAnyOrder(10L, 11L);
        assertThat(persistence.isLoaded(user, "documents")).isTrue();
    }

    @Test
    void joinFetchLoadsTheToOneAssociationWithTheDocument() {
        PersistenceUnitUtil persistence = entityManager
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();

        Document document = entityManager.createQuery("""
                        select d
                        from Document d
                        join fetch d.owner
                        where d.id = :id
                        """, Document.class)
                .setParameter("id", 10L)
                .getSingleResult();

        assertThat(persistence.isLoaded(document, "owner")).isTrue();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
    }
}
