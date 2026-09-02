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
- The test-only Hibernate SPI experiment routes direct `EntityManager.find(Document.class, id)` calls only; it is not a general EntityMux implementation.
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
