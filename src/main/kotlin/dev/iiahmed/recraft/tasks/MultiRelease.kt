package dev.iiahmed.recraft.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.jar.*
import org.gradle.api.provider.Property
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class MultiRelease @Inject constructor() : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract var baselineMajor: Property<Int>

    @TaskAction
    fun run() {
        val file = jarFile.get().asFile
        if (!file.exists()) {
            logger.warn("Jar not found: ${file.absolutePath}")
            return
        }

        var maxMajor = baselineMajor.get()
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .forEach { entry ->
                    zip.getInputStream(entry).use { stream ->
                        val bytes = ByteArray(8)
                        stream.read(bytes)
                        val major = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
                        if (major > maxMajor) maxMajor = major
                    }
                }
        }

        val javaMap = mapOf(
            52 to "8", 53 to "9", 54 to "10", 55 to "11", 56 to "12",
            57 to "13", 58 to "14", 59 to "15", 60 to "16", 61 to "17",
            62 to "18", 63 to "19", 64 to "20", 65 to "21"
        )

        val targetJava = javaMap[maxMajor] ?: "?"

        if (maxMajor > baselineMajor.get()) {
            logger.lifecycle("Detected Java $targetJava (major=$maxMajor). Marking as Multi-Release.")
            val tempJar = file.resolveSibling("${file.nameWithoutExtension}-temp.jar")
            JarFile(file).use { jar ->
                val manifest = Manifest(jar.manifest)
                manifest.mainAttributes[Attributes.Name("Multi-Release")] = "true"
                JarOutputStream(tempJar.outputStream(), manifest).use { out ->
                    val entries = jar.entries()
                    while (entries.hasMoreElements()) {
                        val e = entries.nextElement()
                        out.putNextEntry(JarEntry(e.name))
                        jar.getInputStream(e).use { it.copyTo(out) }
                        out.closeEntry()
                    }
                }
            }
            file.delete()
            tempJar.renameTo(file)
        } else {
            logger.lifecycle("No higher-version classes found; skipping Multi-Release marking.")
        }
    }
}
