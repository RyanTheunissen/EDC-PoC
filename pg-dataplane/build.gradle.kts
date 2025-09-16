plugins { `java-library` }


java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }


repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}


dependencies {
    val edc = "0.13.2"


    api("org.eclipse.edc:data-plane-spi:$edc")
    api("org.eclipse.edc:data-plane-selector-spi:$edc")
    api("org.eclipse.edc:data-plane-util:$edc")
    api("org.eclipse.edc:http-spi:$edc")


    implementation("org.postgresql:postgresql:42.7.4")
}