// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Every plugin is declared here with `apply false` so its version resolves once on the build
// classpath; modules then apply via `alias(...)` without re-requesting a version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
// ── Infrastructure boundary ─────────────────────────────────────────────────────────────────────
// :core:voice and :core:device must stay reusable outside Discovery: no dependency on another
// project module, and no reference to Discovery code outside their own package. `check` runs it.
val infrastructureModules = mapOf(
    ":core:voice" to "com.qhathia.mobile.discovery.core.voice",
    ":core:device" to "com.qhathia.mobile.discovery.core.device",
)
infrastructureModules.keys.forEach { evaluationDependsOn(it) }

val verifyInfrastructureBoundaries by tasks.registering {
    group = "verification"
    description = "Fails if :core:voice or :core:device depend on, or reference, other Discovery code."

    // Collected at configuration time so the action holds only plain values (configuration cache).
    val projectDependencies = infrastructureModules.keys.flatMap { path ->
        project(path).configurations.flatMap { configuration ->
            configuration.dependencies.withType<ProjectDependency>()
                .filter { it.path != path }
                .map { "$path depends on project ${it.path} (configuration '${configuration.name}')" }
        }
    }.distinct()
    val sources = infrastructureModules.map { (path, ownPackage) ->
        Triple(path, ownPackage, project(path).fileTree("src/main") { include("**/*.kt", "**/*.java") })
    }
    sources.forEach { (_, _, tree) -> inputs.files(tree) }
    inputs.property("projectDependencies", projectDependencies)
    val root = rootDir

    doLast {
        val problems = projectDependencies.toMutableList()
        sources.forEach { (path, ownPackage, tree) ->
            val foreign = Regex("""com\.qhathia\.mobile\.discovery\.(?!${Regex.escape(ownPackage.removePrefix("com.qhathia.mobile.discovery."))}\b)[\w.]+""")
            tree.files.sorted().forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    foreign.find(line)?.let {
                        problems += "$path references ${it.value} in ${file.relativeTo(root)}:${index + 1}"
                    }
                }
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Infrastructure boundary broken:\n" + problems.joinToString("\n") { "  - $it" },
            )
        }
    }
}

infrastructureModules.keys.forEach { path ->
    project(path).tasks.named("check") { dependsOn(verifyInfrastructureBoundaries) }
}
