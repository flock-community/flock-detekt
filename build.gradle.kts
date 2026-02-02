plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.nexus.publish)
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

    group = "community.flock"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        val detektVersion = rootProject.libs.versions.detekt.get()
        "compileOnly"("dev.detekt:detekt-api:$detektVersion")
        "testImplementation"("dev.detekt:detekt-api:$detektVersion")
        "testImplementation"("dev.detekt:detekt-test:$detektVersion")
        "testImplementation"(kotlin("test"))
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

    configure<SigningExtension> {
        val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY")
        val signingPassword = providers.environmentVariable("GPG_PASSPHRASE")
        if (signingKey.isPresent && signingPassword.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    dependsOn(subprojects.map { it.tasks.named("publishToMavenLocal") })
    description = "Publishes all subproject artifacts to Maven Local"
    group = "publishing"
}
