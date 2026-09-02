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
- No entity-routing functionality has been implemented yet.
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
