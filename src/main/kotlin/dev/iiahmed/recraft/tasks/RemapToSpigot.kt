package dev.iiahmed.recraft.tasks

import dev.iiahmed.recraft.util.MappingFetcher
import net.md_5.specialsource.SpecialSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class RemapToSpigot @Inject constructor() : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:InputDirectory
    abstract val mappingsFolder: DirectoryProperty

    init {
        group = "recraft"
        description = "Remaps compiled plugin from Mojang names to Spigot obfuscated names."
    }

    @TaskAction
    fun remap() {
        val input = inputJar.get().asFile
        val output = outputJar.get().asFile
        val obfOutput = output.resolveSibling("${input.nameWithoutExtension}-obf.jar")

        if (!input.exists()) throw IllegalStateException("Input JAR not found: ${input.absolutePath}")
        output.parentFile?.mkdirs()

        val mappingsDir = mappingsFolder.get().asFile
        if (!mappingsDir.exists()) {
            mappingsDir.mkdirs()
        }

        MappingFetcher.downloadMappings(
            minecraftVersion.get(),
            mappingsDir
        )

        logger.lifecycle("Remapping ${input.name} to Obf mappings...")
        val obfMappings = mappingsDir.resolve("mojang-mappings.txt")
        val spigotMappings = mappingsDir.resolve("spigot-mappings.csrg")
        if (!obfMappings.exists()) {
            throw IllegalStateException("Obfuscated mappings not found: ${obfMappings.absolutePath}")
        }

        if (!spigotMappings.exists()) {
            throw IllegalStateException("Spigot mappings not found: ${spigotMappings.absolutePath}")
        }

        val obfArgs = listOf(
            "--in-jar", input.absolutePath,
            "--out-jar", obfOutput.absolutePath,
            "--srg-in", obfMappings.absolutePath,
            "--reverse",
            "--quiet"
        )

        try {
            SpecialSource.main(obfArgs.toTypedArray())
            logger.lifecycle("Remapped JAR written to: ${output.absolutePath}")
        } catch (e: Exception) {
            throw RuntimeException("SpecialSource remapping failed", e)
        }

        logger.lifecycle("Remapping ${input.name} to Spigot mappings...")
        val spigotArgs = listOf(
            "--in-jar", obfOutput.absolutePath,
            "--out-jar", output.absolutePath,
            "--srg-in", spigotMappings.absolutePath,
            "--quiet"
        )

        try {
            SpecialSource.main(spigotArgs.toTypedArray())
            logger.lifecycle("Remapped JAR written to: ${output.absolutePath}")
        } catch (e: Exception) {
            throw RuntimeException("SpecialSource remapping failed", e)
        }
    }

}
