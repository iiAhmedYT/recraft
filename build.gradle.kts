plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.1.21"
}

group = "dev.iiahmed"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.md-5:SpecialSource:1.11.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    isAutomatedPublishing = false
    plugins {
        create("recraftPlugin") {
            id = "dev.iiahmed.recraft"
            displayName = "Recraft Plugin"
            implementationClass = "dev.iiahmed.recraft.RecraftPlugin"
            description = "A Gradle plugin for remapping Minecraft NMS plugins to Spigot and Paper and combining."
            tags.set(listOf("minecraft", "nms", "spigot", "paper", "remap", "combine"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("recraft") {
            from(components["java"])

            artifactId = "recraft"
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("Recraft Plugin")
                description.set("A Gradle plugin for remapping Minecraft NMS plugins to Spigot and Paper and combining.")
                url.set("https://github.com/iiAhmedYT/Recraft")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("iiAhmedYT")
                        name.set("Ahmed Waleed")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/iiAhmedYT/recraft.git")
                    developerConnection.set("scm:git:ssh://github.com:iiAhmedYT/recraft.git")
                    url.set("https://github.com/iiAhmedYT/Recraft")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GraveMC"
            url = uri("https://repo.gravemc.net/releases")
            credentials {
                username = findProperty("gravemc.repo.user") as String? ?: System.getenv("REPO_USER")
                password = findProperty("gravemc.repo.password") as String? ?: System.getenv("REPO_PASSWORD")
            }
        }
    }
}
