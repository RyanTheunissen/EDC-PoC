package org.example.edc.dataplane.postgres.pipeline;


public interface PostgresDataSchema {
    // dataAddress.type / dataDestination.type
    String TYPE = "JdbcData"; // matches your README JSONs


    // Common (source + sink)
    String[] JDBC_URL_KEYS = { "jdbcUrl", "edc:jdbc:url", "jdbc:url" };
    String[] USER_KEYS = { "user", "edc:jdbc:user", "jdbc:user" };
    String[] PASSWORD_KEYS = { "password", "edc:jdbc:password" }; // discouraged; prefer vault
    String[] PASSWORD_KEY_KEYS = { "passwordKey", "edc:secret:key", "secretKey" }; // vault key


    // Source
    String[] TABLE_KEYS = { "table", "edc:sql:table" };
    String[] QUERY_KEYS = { "query", "edc:sql:query" };
    String[] CSV_HEADER_KEYS = { "csvHeader" };


    // Sink
    String[] TARGET_TABLE_KEYS = { "targetTable", "edc:sql:targetTable" };
    String[] TRUNCATE_KEYS = { "truncate" };
    String[] CREATE_DDL_KEYS = { "createTableDdl", "edc:sql:ddl" };
}