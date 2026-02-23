enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
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
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    include(":examples:android-wallet")
}
