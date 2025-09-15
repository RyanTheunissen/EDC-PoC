package org.eclipse.edc.poc;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Simple extension that exposes an HTTP endpoint on the default API context to run read-only SQL queries
 * against a Postgres database running next to the provider. This is a PoC helper to demonstrate
 * sending a SQL query from the consumer side via an HttpData asset.
 */
public class SqlQueryExtension implements ServiceExtension {

    public static final String CFG_JDBC_URL = "poc.sql.jdbc.url";
    public static final String CFG_JDBC_USER = "poc.sql.jdbc.user";
    public static final String CFG_JDBC_PASSWORD = "poc.sql.jdbc.password";

    @Inject
    private WebService webService;

    @Inject
    private Monitor monitor;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionStore policyStore;

    @Inject
    private ContractDefinitionStore contractStore;

    private ConnectionSupplier connectionSupplier;

    @Override
    public String name() {
        return "PoC SQL Query API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var cfg = context.getConfig();
        var url = cfg.getString(CFG_JDBC_URL, null);
        var user = cfg.getString(CFG_JDBC_USER, null);
        var pass = cfg.getString(CFG_JDBC_PASSWORD, null);

        if (url == null) {
            monitor.warning("No JDBC URL configured (" + CFG_JDBC_URL + "), SQL endpoint will be disabled.");
            return;
        }
        connectionSupplier = () -> DriverManager.getConnection(url, user, pass);

        var queryController = new SqlQueryResource(connectionSupplier, monitor);
        webService.registerResource(queryController);
        monitor.info("Registered SQL query endpoint at /api/sql");

        // Dynamic asset creation for arbitrary SELECT queries
        var assetController = new SqlAssetResource(assetIndex, policyStore, contractStore, context, monitor);
        webService.registerResource(assetController);
        monitor.info("Registered SQL asset creation endpoint at /api/sql/asset");
    }

    @FunctionalInterface
    interface ConnectionSupplier {
        Connection get() throws Exception;
    }
}
