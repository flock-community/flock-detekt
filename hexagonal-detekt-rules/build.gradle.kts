plugins {
    alias(libs.plugins.kotlin.jvm)
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "hexagonal-detekt-rules"
            pom {
                name.set("Hexagonal Detekt Rules")
                description.set("Detekt rules for enforcing hexagonal architecture patterns")
            }
        }
    }
}
