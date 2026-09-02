package dev.feit.entitymux.experiment.hibernate.spi;

import dev.feit.entitymux.experiment.model.Document;
import dev.feit.entitymux.experiment.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
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
@Import(HibernateSpiExperimentConfiguration.class)
@Transactional(readOnly = true)
class HibernateSpiQueryBoundaryTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AlternativeDocumentStore alternativeStore;

    @BeforeEach
    void resetLoadCount() {
        alternativeStore.resetLoadCount();
    }

    @Test
    void jpqlBypassesTheDirectGetListener() {
        Document document = entityManager.createQuery("""
                        select d
                        from Document d
                        where d.id = :id
                        """, Document.class)
                .setParameter("id", 10L)
                .getSingleResult();

        assertThat(document.getTitle()).isEqualTo("Architecture Notes");
        assertThat(alternativeStore.loadCount()).isZero();
    }

    @Test
    void criteriaApiBypassesTheDirectGetListener() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> query = builder.createQuery(Document.class);
        Root<Document> document = query.from(Document.class);
        query.select(document).where(builder.equal(document.get("id"), 11L));

        Document result = entityManager.createQuery(query).getSingleResult();

        assertThat(result.getTitle()).isEqualTo("Hibernate Findings");
        assertThat(alternativeStore.loadCount()).isZero();
    }

    @Test
    void countAndPaginationRemainOnThePrimaryStore() {
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
        assertThat(alternativeStore.loadCount()).isZero();
    }

    @Test
    void lazyToManyCollectionRemainsOnThePrimaryStore() {
        User user = entityManager.find(User.class, 1L);

        assertThat(user.getDocuments())
                .extracting(Document::getTitle)
                .containsExactlyInAnyOrder("Architecture Notes", "Hibernate Findings");
        assertThat(alternativeStore.loadCount()).isZero();
    }

    @Test
    void joinFetchRemainsOnThePrimaryStore() {
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

        assertThat(document.getTitle()).isEqualTo("Architecture Notes");
        assertThat(persistence.isLoaded(document, "owner")).isTrue();
        assertThat(document.getOwner().getUsername()).isEqualTo("alice");
        assertThat(alternativeStore.loadCount()).isZero();
    }
}
