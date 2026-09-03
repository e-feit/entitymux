# EntityMux (Entity Multiplexer)

EntityMux is an experimental playground prototype for entity-level persistence routing in JPA/Hibernate applications.

The long-term goal is to route selected entities to alternative persistence sources while other entities continue to use the configured `DataSource`. This routing should eventually be as transparent as possible to application code.

```text
User      -> configured database
Document  -> alternative EntityMux provider
```

## Status

EntityMux is a playground and prototype. Its API and architecture may change completely. It is not intended for production use.

## Goals

- Explore entity-level persistence routing with JPA and Hibernate.
- Keep regular persistence unchanged for entities that are not routed.
- Investigate a transparent integration model for application code.

## Non-Goals / Current Scope

- Spring Boot is the only supported runtime for now; WildFly may be considered later.
- The test-only Hibernate SPI experiments route selected direct `EntityManager.find(Document.class, id)` calls only; the separate H2-materialization spike projects provider data into H2 during test setup. Neither is a general EntityMux implementation.
- Production readiness and stable APIs are not current goals.

## Technology

- Java 25
- Maven
- Spring Boot
- Spring Data JPA / Hibernate
- H2 for local development and tests

## Build

```bash
./mvnw clean verify
```

## Run

```bash
./mvnw spring-boot:run
```

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

## Hibernate SPI Read Spike

The first routing experiment registers a test-only Hibernate `LoadEventListener`
and routes `GET` events for `Document` to a second H2 in-memory database. Existing
Hibernate load listeners still handle every other entity type and load mode.

| Behavior | Observed result |
| --- | --- |
| `EntityManager.find(Document.class, id)` | routed by entity type |
| `EntityManager.find(User.class, id)` | unchanged primary load |
| Primary persistence-context membership | routed document is not managed |
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

## Synthetic Document Provider Spike

The second Hibernate SPI experiment keeps `User` and regular `Document` rows
in the primary H2 database while generating the reserved document IDs `1000`
and `1001` on demand. The provider returns plain data, and the test-only load
listener creates mapped `Document` instances through Hibernate metadata. A
`StatementInspector` verifies the executed SQL without interpreting it.

| Behavior | Observed result |
| --- | --- |
| `find(Document.class, 1000L / 1001L)` | generated on demand without executing SQL |
| `find(User.class, 1L)` | loaded from the primary H2 database |
| `find(Document.class, 10L)` | loaded from the primary H2 database |
| Primary persistence-context membership | synthetic document is not managed |
| Repeated synthetic `find()` identity | not preserved |
| Synthetic to-one owner | resolves `User#1` from H2 while the primary session is open |
| JPQL and Criteria API | do not include synthetic documents |
| Count and pagination | include primary documents only |
| Lazy to-many collection | includes primary documents only |
| `JOIN FETCH` | cannot discover a synthetic document |

This proves that a direct entity load can be supplied entirely on the fly while
other data continues to come from H2. It does not provide transparent query
federation: synthetic documents are absent from arbitrary queries, collections,
counts, pagination, and fetch joins. The experiment remains read-only and
test-only; cross-source joins and write routing remain unsupported.

## H2 Materialization Spike

The third experiment lets a test provider generate two document records and
inserts them into the primary H2 database when the test application context is
ready. The behavior tests then use neither a Hibernate load hook nor custom SQL
handling: Hibernate reads the resulting five H2 document rows normally.

| Behavior | Observed result |
| --- | --- |
| `EntityManager.find(Document.class, 1000L)` | regular managed H2 entity |
| Repeated `find()` identity | preserved by the primary persistence context |
| JPQL and Criteria API | include materialized documents |
| Count and pagination | include all five H2 documents |
| Lazy to-one owner | loads normally from primary H2 |
| Lazy to-many collection | includes the materialized document |
| `JOIN FETCH` | fetches the materialized document and its H2 owner |

This shows that provider data can be projected into H2 while retaining ordinary
JPA read behavior. The provider data have physically become H2 rows; this is
not SQL-free on-the-fly loading or query federation. The setup-time inserts are
only a test fixture, not write routing, and say nothing about production
synchronization, refresh, or transaction isolation. Cross-source joins and
write routing remain unsupported.
