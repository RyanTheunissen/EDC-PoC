package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;

class PostgresSourceFactory implements DataSourceFactory {
    private static final String EDC_NS = "https://w3id.org/edc/v0.0.1/ns/";
    private final Monitor monitor;

    PostgresSourceFactory(Monitor monitor) { this.monitor = monitor; }

    @Override
    public String supportedType() { return "postgres"; }

    @Override
    public DataSource createSource(DataFlowStartMessage message) {
        DataAddress src = message.getSourceDataAddress();
        var cfg = PgCfg.fromAddress(src);
        monitor.info("PostgresSource[" + message.getProcessId() + "] cfg: jdbcUrl=" + cfg.jdbcUrl());
        return new PostgresSource(cfg, monitor, message.getProcessId());
    }

    @Override
    public Result<Void> validateRequest(DataFlowStartMessage message) {
        var src = message.getSourceDataAddress();
        if (src == null) return Result.failure("Missing source DataAddress");
        if (!"postgres".equalsIgnoreCase(src.getType())) {
            return Result.failure("Unsupported source type: " + src.getType());
        }

        var p = src.getProperties(); // Map<String, Object> with plain or JSON-LD-expanded keys
        var miss = new java.util.ArrayList<String>();
        require(p, "jdbcUrl", miss);
        require(p, "user", miss);
        require(p, "password", miss);
        require(p, "sql", miss);

        // Always log the actual keys we received for quick diagnosis
        monitor.severe("PostgresSource validate: type=" + src.getType() + ", props.keys=" + p.keySet());

        if (!miss.isEmpty()) {
            return Result.failure("Source address missing: " + String.join(", ", miss)
                    + " | type=" + src.getType()
                    + " | props.keys=" + p.keySet());
        }

        // Fast connectivity probe (3s) using resolved values (plain or namespaced)
        try {
            String jdbcUrl = getStr(p, "jdbcUrl");
            String user = getStr(p, "user");
            String password = getStr(p, "password");

            java.sql.DriverManager.setLoginTimeout(3);
            try (var c = java.sql.DriverManager.getConnection(jdbcUrl, user, password)) {
                // ok
            }
        } catch (Exception e) {
            return Result.failure("Source DB connection failed: " + e.getMessage());
        }
        return Result.success();
    }

    /* ===== helpers that understand JSON-LD-expanded keys ===== */

    private static void require(Map<String, Object> p, String local, java.util.List<String> miss) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        if (v == null || v.toString().trim().isEmpty()) miss.add(local);
    }

    private static String getStr(Map<String, Object> p, String local) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        return v == null ? null : v.toString();
    }
}
