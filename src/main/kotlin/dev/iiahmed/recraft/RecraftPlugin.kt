package dev.iiahmed.recraft

import dev.iiahmed.recraft.tasks.MergeJars
import dev.iiahmed.recraft.tasks.RemapToPaper
import dev.iiahmed.recraft.tasks.RemapToSpigot
import org.gradle.api.Plugin
import org.gradle.api.Project

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
            if (!mappingFolder.asFile.exists()) {
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
            dependsOn(remapToSpigot, remapToPaper)
            minecraftVersion.set(extension.minecraftVersion)
            paperPrefix.set(extension.paperPrefix)
            spigotPrefix.set(extension.spigotPrefix)
            targetedPackages.set(extension.targetedPackages)

            inputSpigotJar.set(remapToSpigot.flatMap { it.outputJar })
            inputPaperJar.set(remapToPaper.flatMap { it.outputJar })
            outputJar.set(project.layout.buildDirectory.file("libs/${project.name}-merged.jar"))
        }

        /* Disable for now, as multi-release jars are not ideal.
        val markMultiRelease = project.tasks.register("markMultiRelease", MultiRelease::class.java) {
            dependsOn(mergeBothJars)
            baselineMajor.set(extension.baselineMajor)
            jarFile.set(mergeBothJars.flatMap { it.outputJar })
        } */

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

            project.dependencies.add(
                "compileOnly",
                "org.spigotmc:spigot:${version.get()}-R0.1-SNAPSHOT:remapped-mojang"
            )

            project.configurations.create("recraft") {
                isCanBeConsumed = true
                isCanBeResolved = false
            }
            project.artifacts.add("recraft", mergeBothJars.flatMap { it.outputJar }) {
                type = "jar"
                builtBy(mergeBothJars)
            }
        }

        project.tasks.named("build") {
            dependsOn(mergeBothJars)
        }
    }

}
