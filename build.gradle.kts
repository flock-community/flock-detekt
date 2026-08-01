plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group = "community.flock"
    version = "1.3.0-SNAPSHOT"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(providers.environmentVariable("SONATYPE_USERNAME"))
            password.set(providers.environmentVariable("SONATYPE_PASSWORD"))
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }

    dependencies {
        val detektVersion = rootProject.libs.versions.detekt.get()
        "compileOnly"("dev.detekt:detekt-api:$detektVersion")
        "testImplementation"("dev.detekt:detekt-api:$detektVersion")
        "testImplementation"("dev.detekt:detekt-test:$detektVersion")
        "testImplementation"(kotlin("test"))

        components {
            // detekt-test 2.0.0-alpha.4/5 declares a runtime dependency on the
            // dev.detekt:detekt-api-test-fixtures capability, but detekt-api's published
            // metadata contains no test-fixtures variant (the fixtures jar is not published
            // to Maven Central at all), which makes testRuntimeClasspath unresolvable.
            // detekt-api is declared explicitly above, so dropping the leaked dependency is safe.
            withModule("dev.detekt:detekt-test") {
                withVariant("runtimeElements") {
                    withDependencies {
                        removeAll { it.group == "dev.detekt" && it.name == "detekt-api" }
                    }
                }
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Custom Detekt rules for enforcing hexagonal architecture and typed error handling")
                    url.set("https://github.com/flock-community/flock-detekt")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("flock-community")
                            name.set("Flock Community")
                            email.set("info@flock.community")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/flock-community/flock-detekt.git")
                        developerConnection.set("scm:git:ssh://github.com/flock-community/flock-detekt.git")
                        url.set("https://github.com/flock-community/flock-detekt")
                    }
                }
            }
        }
    }

    val isSigningEnabled: Boolean = System.getenv("ENABLE_GRADLE_SIGNING")?.toBoolean() ?: true
    if (isSigningEnabled) {
        configure<SigningExtension> {
            useGpgCmd()
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    dependsOn(subprojects.map { it.tasks.named("publishToMavenLocal") })
    description = "Publishes all subproject artifacts to Maven Local"
    group = "publishing"
}
