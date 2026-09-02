# In-Memory-Referenzplayground – Implementierungsplan

> **Für Umsetzungsagenten:** Erzeuge vor der Umsetzung mit
> `2-custom-writing-workpackage-plan` einen Workpackage-Index und für jedes
> Workpackage eine eigene, selbstständige Markdown-Datei mit dem vollständigen
> relevanten Planinhalt. Übergib danach denselben Index an
> `3-custom-executing-next-workpackage` oder `4-custom-review-workpackage`; der
> jeweilige Skill ermittelt daraus automatisch das nächste Paket.

**Ziel:** Ein ausschließlich mit H2 im Arbeitsspeicher betriebener JPA-Referenzplayground dokumentiert das unveränderte Hibernate-Verhalten für `find()`, JPQL, Criteria API, Pagination, Persistence Context, Lazy Loading und Associations als gemeinsame Vergleichsbasis für die nachfolgenden Hibernate-SPI- und JDBC/DataSource-Spikes.

**Architektur:** `User` und `Document` bilden im Paket `dev.feit.entitymux.experiment.model` ein bewusst kleines, bidirektionales Referenzmodell. Spring Boot verwaltet beide Entities in einer einzigen H2-In-Memory-Datenbank; ausschließlich Test-SQL legt reproduzierbare Lesedaten an. Der Liefergegenstand implementiert noch kein Entity-Routing und entscheidet weder für Hibernate SPI noch für einen JDBC/DataSource-Proxy.

**Technik:** Java 25, Spring Boot 4.1.1, Spring Data JPA, Hibernate ORM 7.4.5.Final, Jakarta Persistence, H2 2.4.240, JUnit 5, AssertJ, Maven Wrapper

## Quellen

- Nutzeranforderung vom 2026-09-02: „die ersten Untersuchungen sind gelaufen und das Ergebnis ist dem Entwickler präsentiert. Nutze zunächst ausschließlich die In-Memory Datenbank.“
- Architekturskizze: `/Users/eugen/AI-Markdown/entitymux-architecture-summary.md`
- Projektanweisungen: `/Users/eugen/Dev/entitymux/AGENTS.md`
- Projektbeschreibung: `/Users/eugen/Dev/entitymux/README.md`
- Maven-Konfiguration: `/Users/eugen/Dev/entitymux/pom.xml`
- Bestehende Spring-Konfiguration: `/Users/eugen/Dev/entitymux/src/main/resources/application.properties`
- Bestehender Kontexttest: `/Users/eugen/Dev/entitymux/src/test/java/dev/feit/entitymux/EntityMuxApplicationTests.java`
- Verifizierte Ausgangslage am 2026-09-02: `./mvnw clean verify` mit `BUILD SUCCESS`, 1 Test, 0 Fehler

## Globale Vorgaben

- Sprich immer deutsch, falls der User nicht extra nach einer anderen Sprache fragt.
- Nenne ihn immer per du.
- The project is currently an experimental playground.
- Do not imply production-ready stability.
- Currently in scope: Spring Boot.
- Currently in scope: Hibernate.
- Currently in scope: JPA / Jakarta Persistence.
- Currently in scope: Java 25.
- Currently in scope: Maven.
- Explicitly out of scope: WildFly.
- Explicitly out of scope: other JPA providers.
- Explicitly out of scope: a production-ready API.
- Explicitly out of scope: write routing.
- Explicitly out of scope: distributed data sources.
- Explicitly out of scope: Cross-Source-Joins.
- Explicitly out of scope: complete SQL interpretation.
- Do not commit prematurely to JDBC or DataSource proxies.
- Initially treat Hibernate SPI and JDBC/DataSource routing as alternative research directions.
- Do not unnecessarily lose entity-level semantics at the SQL level.
- Do not build a custom SQL engine.
- Do not make hidden assumptions about Hibernate-generated SQL.
- Explicitly account for lazy loading and associations.
- Treat JPA queries, the Criteria API, `find()`, lazy collections, and associations as distinct technical problems.
- Consider cross-source joins unsupported for now.
- Investigate read-only behavior first and mutations later.
- Do not introduce unnecessary abstractions for hypothetical alternative JPA providers.
- Respect the existing structure and prefer small changes.
- Avoid unnecessary refactoring.
- Add dependencies only when there is a clear, current need.
- Use Spring and Hibernate internal APIs deliberately and encapsulate them in isolation.
- Write tests for observed behavior.
- Clearly separate technical experiments from a future public API.
- Do not adopt an implementation solely because it works with `findById()`.
- Consider behavior involving JPQL, associations, lazy loading, and the persistence context.
- Before completing any change, run at least `./mvnw clean verify`.
- Nutze zunächst ausschließlich die In-Memory Datenbank.

## Offene Fragen

Keine.

## Geklärte offene Fragen

Noch keine.

---

## Aufgabe 1: Kleines Referenzmodell für Entity- und Association-Verhalten

**Dateien:**

- Erstellen: `src/main/java/dev/feit/entitymux/experiment/model/User.java`
- Erstellen: `src/main/java/dev/feit/entitymux/experiment/model/Document.java`
- Prüfen: `./mvnw -q -DskipTests compile`

**Schnittstellen:**

- Nutzt: Jakarta-Persistence-Annotationen aus `spring-boot-starter-data-jpa`
- Liefert: Entity `User` mit `Long id`, `String username` und lazy `List<Document> documents`; Entity `Document` mit `Long id`, `String title` und lazy `User owner`

1. Lege das Paket `dev.feit.entitymux.experiment.model` als sichtbar experimentellen Bereich unter `src/main/java` an; füge keine Registry, Provider-Schnittstelle oder Routing-Abstraktion hinzu.
2. Erstelle `User.java` mit genau folgendem Inhalt:

   ```java
   package dev.feit.entitymux.experiment.model;

   import jakarta.persistence.Entity;
   import jakarta.persistence.Id;
   import jakarta.persistence.OneToMany;
   import jakarta.persistence.Table;

   import java.util.ArrayList;
   import java.util.Collections;
   import java.util.List;

   @Entity
   @Table(name = "users")
   public class User {

       @Id
       private Long id;

       private String username;

       @OneToMany(mappedBy = "owner")
       private List<Document> documents = new ArrayList<>();

       protected User() {
       }

       public Long getId() {
           return id;
       }

       public String getUsername() {
           return username;
       }

       public List<Document> getDocuments() {
           return Collections.unmodifiableList(documents);
       }
   }
   ```

3. Erstelle `Document.java` mit genau folgendem Inhalt:

   ```java
   package dev.feit.entitymux.experiment.model;

   import jakarta.persistence.Entity;
   import jakarta.persistence.FetchType;
   import jakarta.persistence.Id;
   import jakarta.persistence.JoinColumn;
   import jakarta.persistence.ManyToOne;
   import jakarta.persistence.Table;

   @Entity
   @Table(name = "documents")
   public class Document {

       @Id
       private Long id;

       private String title;

       @ManyToOne(fetch = FetchType.LAZY, optional = false)
       @JoinColumn(name = "owner_id", nullable = false)
       private User owner;

       protected Document() {
       }

       public Long getId() {
           return id;
       }

       public String getTitle() {
           return title;
       }

       public User getOwner() {
           return owner;
       }
   }
   ```

4. Führe `./mvnw -q -DskipTests compile` aus; erwarte Exitcode 0 und zwei kompilierte Entity-Klassen unter `target/classes/dev/feit/entitymux/experiment/model`.
5. Übergib beide neuen Entity-Dateien, den erfolgreichen Compile-Befehl und den Hinweis, dass das Modell absichtlich keine Mutationsmethoden und keine Routing-API anbietet.

## Aufgabe 2: Reproduzierbare Read-only-Testdaten in H2 bereitstellen

**Dateien:**

- Erstellen: `src/test/resources/application.properties`
- Erstellen: `src/test/resources/data.sql`
- Prüfen: `./mvnw -q -Dtest=EntityMuxApplicationTests test`

**Schnittstellen:**

- Nutzt: Tabellen `users` und `documents` aus den Entities `User` und `Document`
- Liefert: isolierte H2-Datenbank `jdbc:h2:mem:entitymux-test`, Schemaerzeugung durch Hibernate sowie drei unveränderliche `Document`- und zwei `User`-Fixtures

1. Belasse `src/main/resources/application.properties` unverändert, damit Laufzeit- und Testkonfiguration getrennt bleiben.
2. Erstelle `src/test/resources/application.properties` mit genau folgendem Inhalt:

   ```properties
   spring.datasource.url=jdbc:h2:mem:entitymux-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
   spring.datasource.username=sa
   spring.datasource.password=
   spring.jpa.hibernate.ddl-auto=create-drop
   spring.jpa.defer-datasource-initialization=true
   spring.sql.init.mode=always
   ```

3. Erstelle `src/test/resources/data.sql` mit genau folgendem Inhalt:

   ```sql
   insert into users (id, username) values (1, 'alice');
   insert into users (id, username) values (2, 'bob');

   insert into documents (id, title, owner_id) values (10, 'Architecture Notes', 1);
   insert into documents (id, title, owner_id) values (11, 'Hibernate Findings', 1);
   insert into documents (id, title, owner_id) values (20, 'JDBC Findings', 2);
   ```

4. Führe `./mvnw -q -Dtest=EntityMuxApplicationTests test` aus; erwarte Exitcode 0 und `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` in `target/surefire-reports/dev.feit.entitymux.EntityMuxApplicationTests.txt`.
5. Übergib die beiden Testressourcen, den erfolgreichen Kontexttest und den Hinweis, dass Fixtures ausschließlich per Startskript entstehen und Anwendungscode keine Schreiboperation ausführt.

## Aufgabe 3: JPA-Verhaltensmatrix als ausführbare Integrationstests erfassen

**Dateien:**

- Erstellen: `src/test/java/dev/feit/entitymux/experiment/baseline/JpaBaselineBehaviorTests.java`
- Prüfen: `./mvnw -q -Dtest=JpaBaselineBehaviorTests test`

**Schnittstellen:**

- Nutzt: `User`, `Document` und die IDs 1, 2, 10, 11, 20 aus `data.sql`; Spring-verwalteten `EntityManager`; `PersistenceUnitUtil`
- Liefert: separat benannte Beobachtungen für `find()`, Persistence-Context-Identität, JPQL, Criteria API, Count/Pagination, lazy To-One-, lazy To-Many- und `JOIN FETCH`-Verhalten

1. Lege das Testpaket `dev.feit.entitymux.experiment.baseline` an, damit die Nullmessung nicht mit den separaten SPI- oder JDBC-Spikes vermischt wird.
2. Erstelle `JpaBaselineBehaviorTests.java` mit genau folgendem Inhalt:

   ```java
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
       void countAndPaginationReturnExpectedResults() {
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
   ```

3. Führe `./mvnw -q -Dtest=JpaBaselineBehaviorTests test` aus; erwarte Exitcode 0 und `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` in `target/surefire-reports/dev.feit.entitymux.experiment.baseline.JpaBaselineBehaviorTests.txt`.
4. Prüfe in der Testdatei mit `rg -n "void (find|repeated|jpql|criteria|count|toOne|toMany|joinFetch)" src/test/java/dev/feit/entitymux/experiment/baseline/JpaBaselineBehaviorTests.java`, dass alle acht bewusst getrennten Beobachtungen vorhanden sind.
5. Übergib den neuen Integrationstest, beide Prüfergebnisse und die Einschränkung, dass er das Hibernate-Referenzverhalten in genau einer In-Memory-Datenbank beschreibt, aber noch kein Routing beweist.

## Aufgabe 4: Referenzumfang und Grenzen im README sichtbar machen

**Dateien:**

- Ändern: `README.md:34-44` – Abschnitt nach „Run“ um Referenzplayground und Verhaltensmatrix ergänzen
- Prüfen: `rg -n "In-Memory Reference Playground|Hibernate SPI|JDBC/DataSource|Cross-source" README.md`

**Schnittstellen:**

- Nutzt: die acht ausführbaren Beobachtungen aus `JpaBaselineBehaviorTests`
- Liefert: dokumentierte Nullmessung und klare Abgrenzung gegenüber Routing, produktionsreifer API und Cross-Source-Joins

1. Belasse die bestehenden Abschnitte und Statusaussagen im README unverändert.
2. Ergänze direkt nach dem vorhandenen `Run`-Codeblock genau folgenden Abschnitt:

   ```markdown
   ## In-Memory Reference Playground

   The first executable experiment is a read-only reference baseline backed only by
   H2 in memory. It records regular Hibernate behavior before either routing approach
   changes it.

   | Behavior | Reference coverage |
   | --- | --- |
   | `EntityManager.find()` | entity loading and persistence-context identity |
   | JPQL | filtering and ordering |
   | Criteria API | association-path filtering |
   | Count and pagination | query result semantics |
   | Lazy loading | to-one and to-many associations |
   | `JOIN FETCH` | eager association loading for one query |

   This baseline is not an EntityMux implementation. Hibernate SPI and
   JDBC/DataSource routing remain separate research directions that must be compared
   against the same behavior matrix. Cross-source joins remain unsupported.
   ```

3. Führe `rg -n "In-Memory Reference Playground|Hibernate SPI|JDBC/DataSource|Cross-source" README.md` aus; erwarte Treffer für die Überschrift sowie alle drei Abgrenzungen.
4. Prüfe mit `git diff -- README.md`, dass keine vorhandene Status- oder Non-Goal-Aussage entfernt oder abgeschwächt wurde.
5. Übergib die README-Änderung, beide Prüfergebnisse und den Hinweis, dass der Abschnitt den aktuellen Versuchsstand und keine Produktzusage beschreibt.

## Aufgabe 5: Vollständige Baseline verifizieren

**Dateien:**

- Prüfen: `./mvnw clean verify`
- Prüfen: `git diff --check`
- Prüfen: `git status --short`

**Schnittstellen:**

- Nutzt: alle Dateien und Prüfverträge aus den Aufgaben 1 bis 4
- Liefert: reproduzierbar grüner Maven-Build und reviewbarer Arbeitsbaum als belastbare Ausgangsbasis für getrennte Hibernate-SPI- und JDBC/DataSource-Pläne

1. Führe `./mvnw clean verify` aus; erwarte `BUILD SUCCESS`, insgesamt 9 Tests, 0 Failures und 0 Errors.
2. Führe `git diff --check` aus; erwarte Exitcode 0 ohne Ausgabe.
3. Führe `git status --short` aus; erwarte ausschließlich die in diesem Plan genannten neuen beziehungsweise geänderten Dateien sowie die Planungsdatei selbst.
4. Vergleiche `target/surefire-reports/*.txt` mit der Verhaltensmatrix im README; erwarte einen erfolgreichen Kontexttest und acht erfolgreiche Baseline-Tests.
5. Übergib die vollständige Dateiliste, die Ausgaben aller Prüfungen und als nächsten fachlichen Planungspunkt zwei getrennte Spikes, die dieselbe Baseline jeweils für Hibernate SPI und JDBC/DataSource-Routing ausführen; nimm in dieser Aufgabe keine Routing-Implementierung vor.
