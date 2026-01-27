package org.eclipse.edc.poc;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

// ✅ Try this import (your IDE will confirm the exact package)
// In many EDC setups it exists via the data-plane-http modules:
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;

import static org.eclipse.edc.spi.query.Criterion.criterion;

public class CloudBootstrapExtension implements ServiceExtension {

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private PolicyDefinitionStore policyDefinitionStore;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Cloud Bootstrap (HttpData Asset, Empty Policy)";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // 1) empty/unrestricted policy (fixed id "1")
        var policyDef = PolicyDefinition.Builder.newInstance()
                .id("1")
                .policy(Policy.Builder.newInstance().build())
                .build();
        try {
            policyDefinitionStore.create(policyDef);
        } catch (Exception ignored) {
            context.getMonitor().warning("PolicyDefinition id=1 already exists (ok)");
        }

        // 2) asset (fixed id "1") with HttpDataAddress (IMPORTANT)
        registerHttpAsset(context);

        // 3) contract definition (fixed id "1")
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("1")
                .contractPolicyId("1")
                .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", "1"))
                .build();
        try {
            contractDefinitionStore.save(contractDefinition);
        } catch (Exception ignored) {
            context.getMonitor().warning("ContractDefinition id=1 already exists (ok)");
        }
    }

    private void registerHttpAsset(ServiceExtensionContext context) {
        var httpDataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://file-server/test.txt")
                .build();

        var asset = Asset.Builder.newInstance()
                .id("1")
                .property("name", "Test txt")
                .property("description", "local test txt")
                .property("type", "txt")
                .dataAddress(httpDataAddress)
                .build();

        assetIndex.create(asset);

        context.getMonitor().info("Bootstrapped asset id=1 with HttpDataAddress baseUrl=http://file-server/test.txt");
    }
}
