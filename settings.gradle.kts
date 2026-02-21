enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "aptos-kotlin-sdk"

include(":core")
include(":client")
include(":sdk")
include(":indexer")
include(":benchmarks")
