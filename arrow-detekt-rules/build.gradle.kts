plugins {
    alias(libs.plugins.kotlin.jvm)
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "arrow-detekt-rules"
            pom {
                name.set("Arrow Detekt Rules")
                description.set("Detekt rules for enforcing Arrow typed error handling patterns")
            }
        }
    }
}
