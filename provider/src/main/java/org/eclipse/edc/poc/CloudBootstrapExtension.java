package org.eclipse.edc.poc;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Bootstraps provider-side catalog resources (asset, policy, and contract definition) at startup,
 * replacing the need to POST JSON files manually.
 *
 * IDs are fixed to keep offer IDs stable and compatible with existing consumer requests:
 * - Asset id = "1"
 * - PolicyDefinition id = "1" (simple USE policy)
 * - ContractDefinition id = "1" (selects asset id "1" and references policy "1")
 */
public class CloudBootstrapExtension implements ServiceExtension {

    @Inject
    private AssetIndex assetIndex;
    @Inject
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Cloud Bootstrap (Assets/Policies/Contracts)";
        
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // 1) Create or upsert the policy with a fixed id "1"
        var policy = createPolicy();
        policyDefinitionStore.create(policy);

        // 2) Register the Azure-backed asset with id "1"
        registerAsset();

        // 3) Register the contract definition with id "1" selecting asset "1" and using policy "1"
        registerContractDefinition(policy.getId());
    }

    private void registerAsset() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("@type", "DataAddress")
                .property("account", "provider")
                .property("container", "src-container")
                .property("blobName", "test-document.txt")
                .keyName("provider-key")
                .build();

        var asset = Asset.Builder.newInstance()
                .id("Bodegraven")
                .dataAddress(dataAddress)
                .build();

        assetIndex.create(asset);
    }

    private void registerContractDefinition(String policyId) {
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", "1"))
                .build();

        contractDefinitionStore.save(contractDefinition);
    }

    private PolicyDefinition createPolicy() {
        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(usePermission)
                        .build())
                .build();
    }
}
