plugins { `java-library` }

dependencies {
    api("org.eclipse.edc:data-plane-spi:0.13.2")
    implementation("org.eclipse.edc:runtime-metamodel:0.13.2") // for @Extension, etc.
    implementation("org.postgresql:postgresql:42.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

tasks.test { useJUnitPlatform() }
