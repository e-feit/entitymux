# Hibernate-SPI-Read-Spike – Implementierungsplan

> **Für Umsetzungsagenten:** Erzeuge vor der Umsetzung mit
> `2-custom-writing-workpackage-plan` einen Workpackage-Index und für jedes
> Workpackage eine eigene, selbstständige Markdown-Datei mit dem vollständigen
> relevanten Planinhalt. Übergib danach denselben Index an
> `3-custom-executing-next-workpackage` oder `4-custom-review-workpackage`; der
> jeweilige Skill ermittelt daraus automatisch das nächste Paket.

**Ziel:** Ein testseitig isolierter Hibernate-7.4-`LoadEventListener`-Spike routet ausschließlich direkte `EntityManager.find(Document.class, id)`-Lesezugriffe in eine zweite H2-In-Memory-Datenbank und dokumentiert mit ausführbaren Tests, welche Teile der gemeinsamen JPA-Verhaltensmatrix dadurch funktionieren oder bewusst nicht abgedeckt werden.

**Architektur:** Ein Spring-Boot-`HibernatePropertiesCustomizer` registriert einen Hibernate-`Integrator`, der die bestehende `LOAD`-Listenergruppe durch einen delegierenden `RoutingLoadEventListener` ersetzt. Der Listener erkennt `Document` weiterhin anhand der Hibernate-Entity-Metadaten, routet aber nur den `LoadEventListener.GET`-Pfad; alle anderen Entity-Typen und Load-Arten laufen unverändert durch die zuvor registrierten Hibernate-Listener. Ein zweiter, ausschließlich testseitiger Hibernate-`SessionFactory` materialisiert alternative Fixtures in `jdbc:h2:mem:entitymux-spi-alternative`; seine zurückgegebenen `Document`-Instanzen bleiben absichtlich detached, damit Persistence-Context-, Lazy-Loading-, Query- und Association-Grenzen des naiven Event-Ansatzes sichtbar werden statt durch zusätzliche interne Hibernate-Manipulation verdeckt zu werden.

**Technik:** Java 25, Spring Boot 4.1.1, Spring Data JPA, Hibernate ORM 7.4.5.Final, Jakarta Persistence, H2 2.4.240, JUnit 5, AssertJ, Maven Wrapper

## Quellen

- Nutzeranforderung: „die ersten Untersuchungen sind gelaufen und das Ergebnis ist dem Entwickler präsentiert. Nutze zunächst ausschließlich die In-Memory Datenbank.“
- Architekturskizze: `/Users/eugen/AI-Markdown/entitymux-architecture-summary.md`
- Projektanweisungen: `/private/tmp/entitymux-hibernate-spi-read-spike/AGENTS.md`
- In-Memory-Referenzplan: `/private/tmp/entitymux-hibernate-spi-read-spike/2026-09-02-in-memory-referenzplayground-umsetzungsplan.md`
- Referenztests: `/private/tmp/entitymux-hibernate-spi-read-spike/src/test/java/dev/feit/entitymux/experiment/baseline/JpaBaselineBehaviorTests.java`
- Spring Boot 4.1.1 API, `HibernatePropertiesCustomizer`: `https://docs.spring.io/spring-boot/api/java/org/springframework/boot/hibernate/autoconfigure/HibernatePropertiesCustomizer.html`
- Spring Boot Data-Access-Dokumentation zur erweiterten Hibernate-Konfiguration: `https://docs.spring.io/spring-boot/4.0/how-to/data-access.html`
- Hibernate-7.4.5-Quellcode, `SessionImpl.fireLoadNoChecks`: `/Users/eugen/.m2/repository/org/hibernate/orm/hibernate-core/7.4.5.Final/hibernate-core-7.4.5.Final-sources.jar!/org/hibernate/internal/SessionImpl.java`
- Hibernate-7.4.5-Quellcode, `EventListenerGroupImpl`: `/Users/eugen/.m2/repository/org/hibernate/orm/hibernate-core/7.4.5.Final/hibernate-core-7.4.5.Final-sources.jar!/org/hibernate/event/service/internal/EventListenerGroupImpl.java`
- Hibernate-7.4.5-Quellcode, `DefaultLoadEventListener`: `/Users/eugen/.m2/repository/org/hibernate/orm/hibernate-core/7.4.5.Final/hibernate-core-7.4.5.Final-sources.jar!/org/hibernate/event/internal/DefaultLoadEventListener.java`

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

## Aufgabe 1: Alternative Document-Fixtures in einer zweiten H2-In-Memory-Datenbank

**Dateien:**

- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/AlternativeDocumentStore.java`
- Prüfen: `./mvnw -q -DskipTests test-compile`

**Schnittstellen:**

- Nutzt: `User`, `Document`, Hibernate `Configuration`, `SessionFactory` und ausschließlich die URL `jdbc:h2:mem:entitymux-spi-alternative`
- Liefert: package-private `AlternativeDocumentStore.create()`, `find(Long)`, `loadCount()`, `resetLoadCount()` und `close()` sowie alternative Titel mit dem Präfix `SPI `

1. Lege das Paket `dev.feit.entitymux.experiment.hibernate.spi` ausschließlich unter `src/test/java` an; füge keine Main-Code-API und keine Maven-Abhängigkeit hinzu.
2. Erstelle `AlternativeDocumentStore.java` mit genau folgendem Inhalt:

   ```java
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
   ```

3. Führe `./mvnw -q -DskipTests test-compile` aus; erwarte Exitcode 0 und `target/test-classes/dev/feit/entitymux/experiment/hibernate/spi/AlternativeDocumentStore.class`.
4. Prüfe mit `rg -n "jdbc:h2:mem:|createNativeMutationQuery|find\(Document.class" src/test/java/dev/feit/entitymux/experiment/hibernate/spi/AlternativeDocumentStore.java`, dass nur die alternative H2-URL, fünf Fixture-Inserts und ein read-only `Document`-Load vorhanden sind.
5. Übergib die neue Store-Datei, beide Prüfergebnisse und den Hinweis, dass die fünf Inserts ausschließlich Test-Fixtures materialisieren und keine Write-Routing-Funktion darstellen.

## Aufgabe 2: Hibernate-LOAD-Listener testseitig und delegierend registrieren

**Dateien:**

- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/RoutingLoadEventListener.java`
- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateLoadRoutingIntegrator.java`
- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateSpiExperimentConfiguration.java`
- Prüfen: `./mvnw -q -DskipTests test-compile`

**Schnittstellen:**

- Nutzt: `AlternativeDocumentStore`, Spring Boots `HibernatePropertiesCustomizer`, `JpaSettings.INTEGRATOR_PROVIDER`, Hibernate `Integrator`, `EventListenerRegistry`, `EventType.LOAD` und `LoadEventListener.GET`
- Liefert: einen nur im Testkontext importierbaren `HibernateSpiExperimentConfiguration`; direkte GET-Loads für `Document` laufen zum alternativen Store, alle anderen Load-Events in ursprünglicher Reihenfolge zu den bestehenden Listenern

1. Erstelle `RoutingLoadEventListener.java` mit genau folgendem Inhalt:

   ```java
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
   ```

2. Erstelle `HibernateLoadRoutingIntegrator.java` mit genau folgendem Inhalt:

   ```java
   package dev.feit.entitymux.experiment.hibernate.spi;

   import org.hibernate.boot.Metadata;
   import org.hibernate.boot.spi.BootstrapContext;
   import org.hibernate.engine.spi.SessionFactoryImplementor;
   import org.hibernate.event.service.spi.EventListenerGroup;
   import org.hibernate.event.service.spi.EventListenerRegistry;
   import org.hibernate.event.spi.EventType;
   import org.hibernate.event.spi.LoadEventListener;
   import org.hibernate.integrator.spi.Integrator;

   import java.util.List;
   import java.util.stream.StreamSupport;

   final class HibernateLoadRoutingIntegrator implements Integrator {

       private final AlternativeDocumentStore alternativeStore;

       HibernateLoadRoutingIntegrator(AlternativeDocumentStore alternativeStore) {
           this.alternativeStore = alternativeStore;
       }

       @Override
       public void integrate(
               Metadata metadata,
               BootstrapContext bootstrapContext,
               SessionFactoryImplementor sessionFactory) {
           EventListenerRegistry registry = sessionFactory.getEventListenerRegistry();
           EventListenerGroup<LoadEventListener> loadListeners =
                   registry.getEventListenerGroup(EventType.LOAD);
           List<LoadEventListener> delegates = copyListeners(loadListeners);

           registry.setListeners(
                   EventType.LOAD,
                   new RoutingLoadEventListener(alternativeStore, delegates));
       }

       @SuppressWarnings("deprecation")
       private static List<LoadEventListener> copyListeners(
               EventListenerGroup<LoadEventListener> listeners) {
           return StreamSupport.stream(listeners.listeners().spliterator(), false)
                   .toList();
       }
   }
   ```

3. Erstelle `HibernateSpiExperimentConfiguration.java` mit genau folgendem Inhalt:

   ```java
   package dev.feit.entitymux.experiment.hibernate.spi;

   import org.hibernate.jpa.boot.spi.IntegratorProvider;
   import org.hibernate.jpa.boot.spi.JpaSettings;
   import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
   import org.springframework.boot.test.context.TestConfiguration;
   import org.springframework.context.annotation.Bean;

   import java.util.List;

   @TestConfiguration(proxyBeanMethods = false)
   class HibernateSpiExperimentConfiguration {

       @Bean(destroyMethod = "close")
       AlternativeDocumentStore alternativeDocumentStore() {
           return AlternativeDocumentStore.create();
       }

       @Bean
       HibernatePropertiesCustomizer hibernateLoadRouting(
               AlternativeDocumentStore alternativeStore) {
           IntegratorProvider provider = () -> List.of(
                   new HibernateLoadRoutingIntegrator(alternativeStore));
           return properties -> properties.put(JpaSettings.INTEGRATOR_PROVIDER, provider);
       }
   }
   ```

4. Führe `./mvnw -q -DskipTests test-compile` aus; erwarte Exitcode 0 und vier kompilierte SPI-Experimentklassen einschließlich `AlternativeDocumentStore` unter `target/test-classes/dev/feit/entitymux/experiment/hibernate/spi`.
5. Prüfe mit `rg -n "LoadType|GET|setListeners|INTEGRATOR_PROVIDER|HibernatePropertiesCustomizer" src/test/java/dev/feit/entitymux/experiment/hibernate/spi`, dass nur `GET` für `Document` geroutet wird und die Spring-Boot-Anbindung im Testpaket bleibt.
6. Übergib die drei neuen Integrationsdateien, beide Prüfergebnisse und die explizite Kopplung an Hibernate ORM 7.4.5; stelle diese Klassen nicht als zukünftige öffentliche EntityMux-API dar.

## Aufgabe 3: Direkten find()-Erfolg und Persistence-Context-Grenzen beobachten

**Dateien:**

- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateSpiFindBehaviorTests.java`
- Prüfen: `./mvnw -q -Dtest=HibernateSpiFindBehaviorTests test`

**Schnittstellen:**

- Nutzt: `HibernateSpiExperimentConfiguration`, `AlternativeDocumentStore`, primäre Fixtures aus `data.sql` und alternative Fixtures mit `SPI `-Titeln
- Liefert: fünf getrennte Beobachtungen für geroutetes `Document.find`, unverändertes `User.find`, fehlende primäre Managed-Zugehörigkeit, fehlende Identity-Garantie und fehlschlagendes Lazy-To-One auf dem detached Ergebnis

1. Erstelle `HibernateSpiFindBehaviorTests.java` mit genau folgendem Inhalt:

   ```java
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
   ```

2. Führe `./mvnw -q -Dtest=HibernateSpiFindBehaviorTests test` aus; erwarte Exitcode 0 und `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` in `target/surefire-reports/dev.feit.entitymux.experiment.hibernate.spi.HibernateSpiFindBehaviorTests.txt`.
3. Prüfe mit `rg -n "void (find|routed|repeated|lazy)" src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateSpiFindBehaviorTests.java`, dass alle fünf benannten Beobachtungen getrennt bleiben.
4. Übergib die Testdatei, beide Prüfergebnisse und die Bewertung: Der Hook behält Entity-Semantik für direkten `find()`-Zugriff, liefert in dieser Form aber kein vom primären Persistence Context verwaltetes Objekt und erhält weder Identity noch Lazy-To-One-Verhalten.

## Aufgabe 4: JPQL-, Criteria-, Pagination- und Association-Grenzen beobachten

**Dateien:**

- Erstellen: `src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateSpiQueryBoundaryTests.java`
- Prüfen: `./mvnw -q -Dtest=HibernateSpiQueryBoundaryTests test`

**Schnittstellen:**

- Nutzt: denselben `GET`-Listener, primäre `data.sql`-Fixtures ohne `SPI `-Präfix und den `AlternativeDocumentStore.loadCount()` als Routing-Indikator
- Liefert: fünf getrennte Beobachtungen, dass JPQL, Criteria API, Count/Pagination, lazy To-Many und `JOIN FETCH` den direkten GET-Hook nicht verwenden und weiterhin gegen die primäre In-Memory-Datenbank laufen

1. Erstelle `HibernateSpiQueryBoundaryTests.java` mit genau folgendem Inhalt:

   ```java
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
   ```

2. Führe `./mvnw -q -Dtest=HibernateSpiQueryBoundaryTests test` aus; erwarte Exitcode 0 und `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` in `target/surefire-reports/dev.feit.entitymux.experiment.hibernate.spi.HibernateSpiQueryBoundaryTests.txt`.
3. Prüfe mit `rg -n "void (jpql|criteria|count|lazy|joinFetch)" src/test/java/dev/feit/entitymux/experiment/hibernate/spi/HibernateSpiQueryBoundaryTests.java`, dass alle fünf Query- und Association-Grenzen getrennt beobachtet werden.
4. Übergib die Testdatei, beide Prüfergebnisse und die Bewertung: Ein `LoadEventListener.GET` ist keine allgemeine Query-, Collection- oder Fetch-Routing-Schnittstelle; die unveränderten primären Ergebnisse sind erwartete Evidenz und kein vermeintlicher Erfolg des Routers.

## Aufgabe 5: Experimentergebnis dokumentieren und vollständig verifizieren

**Dateien:**

- Ändern: `README.md:20-65` – veraltete Aussage „No entity-routing functionality“ präzisieren und SPI-Ergebnismatrix ergänzen
- Prüfen: `./mvnw clean verify`
- Prüfen: `git diff --check`

**Schnittstellen:**

- Nutzt: fünf Find-Beobachtungen, fünf Query-/Association-Beobachtungen und die bestehende achtteilige Referenzbaseline
- Liefert: ehrliche SPI-Bewertung ohne Produktzusage sowie einen Gesamtbuild mit 19 erfolgreichen Tests

1. Ersetze im Abschnitt `Non-Goals / Current Scope` genau die Zeile
   `- No entity-routing functionality has been implemented yet.` durch:

   ```markdown
   - The test-only Hibernate SPI experiment routes direct `EntityManager.find(Document.class, id)` calls only; it is not a general EntityMux implementation.
   ```

2. Ergänze nach dem bestehenden Abschnitt `In-Memory Reference Playground` genau folgenden Abschnitt:

   ```markdown
   ## Hibernate SPI Read Spike

   The first routing experiment registers a test-only Hibernate `LoadEventListener`
   and routes `GET` events for `Document` to a second H2 in-memory database. Existing
   Hibernate load listeners still handle every other entity type and load mode.

   | Behavior | Observed result |
   | --- | --- |
   | `EntityManager.find(Document.class, id)` | routed by entity type |
   | `EntityManager.find(User.class, id)` | unchanged primary load |
   | Primary persistence-context membership | routed document is detached |
   | Repeated `find()` identity | not preserved |
   | Lazy to-one association | cannot initialize after the alternative session closes |
   | JPQL and Criteria API | bypass the direct `GET` listener |
   | Count and pagination | remain on the primary store |
   | Lazy to-many collection | remains on the primary store |
   | `JOIN FETCH` | remains on the primary store |

   This result demonstrates that Hibernate load events retain entity semantics, but
   a naive `GET` listener is not a transparent routing architecture. It does not
   preserve the primary persistence context, lazy associations, arbitrary queries,
   collections, or fetch joins. The JDBC/DataSource direction remains a separate
   research spike; cross-source joins and write routing remain unsupported.
   ```

3. Führe `rg -n "Hibernate SPI Read Spike|routed document is detached|JPQL and Criteria API|write routing remain unsupported" README.md` aus; erwarte Treffer für Überschrift, Persistence-Context-Grenze, Query-Grenze und Scope-Abgrenzung.
4. Führe `./mvnw clean verify` aus; erwarte `BUILD SUCCESS`, insgesamt 19 Tests, 0 Failures und 0 Errors.
5. Führe `git diff --check` aus; erwarte Exitcode 0 ohne Ausgabe.
6. Führe `git status --short` aus; erwarte ausschließlich die in diesem Plan genannten neuen beziehungsweise geänderten Dateien sowie diese Planungsdatei.
7. Übergib die vollständige Dateiliste, die Ausgaben aller Prüfungen und als nächsten eigenständigen Planungspunkt den JDBC/DataSource-Read-Spike gegen dieselbe In-Memory-Referenzmatrix; implementiere in dieser Aufgabe weder JDBC-Routing noch Cross-Source-Joins oder Mutationen.
