package org.eclipse.edc.poc;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.spi.query.Criterion.criterion;

@Path("/sql/asset")
public class SqlAssetResource {
    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore contractStore;
    private final ServiceExtensionContext ctx;
    private final Monitor monitor;

    public SqlAssetResource(AssetIndex assetIndex,
                            PolicyDefinitionStore policyStore,
                            ContractDefinitionStore contractStore,
                            ServiceExtensionContext ctx,
                            Monitor monitor) {
        this.assetIndex = assetIndex;
        this.policyStore = policyStore;
        this.contractStore = contractStore;
        this.ctx = ctx;
        this.monitor = monitor;
    }

    public record CreateRequest(String assetId, String query) { }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(CreateRequest req) {
        if (req == null || req.query() == null || req.query().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "missing 'query' in body"))
                    .build();
        }
        var normalized = req.query().trim().toLowerCase();
        if (!normalized.startsWith("select ") || normalized.contains(";") ||
                normalized.contains(" update ") || normalized.contains(" delete ") ||
                normalized.contains(" insert ") || normalized.contains(" drop ") ||
                normalized.contains(" alter ") || normalized.contains(" create ")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "only single SELECT statements are allowed"))
                    .build();
        }

        var assetId = req.assetId() != null && !req.assetId().isBlank() ? req.assetId().trim() : ("sql-" + UUID.randomUUID());

        ensureDefaultPolicy();

        var host = ctx.getConfig().getString("edc.hostname", "localhost");
        var port = ctx.getConfig().getInteger("web.http.port", 19191);
        var encoded = URLEncoder.encode(req.query(), StandardCharsets.UTF_8);
        var url = String.format("http://%s:%d/api/sql?q=%s", host, port, encoded);

        var dataAddress = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property("baseUrl", url)
                .property("name", "SQL Endpoint for: " + abbreviate(req.query(), 40))
                .build();

        var asset = Asset.Builder.newInstance()
                .id(assetId)
                .dataAddress(dataAddress)
                .build();
        assetIndex.create(asset);

        var contractId = "cd-" + assetId;
        var cd = ContractDefinition.Builder.newInstance()
                .id(contractId)
                .accessPolicyId("1")
                .contractPolicyId("1")
                .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", assetId))
                .build();
        contractStore.save(cd);

        monitor.info("Created SQL asset " + assetId + " and contract " + contractId);
        return Response.ok(Map.of(
                "assetId", assetId,
                "contractDefinitionId", contractId,
                "baseUrl", url
        )).build();
    }

    private void ensureDefaultPolicy() {
        var existing = policyStore.findById("1");
        if (existing == null) {
            var usePermission = Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .build();

            PolicyDefinition pd = PolicyDefinition.Builder.newInstance()
                    .policy(Policy.Builder.newInstance().permission(usePermission).build())
                    .id("1")
                    .build();
            policyStore.create(pd);
        }
    }

    private static String abbreviate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }
}