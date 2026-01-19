plugins {
    id("java-library")
    id("maven-publish")
}

val hytaleServerJar = rootProject.file("../HytaleServer.jar").takeIf { it.exists() }
    ?: file("C:/Users/fores/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar")

dependencies {
    compileOnly(files(hytaleServerJar))

    compileOnly("de.oliver.FancyAnalytics:logger:0.0.9")
    implementation("de.oliver.FancyAnalytics:java-sdk:0.0.5")
    compileOnly("com.fancyinnovations.fancyspaces:java-sdk:0.0.3")

    compileOnly("com.google.code.gson:gson:2.13.2")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks {
    publishing {
        repositories {
            maven {
                name = "fancyinnovationsReleases"
                url = uri("https://repo.fancyinnovations.com/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    isAllowInsecureProtocol = true
                    create<BasicAuthentication>("basic")
                }
            }

            maven {
                name = "fancyinnovationsSnapshots"
                url = uri("https://repo.fancyinnovations.com/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    isAllowInsecureProtocol = true
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.fancyinnovations.hytale"
                artifactId = "ui-helper"
                version = getUIHelperVersion()
                from(project.components["java"])
            }
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()

        options.release.set(25)
    }
}

fun getUIHelperVersion(): String {
    return file("VERSION").readText()
}
