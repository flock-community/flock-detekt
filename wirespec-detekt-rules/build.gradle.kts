plugins {
    alias(libs.plugins.kotlin.jvm)
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "wirespec-detekt-rules"
            pom {
                name.set("Wirespec Detekt Rules")
                description.set("Detekt rules for enforcing contract-first design with Wirespec")
            }
        }
    }
}
