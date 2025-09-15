package org.eclipse.edc.poc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource that executes read-only SQL queries and returns result rows as JSON.
 * This is intentionally simplistic and for PoC usage only. It performs some basic guards
 * to only allow single SELECT statements without semicolons or dangerous keywords.
 */
@Path("/sql")
public class SqlQueryResource {

    private final SqlQueryExtension.ConnectionSupplier connectionSupplier;
    private final Monitor monitor;

    public SqlQueryResource(SqlQueryExtension.ConnectionSupplier connectionSupplier, Monitor monitor) {
        this.connectionSupplier = connectionSupplier;
        this.monitor = monitor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("q") String query) {
        if (query == null || query.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "missing 'q' query parameter"))
                    .build();
        }
        var normalized = query.trim().toLowerCase();
        if (!normalized.startsWith("select ") || normalized.contains(";") ||
                normalized.contains(" update ") || normalized.contains(" delete ") ||
                normalized.contains(" insert ") || normalized.contains(" drop ") ||
                normalized.contains(" alter ") || normalized.contains(" create ")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "only single SELECT statements are allowed"))
                    .build();
        }

        try (var conn = connectionSupplier.get();
             var stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            stmt.setFetchSize(1000);
            try (var rs = stmt.executeQuery(query)) {
                var rows = toList(rs);
                return Response.ok(rows).build();
            }
        } catch (Exception e) {
            monitor.severe("SQL query failed: " + e.getMessage(), e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    private List<Map<String, Object>> toList(ResultSet rs) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columns; ++i) {
                String colName = md.getColumnLabel(i);
                Object val = rs.getObject(i);
                row.put(colName, val);
            }
            list.add(row);
        }
        return list;
    }
}
