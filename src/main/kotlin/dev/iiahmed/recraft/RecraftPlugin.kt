package dev.iiahmed.recraft

import dev.iiahmed.recraft.tasks.MergeJars
import dev.iiahmed.recraft.tasks.RemapToPaper
import dev.iiahmed.recraft.tasks.RemapToSpigot
import dev.iiahmed.recraft.util.VersionScheme
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

abstract class RecraftPlugin : Plugin<Project> {

    /**
     * Applies the RecraftBuild plugin to the given project.
     *
     * @param project The project to which the plugin is applied.
     */
    override fun apply(project: Project) {
        val extension = project.extensions.create("recraft", RecraftExtention::class.java)

        val remapToSpigot = project.tasks.register("remapToSpigot", RemapToSpigot::class.java) {
            dependsOn("jar")
            minecraftVersion.set(extension.minecraftVersion)

            val inputFileLocation = extension.jarFilePattern.getOrElse("libs/${project.name}.jar")
            val inputFile = project.layout.buildDirectory.file(inputFileLocation)
            inputJar.set(inputFile)
            outputJar.set(project.layout.buildDirectory.file("libs/${project.name}-spigot.jar"))

            val mappingFolder = project.layout.projectDirectory.dir("mappings/${extension.minecraftVersion.get()}/")
            // New scheme (26.1+) never runs this task, so don't create a mappings
            // folder for it (this block realizes on IDE sync / `gradle tasks` too).
            if (!VersionScheme.isMojangOnly(extension.minecraftVersion.get()) && !mappingFolder.asFile.exists()) {
                mappingFolder.asFile.mkdirs()
            }

            mappingsFolder.set(mappingFolder)
        }

        val remapToPaper = project.tasks.register("remapToPaper", RemapToPaper::class.java) {
            dependsOn("jar")
            minecraftVersion.set(extension.minecraftVersion)

            val inputFileLocation = extension.jarFilePattern.getOrElse("libs/${project.name}.jar")
            val inputFile = project.layout.buildDirectory.file(inputFileLocation)
            inputJar.set(inputFile)
            outputJar.set(project.layout.buildDirectory.file("libs/${project.name}-paper.jar"))
        }

        val mergeBothJars = project.tasks.register("mergeBothJars", MergeJars::class.java) {
            dependsOn(remapToSpigot)
            minecraftVersion.set(extension.minecraftVersion)
            shouldRemapToPaper.set(extension.remapToPaper)
            paperPrefix.set(extension.paperPrefix)
            spigotPrefix.set(extension.spigotPrefix)
            targetedPackages.set(extension.targetedPackages)

            inputSpigotJar.set(remapToSpigot.flatMap { it.outputJar })
            outputJar.set(project.layout.buildDirectory.file("libs/${project.name}-merged.jar"))

            // Only depend on remapToPaper if enabled
            if (extension.remapToPaper.get()) {
                dependsOn(remapToPaper)
                inputPaperJar.set(remapToPaper.flatMap { it.outputJar })
            }
        }

        // Passthrough used by the new (26.1+) scheme: the plain jar already works
        // everywhere, so just copy it to the merged output name for consistency.
        val recraftPassthrough = project.tasks.register("recraftPassthrough", Copy::class.java) {
            dependsOn("jar")

            val inputFileLocation = extension.jarFilePattern.getOrElse("libs/${project.name}.jar")
            from(project.layout.buildDirectory.file(inputFileLocation))
            into(project.layout.buildDirectory.dir("libs"))
            rename { "${project.name}-merged.jar" }
        }

        project.afterEvaluate {
            val version = extension.minecraftVersion

            if (version.get().isBlank()) {
                project.logger.warn("Recraft: 'minecraftVersion' is not set.")
                return@afterEvaluate
            }

            project.repositories.maven {
                name = "CodeMC"
                url = project.uri("https://repo.codemc.org/repository/nms/")
            }

            project.repositories.maven {
                name = "Minecraft Libraries"
                url = project.uri("https://libraries.minecraft.net/")
            }

            val newScheme = VersionScheme.isMojangOnly(version.get())

            // 26.1+ ships Mojang-mapped by default, so the `remapped-mojang`
            // classifier is obsolete — use the default artifact.
            project.dependencies.add(
                "compileOnly",
                if (newScheme) "org.spigotmc:spigot:${version.get()}-R0.1-SNAPSHOT"
                else "org.spigotmc:spigot:${version.get()}-R0.1-SNAPSHOT:remapped-mojang"
            )

            project.configurations.create("recraft") {
                isCanBeConsumed = true
                isCanBeResolved = false
            }

            if (newScheme) {
                project.logger.lifecycle(
                    "Recraft: MC ${version.get()} is Mojang-mapped by default; " +
                        "skipping Spigot remap and CraftBukkit unrelocation."
                )
                // A Copy task has no single-file output provider, so point the
                // artifact at the known output path, built by the copy task.
                project.artifacts.add(
                    "recraft",
                    project.layout.buildDirectory.file("libs/${project.name}-merged.jar")
                ) {
                    type = "jar"
                    builtBy(recraftPassthrough)
                }
                project.tasks.named("build") {
                    dependsOn(recraftPassthrough)
                }
            } else {
                project.artifacts.add("recraft", mergeBothJars.flatMap { it.outputJar }) {
                    type = "jar"
                    builtBy(mergeBothJars)
                }
                project.tasks.named("build") {
                    dependsOn(mergeBothJars)
                }
            }
        }
    }

}
