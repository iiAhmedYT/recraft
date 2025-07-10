package dev.iiahmed.recraft.tasks

import dev.iiahmed.recraft.util.ClassPrefixer
import dev.iiahmed.recraft.util.JarMerger
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class MergeJars @Inject constructor() : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val paperPrefix: Property<String>
    @get:Input
    abstract val spigotPrefix: Property<String>

    @get:InputFile
    abstract val inputSpigotJar: RegularFileProperty
    @get:InputFile
    abstract val inputPaperJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Input
    abstract val targetedPackages: ListProperty<String>

    init {
        group = "recraft"
        description = "Prefix and merge Spigot and Paper jars."
    }

    @TaskAction
    fun merge() {
        val spigotJar = inputSpigotJar.get().asFile
        val paperJar = inputPaperJar.get().asFile
        val output = outputJar.get().asFile

        val tempDir = project.layout.buildDirectory.dir("recraft/tmp/${minecraftVersion.get()}").get().asFile
        if (!tempDir.exists()) tempDir.mkdirs()

        val spigotPrefixedJar = File(tempDir, "spigot-prefixed.jar")
        val paperPrefixedJar = File(tempDir, "paper-prefixed.jar")

        logger.lifecycle("Prefixing Spigot JAR classes with '${spigotPrefix.get()}'...")
        ClassPrefixer.prefix(spigotJar, spigotPrefixedJar, spigotPrefix.get(), targetedPackages.get())

        logger.lifecycle("Prefixing Paper JAR classes with '${paperPrefix.get()}'...")
        ClassPrefixer.prefix(paperJar, paperPrefixedJar, paperPrefix.get(), targetedPackages.get())

        logger.lifecycle("Merging prefixed JARs into ${output.absolutePath}...")
        if (output.exists()) {
            output.delete()
        }
        JarMerger.merge(spigotPrefixedJar, paperPrefixedJar, output)

        logger.lifecycle("Merge completed!")
    }

}