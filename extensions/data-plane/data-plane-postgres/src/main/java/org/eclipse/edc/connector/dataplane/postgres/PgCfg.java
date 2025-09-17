package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

final class PgCfg {
    private static final String EDC_NS = "https://w3id.org/edc/v0.0.1/ns/";

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String sql;      // source only
    private final String table;    // sink only
    private final boolean truncateBeforeLoad;

    private PgCfg(String jdbcUrl, String user, String password, String sql, String table, boolean truncateBeforeLoad) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.sql = sql;
        this.table = table;
        this.truncateBeforeLoad = truncateBeforeLoad;
    }

    static PgCfg fromAddress(DataAddress a) {
        Map<String, Object> p = a.getProperties();
        return new PgCfg(
                require(p, "jdbcUrl"),
                require(p, "user"),
                require(p, "password"),
                get(p, "sql"),
                get(p, "table"),
                Boolean.parseBoolean(orDefault(p, "truncateBeforeLoad", "false"))
        );
    }

    /* === helpers that understand JSON-LD-expanded keys === */

    private static String require(Map<String, Object> p, String local) {
        var v = get(p, local);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing DataAddress property: " + local + " (or " + EDC_NS + local + ")");
        }
        return v;
    }

    private static String get(Map<String, Object> p, String local) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        return v == null ? null : v.toString();
    }

    private static String orDefault(Map<String, Object> p, String local, String def) {
        var v = get(p, local);
        return v == null ? def : v;
    }

    String jdbcUrl() { return jdbcUrl; }
    String user() { return user; }
    String password() { return password; }
    String sql() { return sql; }
    String table() { return table; }
    boolean truncateBeforeLoad() { return truncateBeforeLoad; }
}
