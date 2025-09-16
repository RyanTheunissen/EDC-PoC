plugins {
    application
    id("com.gradleup.shadow") version "8.3.8"
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    val edc = "0.13.2"

    // Core runtime + connector
    implementation("org.eclipse.edc:runtime-core:$edc")
    implementation("org.eclipse.edc:connector-core:$edc")

    // Control plane & APIs
    implementation("org.eclipse.edc:control-plane-core:$edc")
    implementation("org.eclipse.edc:control-plane-api:$edc")
    implementation("org.eclipse.edc:management-api:$edc")
    implementation("org.eclipse.edc:dsp:$edc")
    implementation("org.eclipse.edc:http:$edc")
    implementation("org.eclipse.edc:configuration-filesystem:$edc")

    // Add these missing ones
    implementation("org.eclipse.edc:control-plane-api-client:$edc")     // fixes TransferProcessApiClient usage
    implementation("org.eclipse.edc:control-api-configuration:$edc")    // fixes ControlApiUrl
    implementation("org.eclipse.edc:iam-mock:$edc")                     // fixes IdentityService/AudienceResolver
    implementation("org.eclipse.edc:edr-store-core:$edc")               // fixes EDR store

    // Data plane + signaling + selector
    implementation("org.eclipse.edc:data-plane-core:$edc")
    implementation("org.eclipse.edc:data-plane-http:$edc")
    implementation("org.eclipse.edc:data-plane-self-registration:$edc")
    implementation("org.eclipse.edc:data-plane-selector-api:$edc")
    implementation("org.eclipse.edc:data-plane-selector-core:$edc")
    implementation("org.eclipse.edc:data-plane-signaling-api:$edc")
    implementation("org.eclipse.edc:transfer-data-plane-signaling:$edc")
    implementation("org.eclipse.edc:data-plane-public-api-v2:$edc")

    // Add client impl for transfer DP signaling
    implementation("org.eclipse.edc:data-plane-signaling-client:$edc")

    // Vault (ok to keep even if you use PUSH)

    implementation("org.eclipse.edc:vault-hashicorp:$edc")

    implementation("org.eclipse.edc:validator-data-address-http-data:$edc")

    // JDBC driver for PostgreSQL
    implementation(project(":pg-dataplane"))
    implementation("org.postgresql:postgresql:42.7.4")
}

tasks.shadowJar {
    archiveBaseName.set("consumer")
    mergeServiceFiles()
}
