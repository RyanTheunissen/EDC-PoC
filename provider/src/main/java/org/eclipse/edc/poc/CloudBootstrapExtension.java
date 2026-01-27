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
import org.eclipse.edc.spi.types.domain.DataAddress;

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
        return "Cloud Bootstrap (Http Asset, Empty Policy)";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var policy = createEmptyPolicy();
        policyDefinitionStore.create(policy);

        registerAsset();

        registerContractDefinition(policy.getId());
    }

    private void registerAsset() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property("baseUrl", "http://file-server/test.txt")
                .property("proxyPath", "false")
                .build();

        var asset = Asset.Builder.newInstance()
                .id("1")
                .property("name", "Test txt")
                .property("description", "local test txt")
                .property("type", "txt")
                .dataAddress(dataAddress)
                .build();

        assetIndex.create(asset);
    }


    private void registerContractDefinition(String policyId) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .assetsSelectorCriterion(
                        criterion(Asset.PROPERTY_ID, "=", "1")
                )
                .build();

        contractDefinitionStore.save(contractDefinition);
    }

    private PolicyDefinition createEmptyPolicy() {

        var policy = Policy.Builder.newInstance().build();

        return PolicyDefinition.Builder.newInstance()
                .id("1")
                .policy(policy)
                .build();
    }
}
