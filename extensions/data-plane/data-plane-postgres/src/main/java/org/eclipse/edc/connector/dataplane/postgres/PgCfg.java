package org.eclipse.edc.connector.dataplane.postgres;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

final class PgCfg {
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
        Map<String, Object> props = a.getProperties();
        return new PgCfg(
                require(props, "jdbcUrl"),
                require(props, "user"),
                require(props, "password"),
                asString(props.get("sql")),
                asString(props.get("table")),
                Boolean.parseBoolean(asString(props.getOrDefault("truncateBeforeLoad", "false")))
        );
    }

    private static String require(Map<String, Object> props, String key) {
        var v = props.get(key);
        if (v == null || asString(v).trim().isEmpty()) {
            throw new IllegalArgumentException("Missing DataAddress property: " + key);
        }
        return asString(v);
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    String jdbcUrl() { return jdbcUrl; }
    String user() { return user; }
    String password() { return password; }
    String sql() { return sql; }
    String table() { return table; }
    boolean truncateBeforeLoad() { return truncateBeforeLoad; }
}
