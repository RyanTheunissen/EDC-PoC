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

    // Control plane & management APIs
    implementation("org.eclipse.edc:control-plane-core:$edc")
    implementation("org.eclipse.edc:control-plane-api:$edc")
    implementation("org.eclipse.edc:control-plane-api-client:$edc")
    implementation("org.eclipse.edc:management-api:$edc")
    implementation("org.eclipse.edc:control-api-configuration:$edc")
    implementation("org.eclipse.edc:dsp:$edc")
    implementation("org.eclipse.edc:http:$edc")
    implementation("org.eclipse.edc:configuration-filesystem:$edc")
    implementation("org.eclipse.edc:iam-mock:$edc")
    implementation("org.eclipse.edc:edr-store-core:$edc")
    implementation("org.eclipse.edc:transfer-data-plane-signaling:$edc")
    implementation("org.eclipse.edc:validator-data-address-http-data:$edc")

    // Data plane + signaling + selector (provider needs DP to do PUSH)
    implementation("org.eclipse.edc:data-plane-core:$edc")
    implementation("org.eclipse.edc:data-plane-http:$edc")
    implementation("org.eclipse.edc:data-plane-public-api-v2:$edc")
    implementation("org.eclipse.edc:data-plane-signaling-api:$edc")
    implementation("org.eclipse.edc:data-plane-self-registration:$edc")
    implementation("org.eclipse.edc:data-plane-selector-api:$edc")
    implementation("org.eclipse.edc:data-plane-selector-core:$edc")

    //Vault (replace the old transfer-file-cloud module)
    implementation("org.eclipse.edc:vault-hashicorp:$edc")

    // JDBC driver for PostgreSQL
    implementation("org.postgresql:postgresql:42.7.4")

    // JDBC Data-Plane extension (choose one available in your distro)
    // NOTE: Uncomment ONE of the following once you add the JDBC DP module to your repository or Maven repo.
    implementation("org.eclipse.edc:data-plane-jdbc:$edc")
    // implementation("org.eclipse.edc:extensions:dataplane:data-plane-jdbc:$edc")
    // implementation("org.eclipse.edc:community:data-plane-sql:$edc")
    // Optional: validator for JdbcData addresses if provided by your module
    // implementation("org.eclipse.edc:validator-data-address-jdbc-data:$edc")
}

tasks.shadowJar {
    archiveBaseName.set("provider")
    mergeServiceFiles()
}
