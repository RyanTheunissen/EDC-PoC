rootProject.name = "edc-poc"

pluginManagement {
    repositories { mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { mavenCentral(); mavenLocal() }
}

include("provider")
