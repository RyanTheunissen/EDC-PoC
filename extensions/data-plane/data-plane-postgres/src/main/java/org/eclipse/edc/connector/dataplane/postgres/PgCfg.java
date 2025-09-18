package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;


final class PgCfg {
    private static final String EDC_NS = "https://w3id.org/edc/v0.0.1/ns/";

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String sql;
    private final String table;
    private final boolean truncateBeforeLoad;
    private final boolean createIfMissing;
    private final String columns;
    private final String columnsDdl;

    private PgCfg(String jdbcUrl,
                  String user,
                  String password,
                  String sql,
                  String table,
                  boolean truncateBeforeLoad,
                  boolean createIfMissing,
                  String columns,
                  String columnsDdl) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.sql = sql;
        this.table = table;
        this.truncateBeforeLoad = truncateBeforeLoad;
        this.createIfMissing = createIfMissing;
        this.columns = columns;
        this.columnsDdl = columnsDdl;
    }

    static PgCfg fromAddress(DataAddress address) {
        Map<String, Object> p = address.getProperties();

        return new PgCfg(
                require(p, "jdbcUrl"),
                require(p, "user"),
                require(p, "password"),
                // source-only
                get(p, "sql"),
                // sink-only
                get(p, "table"),
                Boolean.parseBoolean(orDefault(p, "truncateBeforeLoad")),
                // auto-create
                Boolean.parseBoolean(orDefault(p, "createIfMissing")),
                get(p, "columns"),
                get(p, "columnsDdl")
        );
    }

    private static String require(Map<String, Object> p, String local) {
        String v = get(p, local);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing DataAddress property: '" + local + "' (or '" + EDC_NS + local + "')");
        }
        return v;
    }

    private static String get(Map<String, Object> p, String local) {
        Object v = p.get(local);
        if (v == null) v = p.get(EDC_NS + local);
        return v == null ? null : v.toString();
    }

    private static String orDefault(Map<String, Object> p, String local) {
        String v = get(p, local);
        return v == null ? "false" : v;
    }

    String jdbcUrl() { return jdbcUrl; }
    String user() { return user; }
    String password() { return password; }

    String sql() { return sql; }

    String table() { return table; }
    boolean truncateBeforeLoad() { return truncateBeforeLoad; }

    boolean createIfMissing() { return createIfMissing; }
    String columns() { return columns; }
    String columnsDdl() { return columnsDdl; }
}
