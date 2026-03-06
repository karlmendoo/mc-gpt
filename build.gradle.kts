plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.mcgpt"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("com.google.genai:google-genai:1.42.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.google.genai", "com.mcgpt.libs.genai")
        relocate("com.fasterxml.jackson", "com.mcgpt.libs.jackson")
        relocate("com.google.common", "com.mcgpt.libs.guava")
        relocate("com.google.auth", "com.mcgpt.libs.auth")
        relocate("com.google.api", "com.mcgpt.libs.api")
        relocate("okhttp3", "com.mcgpt.libs.okhttp3")
        relocate("okio", "com.mcgpt.libs.okio")
        relocate("com.google.protobuf", "com.mcgpt.libs.protobuf")
        relocate("org.java_websocket", "com.mcgpt.libs.websocket")
        relocate("kotlin", "com.mcgpt.libs.kotlin")
        relocate("org.jetbrains", "com.mcgpt.libs.jetbrains")
        relocate("org.slf4j", "com.mcgpt.libs.slf4j")
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
