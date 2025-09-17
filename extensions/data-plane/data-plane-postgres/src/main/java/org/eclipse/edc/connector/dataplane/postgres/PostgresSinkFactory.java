package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;


class PostgresSinkFactory implements DataSinkFactory {
    private final Monitor monitor;

    PostgresSinkFactory(Monitor monitor) { this.monitor = monitor; }

    @Override
    public String supportedType() { return "postgres"; }

    @Override
    public DataSink createSink(DataFlowStartMessage message) {
        var dest = message.getDestinationDataAddress();
        var cfg = PgCfg.fromAddress(dest);
        monitor.info("PostgresSink[" + message.getProcessId() + "] cfg: jdbcUrl=" + cfg.jdbcUrl() + ", table=" + cfg.table());
        return new PostgresSink(cfg, monitor, message.getProcessId());
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage message) {
        var dest = message.getDestinationDataAddress();
        if (dest == null) return Result.failure("Missing destination DataAddress");
        if (!"postgres".equalsIgnoreCase(dest.getType())) {
            return Result.failure("Unsupported destination type: " + dest.getType());
        }

        monitor.info("PostgresSink validate: type=" + dest.getType() + ", props.keys=" + dest.getProperties().keySet());

        PgCfg cfg;
        try {
            cfg = PgCfg.fromAddress(dest); // resolves json-LD and plain keys
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }

        // check table is present
        var table = cfg.table();
        if (table == null || table.trim().isEmpty()) {
            return Result.failure("Destination address missing: table");
        }

        // Connectivity check
        try {
            java.sql.DriverManager.setLoginTimeout(3);
            try (var c = java.sql.DriverManager.getConnection(cfg.jdbcUrl(), cfg.user(), cfg.password())) {
                String schema = "public", name = cfg.table();
                var parts = name.split("\\.", 2);
                if (parts.length == 2) { schema = parts[0]; name = parts[1]; }
                try (var rs = c.getMetaData().getTables(null, schema, name, null)) {
                    if (!rs.next()) {
                        return Result.failure("Destination table not found: " + cfg.table() + " (schema=" + schema + ")");
                    }
                }
            }
        } catch (Exception e) {
            return Result.failure("Destination DB connection failed: " + e.getMessage());
        }

        return Result.success();
    }
}
