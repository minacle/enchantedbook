plugins {
    java
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

group = "moe.minacle.minecraft"
version = "0.6.2"

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "Sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "Sonatype"
        url = uri("https://s01.oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${libs.versions.minecraft.get()}-R0.1-SNAPSHOT")
    implementation(libs.bstats.bukkit)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }

    shadowJar {
        archiveClassifier.set("")
        enableAutoRelocation.set(true)
        relocationPrefix.set("moe.minacle.minecraft.plugins.enchantedbook.shadowjar")
        minimize()
    }
}

runPaper.folia.registerTask()
