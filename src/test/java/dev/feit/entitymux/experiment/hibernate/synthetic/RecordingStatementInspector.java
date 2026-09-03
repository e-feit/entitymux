package dev.feit.entitymux.experiment.hibernate.synthetic;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingStatementInspector implements StatementInspector {

    private final List<String> statements = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        statements.add(sql);
        return sql;
    }

    List<String> statements() {
        return List.copyOf(statements);
    }

    void reset() {
        statements.clear();
    }
}
