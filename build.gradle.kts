plugins {
    java
}

group = "mod.wurmunlimited.npcs.beastsummoner"
version = "0.1.3-testing"
val shortName = "beastsummoner"
val wurmServerFolder = "E:/Steam/steamapps/common/Wurm Unlimited/WurmServerLauncher/"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(project(":WurmTestingHelper"))
    implementation(project(":BMLBuilder"))
    implementation(project(":CreatureCustomiser"))
    implementation(project(":PlaceNpc"))
    implementation(project(":QuestionLibrary"))
    implementation(project(":TradeLibrary"))
    implementation("com.wurmonline:server:1.9")
    implementation("org.gotti.wurmunlimited:server-modlauncher:0.45")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    jar {
        doLast {
            copy {
                from(jar)
                into(wurmServerFolder + "mods/" + shortName)
            }

            copy {
                from("src/main/resources/$shortName.properties")
                into(wurmServerFolder + "mods/")
            }
        }

        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("BMLBuilder") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("CreatureCustomiser") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("PlaceNpc") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("QuestionLibrary") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("TradeLibrary") && it.name.endsWith("jar") }.map { zipTree(it) })

        includeEmptyDirs = false
        archiveFileName.set("$shortName.jar")
        exclude("**/TradeHandler.class", "**/Trade.class", "**/TradingWindow.class")

        manifest {
            attributes["Implementation-Version"] = archiveVersion
        }
    }

    register<Zip>("zip") {
        into(shortName) {
            from(jar)
        }

        from("src/main/resources/$shortName.properties")
        archiveFileName.set("$shortName.zip")
    }
}