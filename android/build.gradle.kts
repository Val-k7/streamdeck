import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false
    id("com.android.test") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        buildUponDefaultConfig = true
        config.setFrom(files("${rootProject.rootDir}/config/detekt/detekt.yml"))
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        filter { exclude("**/build/**") }
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }
}

tasks.register("clean", Delete::class) { delete(rootProject.layout.buildDirectory) }

// ============================================================
// Tâches pour builder l'UI React embarquée (web-ui/)
// ============================================================

val webUiDir = file("web-ui")
val webUiAssetsDir = file("app/src/main/assets/web")

// Vérifie si npm est disponible
fun isNpmAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("npm", "--version")
            .redirectErrorStream(true)
            .start()
        process.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

// Installe les dépendances npm si nécessaire
tasks.register<Exec>("installWebUiDependencies") {
    group = "web-ui"
    description = "Installe les dépendances npm pour l'UI React"
    workingDir = webUiDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("win")) {
        listOf("cmd", "/c", "npm", "install")
    } else {
        listOf("npm", "install")
    }
    // Ne pas échouer si node_modules existe déjà
    isIgnoreExitValue = true
    onlyIf { !file("$webUiDir/node_modules").exists() }
}

// Build l'UI React
tasks.register<Exec>("buildWebUi") {
    group = "web-ui"
    description = "Build l'UI React et la copie dans les assets Android"
    dependsOn("installWebUiDependencies")
    workingDir = webUiDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("win")) {
        listOf("cmd", "/c", "npm", "run", "build")
    } else {
        listOf("npm", "run", "build")
    }
    doFirst {
        println("Building React UI from web-ui/...")
    }
    doLast {
        println("React UI built successfully to app/src/main/assets/web/")
    }
}

// Nettoie les assets web
tasks.register<Delete>("cleanWebUi") {
    group = "web-ui"
    description = "Supprime les assets web buildés"
    delete(webUiAssetsDir)
}

// Hook: builder l'UI avant le build Android (optionnel, décommenter si souhaité)
// tasks.named("preBuild") {
//     dependsOn("buildWebUi")
// }

