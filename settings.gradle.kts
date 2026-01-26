rootProject.name = "edc-poc"

pluginManagement {
    repositories { mavenCentral(); gradlePluginPortal() }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories { mavenCentral(); mavenLocal() }
}

include("consumer")
