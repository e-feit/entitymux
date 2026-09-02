# AGENTS.md

## Project Goal

EntityMux explores how individual JPA/Hibernate entities can be routed to alternative persistence sources.

```text
User.class      -> real DataSource
Document.class  -> alternative provider
```

The long-term goal is to make the integration as transparent as possible to application code.

## Project Status

The project is currently an experimental playground. Priorities are:

1. investigate technical feasibility
2. understand Hibernate and JPA behavior
3. identify architectural boundaries
4. conduct small, isolated spikes
5. develop stable APIs only after those steps

Do not imply production-ready stability.

## Current Scope

Currently in scope:

- Spring Boot
- Hibernate
- JPA / Jakarta Persistence
- Java 25
- Maven

Explicitly out of scope:

- WildFly
- other JPA providers
- a production-ready API
- write routing
- distributed data sources
- Cross-Source-Joins
- complete SQL interpretation

## Architecture Principles

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

## Coding Rules

- Respect the existing structure and prefer small changes.
- Avoid unnecessary refactoring.
- Add dependencies only when there is a clear, current need.
- Use Spring and Hibernate internal APIs deliberately and encapsulate them in isolation.
- Write tests for observed behavior.
- Clearly separate technical experiments from a future public API.
- Do not imply production-ready stability.
- Do not adopt an implementation solely because it works with `findById()`.
- Consider behavior involving JPQL, associations, lazy loading, and the persistence context.

## Build

Before completing any change, run at least:

```bash
./mvnw clean verify
```
