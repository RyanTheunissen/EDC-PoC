rootProject.name = "edc-poc"

pluginManagement {
    repositories { mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { mavenCentral(); mavenLocal() }
}

include("provider", "consumer")
include(":extensions:data-plane:data-plane-postgres")
