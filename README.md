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
