package org.example.edc.dataplane.postgres.pipeline;


import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;


import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;


public class PgClient {
    private final Monitor monitor;
    private final Vault vault;


    public PgClient(Monitor monitor, Vault vault) {
        this.monitor = monitor;
        this.vault = vault;
    }


    public Connection open(String jdbcUrl, String user, String password, String passwordKey) throws SQLException {
        var pw = password;
        if ((pw == null || pw.isBlank()) && passwordKey != null && !passwordKey.isBlank()) {
            var sec = vault.resolveSecret(passwordKey);
            if (sec == null) throw new SQLException("Vault secret not found: " + passwordKey);
            pw = sec;
        }
        return DriverManager.getConnection(Objects.requireNonNull(jdbcUrl, "jdbcUrl"), user, pw);
    }


    public void copyOutCsv(Connection cn, String copySql, OutputStream out) throws Exception {
        var pg = cn.unwrap(PGConnection.class);
        var mgr = new CopyManager(pg);
        mgr.copyOut(copySql, out);
    }


    public long copyInCsv(Connection cn, String copySql, InputStream in) throws Exception {
        var pg = cn.unwrap(PGConnection.class);
        var mgr = new CopyManager(pg);
        return mgr.copyIn(copySql, in);
    }
}