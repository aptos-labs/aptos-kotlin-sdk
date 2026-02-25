plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

allprojects {
    group = "com.aptos"
    version = "0.1.0"

    repositories {
        google()
        mavenCentral()
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "examples/**")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
            ),
        )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint()
    }
}

val koverModules = setOf("core", "client", "sdk", "indexer")

val androidModules = setOf("android-wallet")

val publishableModules = setOf("core", "client", "sdk", "indexer")

subprojects {
    if (name in androidModules) return@subprojects

    if (name in koverModules) {
        apply(plugin = "org.jetbrains.kotlinx.kover")
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    if (name in publishableModules) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        val artifactName =
            when (name) {
                "sdk" -> "aptos-kotlin-sdk"
                else -> "aptos-$name"
            }

        val artifactDescription =
            when (name) {
                "core" -> "Core types, crypto, BCS serialization, and transaction building for the Aptos blockchain"
                "client" -> "REST API and faucet client for the Aptos blockchain"
                "sdk" -> "High-level Kotlin SDK for interacting with the Aptos blockchain"
                "indexer" -> "GraphQL indexer client for the Aptos blockchain"
                else -> ""
            }

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "sonatype"
                    url =
                        if (version.toString().endsWith("SNAPSHOT")) {
                            uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                        } else {
                            uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        }
                    credentials {
                        username = findProperty("ossrhUsername")?.toString()
                            ?: System.getenv("OSSRH_USERNAME") ?: ""
                        password = findProperty("ossrhPassword")?.toString()
                            ?: System.getenv("OSSRH_PASSWORD") ?: ""
                    }
                }
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            the<JavaPluginExtension>().apply {
                withSourcesJar()
                withJavadocJar()
            }

            configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") {
                        from(components["java"])
                        groupId = project.group.toString()
                        artifactId = artifactName
                        version = project.version.toString()

                        pom {
                            name.set(artifactName)
                            description.set(artifactDescription)
                            url.set("https://github.com/aptos-labs/aptos-kotlin-sdk")

                            licenses {
                                license {
                                    name.set("Apache License 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                                }
                            }

                            developers {
                                developer {
                                    id.set("aptos-labs")
                                    name.set("Aptos Labs")
                                    url.set("https://github.com/aptos-labs")
                                }
                            }

                            scm {
                                url.set("https://github.com/aptos-labs/aptos-kotlin-sdk")
                                connection.set("scm:git:git://github.com/aptos-labs/aptos-kotlin-sdk.git")
                                developerConnection.set(
                                    "scm:git:ssh://git@github.com/aptos-labs/aptos-kotlin-sdk.git",
                                )
                            }
                        }
                    }
                }
            }

            configure<SigningExtension> {
                isRequired =
                    project.hasProperty("signingKey") ||
                    System.getenv("GPG_SIGNING_KEY") != null
                val signingKey =
                    findProperty("signingKey")?.toString()
                        ?: System.getenv("GPG_SIGNING_KEY")
                val signingPassword =
                    findProperty("signingPassword")?.toString()
                        ?: System.getenv("GPG_SIGNING_PASSWORD")
                if (signingKey != null) {
                    useInMemoryPgpKeys(signingKey, signingPassword ?: "")
                }
                sign(the<PublishingExtension>().publications)
            }
        }
    }
}

dependencies {
    subprojects.filter { it.name in koverModules }.forEach { kover(it) }
}
