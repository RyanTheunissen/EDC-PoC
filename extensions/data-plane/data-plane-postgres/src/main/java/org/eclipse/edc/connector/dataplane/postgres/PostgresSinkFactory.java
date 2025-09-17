package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;

class PostgresSinkFactory implements DataSinkFactory {
    private static final String EDC_NS = "https://w3id.org/edc/v0.0.1/ns/";
    private final Monitor monitor;

    PostgresSinkFactory(Monitor monitor) { this.monitor = monitor; }

    @Override
    public String supportedType() { return "postgres"; }

    @Override
    public DataSink createSink(DataFlowStartMessage message) {
        var dest = message.getDestinationDataAddress();
        var cfg = PgCfg.fromAddress(dest); // PgCfg already handles namespaced keys
        monitor.info("PostgresSink[" + message.getProcessId() + "] cfg: jdbcUrl=" + cfg.jdbcUrl() + ", table=" + cfg.table());
        return new PostgresSink(cfg, monitor, message.getProcessId());
    }

    @Override
    public Result<Void> validateRequest(DataFlowStartMessage message) {
        var dest = message.getDestinationDataAddress();
        if (dest == null) return Result.failure("Missing destination DataAddress");
        if (!"postgres".equalsIgnoreCase(dest.getType())) {
            return Result.failure("Unsupported destination type: " + dest.getType());
        }

        var p = dest.getProperties();
        var missing = new java.util.ArrayList<String>();
        require(p, "jdbcUrl", missing);
        require(p, "user", missing);
        require(p, "password", missing);
        require(p, "table", missing);

        // Always log received keys for quick diagnosis
        monitor.severe("PostgresSink validate: type=" + dest.getType() + ", props.keys=" + p.keySet());

        if (!missing.isEmpty()) {
            return Result.failure("Destination address missing: " + String.join(", ", missing)
                    + " | type=" + dest.getType()
                    + " | props.keys=" + p.keySet());
        }

        // Fast connectivity + table existence check using resolved values
        try {
            String jdbc = getStr(p, "jdbcUrl");
            String user = getStr(p, "user");
            String pass = getStr(p, "password");
            String table = getStr(p, "table");

            java.sql.DriverManager.setLoginTimeout(3);
            try (var c = java.sql.DriverManager.getConnection(jdbc, user, pass)) {
                String schema = "public", name = table;
                var parts = table.split("\\.", 2);
                if (parts.length == 2) { schema = parts[0]; name = parts[1]; }
                try (var rs = c.getMetaData().getTables(null, schema, name, null)) {
                    if (!rs.next()) {
                        return Result.failure("Destination table not found: " + table + " (schema=" + schema + ")");
                    }
                }
            }
        } catch (Exception e) {
            return Result.failure("Destination DB connection failed: " + e.getMessage());
        }

        return Result.success();
    }

    /* === helpers that understand JSON-LD-expanded keys === */

    private static void require(Map<String, Object> p, String local, java.util.List<String> missing) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        if (v == null || v.toString().trim().isEmpty()) missing.add(local);
    }

    private static String getStr(Map<String, Object> p, String local) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        return v == null ? null : v.toString();
    }
}
