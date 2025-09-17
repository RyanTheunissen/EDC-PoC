package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

class PostgresSourceFactory implements DataSourceFactory {
    private final Monitor monitor;

    PostgresSourceFactory(Monitor monitor) { this.monitor = monitor; }

    @Override
    public String supportedType() { return "postgres"; }

    @Override
    public DataSource createSource(DataFlowStartMessage message) {
        var cfg = PgCfg.fromAddress(message.getSourceDataAddress());
        monitor.info("PostgresSource[" + message.getProcessId() + "] cfg: jdbcUrl=" + cfg.jdbcUrl());
        return new PostgresSource(cfg, monitor, message.getProcessId());
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage message) {
        var src = message.getSourceDataAddress();
        if (src == null) return Result.failure("Missing source DataAddress");
        if (!"postgres".equalsIgnoreCase(src.getType())) {
            return Result.failure("Unsupported source type: " + src.getType());
        }

        monitor.info("PostgresSource validate: type=" + src.getType() + ", props.keys=" + src.getProperties().keySet());

        final PgCfg cfg;
        try {
            cfg = PgCfg.fromAddress(src); // resolves json-LD and plain keys
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }

        // Ensure SQL is present
        var sql = cfg.sql();
        if (sql == null || sql.trim().isEmpty()) {
            return Result.failure("Source address missing: sql");
        }

        // Connectivity probe + sanity query
        try (var c = java.sql.DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password());
             var stmt = c.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {

            if (!rs.next()) {
                return Result.failure("Source DB connectivity check failed: SELECT 1 returned no rows");
            }

        } catch (Exception e) {
            return Result.failure("Source DB connection failed: " + e.getMessage());
        }

        return Result.success();
    }
}
