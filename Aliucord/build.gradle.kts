import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    alias(libs.plugins.aliucord.core)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka.html)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

group = "com.aliucord"
version = "2.5.0"

android {
    namespace = "com.aliucord"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    defaultConfig {
        buildConfigField("String", "VERSION", "\"$version\"")
        buildConfigField("boolean", "RELEASE", System.getenv("RELEASE") ?: "false")
        buildConfigField("int", "DISCORD_VERSION", libs.versions.discord.get())
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    publishing {
        singleVariant("debug") {}
    }

    lint {
        disable += "SetTextI18n"
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xno-call-assertions",
            "-Xno-param-assertions",
            "-Xno-receiver-assertions",
            "-Xallow-kotlin-package", // Workaround to adding kotlin.enums.EnumEntries polyfill
        )
    }
}
//
// configurations {
//     implementation {
//         exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
//     }
// }

dependencies {
    compileOnly(libs.aliuhook)
    compileOnly(libs.appcompat)
    compileOnly(libs.constraintlayout)
    compileOnly(libs.discord)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.material)
    compileOnly(project(":Injector")) // Needed to access certain stubs

    implementation(libs.credentials) {
        exclude(group = "androidx.appcompat", module = "appcompat")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "androidx.annotation", module = "annotation")
    }

    // implementation(libs.credentials.play)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf(
        "-Xlint:deprecation",
    ))
}
// val shadowDir = File(buildDir, "intermediates/shadowed")
//
// val out = project.layout.buildDirectory.dir("intermediates/copydep")
// tasks.register<ShadowJar>("relocateJar") {
//     val task1 = tasks.findByName("compileDebugKotlin")!!
//     val task2 = tasks.findByName("compileDebugJavaWithJavac")!!
//     from(task1.outputs, task2.outputs)
//     val arts = project.configurations.named("implementationArtifacts")
//     from(arts.map { configuration ->
//         configuration.incoming
//             .artifactView {
//                 attributes.attribute(
//                     ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
//                     ArtifactTypeDefinition.JAR_TYPE)
//             }
//             .files
//     })
//     duplicatesStrategy = DuplicatesStrategy.WARN
//     // from(*a)
//     into(out)
// }

afterEvaluate {
    // tasks.findByName("compileDebugKotlin")!!.dependsOn(tasks.getByName("rgen"))
    // tasks.findByName("compileDebugKotlin")!!.inputs.dir(rout)
    // tasks.findByName("compileDebugJavaWithJavac")!!.dependsOn(tasks.getByName("rgen"))
    // tasks.findByName("compileDebugJavaWithJavac")!!.inputs.dir(rout)
    tasks.findByName("compileDebugJavaWithJavac")!!.outputs.upToDateWhen { false }
    tasks.findByName("compileDex")!!.outputs.upToDateWhen { false }
    tasks.compileDex {
        val copyShadowed = tasks.findByName("copyShadowed")!! as Sync
        dependsOn(copyShadowed)
        input.setFrom(shadowDir)
    }
    publishing {
        publications {
            register<MavenPublication>(project.name) {
                from(components["debug"])
                artifact(tasks["debugSourcesJar"])
            }
        }

        repositories {
            val username = System.getenv("MAVEN_USERNAME")
            val password = System.getenv("MAVEN_PASSWORD")

            if (username != null && password != null) {
                maven {
                    credentials {
                        this.username = username
                        this.password = password
                    }
                    setUrl("https://maven.aliucord.com/snapshots")
                }
            } else {
                mavenLocal()
            }
        }
    }
}

apply {
    plugin(libs.plugins.shadow.get().pluginId)
}

val shadowDir = File(buildDir, "intermediates/shadowed")

tasks.register<ShadowJar>("relocateJar") {
    val task1 = tasks.findByName("compileDebugJavaWithJavac")!!
    val task = tasks.findByName("compileDebugKotlin")!!
    from(task1.outputs, task.outputs)
    val arts = project.configurations.named("implementationArtifacts")
    from(arts.map { configuration ->
        configuration.incoming
            .artifactView {
                attributes.attribute(
                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    ArtifactTypeDefinition.JAR_TYPE)
            }
            .files
    })
    relocate("kotlinx.coroutines", "com.aliucord.shadowed.kotlinx.coroutines")
    archiveClassifier.set("shadowed")
    destinationDirectory.set(File(buildDir, "intermediates"))
    outputs.upToDateWhen { false }
}

tasks.register<Sync>("copyShadowed") {
    val reloc = tasks.findByName("relocateJar")!! as ShadowJar
    dependsOn(reloc)
    from(zipTree(reloc.archiveFile))
    into(shadowDir)
}

project.afterEvaluate {
}
