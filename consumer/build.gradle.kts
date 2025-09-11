plugins {
    application
    id("com.gradleup.shadow") version "8.3.8"
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

java {
    toolchain { languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17)) }
}

dependencies {
    val edc = "0.13.2"

    // Core runtime + connector
    implementation("org.eclipse.edc:runtime-core:$edc")
    implementation("org.eclipse.edc:connector-core:$edc")

    // Control plane & APIs (consumer initiates catalog/negotiation/transfer)
    implementation("org.eclipse.edc:control-plane-core:$edc")
    implementation("org.eclipse.edc:control-plane-api:$edc")
    implementation("org.eclipse.edc:management-api:$edc")
    implementation("org.eclipse.edc:dsp:$edc")
    implementation("org.eclipse.edc:http:$edc")
    implementation("org.eclipse.edc:configuration-filesystem:$edc")

    // Data plane (consumer DP is useful for PULL or EDR flows; harmless to include)
    implementation("org.eclipse.edc:data-plane-core:$edc")
    implementation("org.eclipse.edc:data-plane-http:$edc")
    implementation("org.eclipse.edc:data-plane-self-registration:$edc")
    implementation("org.eclipse.edc:data-plane-selector-api:$edc")
    implementation("org.eclipse.edc:data-plane-selector-core:$edc")
    implementation("org.eclipse.edc:data-plane-signaling-api:$edc")
    implementation("org.eclipse.edc:transfer-data-plane-signaling:$edc")
    implementation("org.eclipse.edc:data-plane-public-api-v2:$edc")

    // Storage + Vault (needed if you test PULL; otherwise harmless)
    implementation("org.eclipse.edc.aws:data-plane-aws-s3:$edc")
    implementation("org.eclipse.edc.azure:data-plane-azure-storage:$edc")
    implementation("org.eclipse.edc:vault-hashicorp:$edc")

    implementation("org.eclipse.edc:validator-data-address-http-data:$edc")
}

tasks.shadowJar {
    archiveBaseName.set("consumer")
    mergeServiceFiles()
}
