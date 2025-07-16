package dev.iiahmed.recraft.tasks

import dev.iiahmed.recraft.util.MappingFetcher
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
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
        val (mojangJar, obfJar) = resolveInheritanceJars(project, minecraftVersion.get())
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

        try {
            this.remapJarWithInheritance(
                inputJar = input,
                outputJar = obfOutput,
                mappingFile = obfMappings,
                inheritanceJar = mojangJar,
                reversed = true
            )
            logger.lifecycle("Remapped JAR written to: ${output.absolutePath}")
        } catch (e: Exception) {
            throw RuntimeException("SpecialSource remapping failed", e)
        }

        logger.lifecycle("Remapping ${obfOutput.name} to Spigot mappings...")
        val spigotArgs = listOf(
            "--in-jar", obfOutput.absolutePath,
            "--out-jar", output.absolutePath,
            "--srg-in", spigotMappings.absolutePath,
            "--h", obfJar.absolutePath,
            "--quiet"
        )

        try {
            this.remapJarWithInheritance(
                inputJar = obfOutput,
                outputJar = output,
                mappingFile = spigotMappings,
                inheritanceJar = obfJar,
                reversed = false
            )
            logger.lifecycle("Remapped JAR written to: ${output.absolutePath}")
        } catch (e: Exception) {
            throw RuntimeException("SpecialSource remapping failed", e)
        }
    }

    private fun resolveInheritanceJars(project: Project, version: String): Pair<File, File> {
        val deps = project.dependencies

        val remappedMojangJar = project.configurations.detachedConfiguration(
            deps.create("org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-mojang")
        ).apply {
            isTransitive = false
        }.singleFile

        val obfuscatedJar = project.configurations.detachedConfiguration(
            deps.create("org.spigotmc:spigot:$version-R0.1-SNAPSHOT")
        ).apply {
            isTransitive = false
        }.singleFile

        return remappedMojangJar to obfuscatedJar
    }

    private fun remapJarWithInheritance(
        inputJar: File,
        outputJar: File,
        mappingFile: File,
        inheritanceJar: File,
        reversed: Boolean = true
    ) {
        Jar.init(inputJar).use { input ->
            Jar.init(inheritanceJar).use { inheritance ->
                val mapping = JarMapping().apply {
                    loadMappings(mappingFile.canonicalPath, reversed, false, null, null)

                    val provider = JointProvider().apply {
                        add(JarProvider(input))
                        add(JarProvider(inheritance))
                    }
                    setFallbackInheritanceProvider(provider)
                }

                val remapper = JarRemapper(mapping)
                remapper.remapJar(input, outputJar)
            }
        }
    }


}
