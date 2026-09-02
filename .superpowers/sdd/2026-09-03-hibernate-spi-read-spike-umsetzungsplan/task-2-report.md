# Aufgabe 2 – Implementierungsbericht

Status: DONE

## Implementierung

Erstellt wurden ausschließlich die drei testseitigen Klassen im Paket
`dev.feit.entitymux.experiment.hibernate.spi`:

- `RoutingLoadEventListener` routet nur `LoadEventListener.GET` für Hibernate-
  Entitäten mit der gemappten Klasse `Document` und einer `Long`-ID in den
  `AlternativeDocumentStore`.
- `HibernateLoadRoutingIntegrator` kopiert die vorhandenen `LOAD`-Listener
  vor dem Ersetzen und delegiert jeden nicht passenden Load-Fall in dieser
  ursprünglichen Reihenfolge.
- `HibernateSpiExperimentConfiguration` registriert den Integrator über den
  testseitigen Spring-Boot-`HibernatePropertiesCustomizer` und
  `JpaSettings.INTEGRATOR_PROVIDER`.

Die Kopplung ist absichtlich auf Hibernate ORM 7.4.5 beschränkt. Sie ist ein
testseitiger, H2-basierter Forschungs-Spike und keine zukünftige öffentliche
EntityMux-API. Es wurden weder Queries, Cross-Source-Joins noch Write-Routing
eingeführt.

## Prüfungen

- `./mvnw -q -DskipTests test-compile` — Exitcode 0.
- Die vier erwarteten Klassen einschließlich `AlternativeDocumentStore` sind
  unter `target/test-classes/dev/feit/entitymux/experiment/hibernate/spi`
  vorhanden.
- `rg -n "LoadType|GET|setListeners|INTEGRATOR_PROVIDER|HibernatePropertiesCustomizer" src/test/java/dev/feit/entitymux/experiment/hibernate/spi`
  — GET-Gate, Listener-Ersetzung und testseitige Spring-Boot-Anbindung
  nachgewiesen.
- Self-Review mit zeilenweisem Abgleich zum Task-Brief und
  `git diff --check` — keine Whitespace-Fehler; alle nicht passenden
  Load-Events werden über die kopierte Delegate-Liste weitergereicht.

## Bedenken

Keine funktionalen Bedenken für den begrenzten Spike. Die Maven-Ausgabe
enthält lediglich die bekannte Warnung zu einer veralteten
`sun.misc.Unsafe`-Methode; die Kompilierung war erfolgreich.
