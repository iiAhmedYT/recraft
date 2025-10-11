package dev.iiahmed.recraft.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.util.jar.*
import java.util.zip.ZipFile
import javax.inject.Inject

@CacheableTask
abstract class MultiRelease @Inject constructor() : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val baselineMajor: Property<Int>

    @TaskAction
    fun run() {
        val file = jarFile.get().asFile
        if (!file.exists()) {
            logger.warn("Jar not found: ${file.absolutePath}")
            return
        }

        val baseline = baselineMajor.get()
        val javaMap = mapOf(
            52 to 8, 53 to 9, 54 to 10, 55 to 11, 56 to 12,
            57 to 13, 58 to 14, 59 to 15, 60 to 16, 61 to 17,
            62 to 18, 63 to 19, 64 to 20, 65 to 21
        )

        // Scan to find classes and their versions
        val classVersions = mutableMapOf<String, Int>()
        var maxMajor = baseline

        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .forEach { entry ->
                    zip.getInputStream(entry).use { stream ->
                        val header = ByteArray(8)
                        if (stream.read(header) == 8) {
                            val major = ((header[6].toInt() and 0xFF) shl 8) or (header[7].toInt() and 0xFF)
                            classVersions[entry.name] = major
                            if (major > maxMajor) maxMajor = major
                        }
                    }
                }
        }

        if (maxMajor <= baseline) {
            logger.lifecycle("No classes newer than baseline Java ${javaMap[baseline]} detected.")
            return
        }

        val targetJava = javaMap[maxMajor] ?: maxMajor
        logger.lifecycle("Detected Java $targetJava (major=$maxMajor). Converting to Multi-Release...")

        val tempJar = File(file.parentFile, "${file.nameWithoutExtension}-temp.jar")

        JarFile(file).use { jar ->
            val manifest = jar.manifest ?: Manifest()
            manifest.mainAttributes.putValue("Manifest-Version", "1.0")
            manifest.mainAttributes.putValue("Multi-Release", "true")

            JarOutputStream(tempJar.outputStream(), manifest).use { out ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    if (e.name.equals("META-INF/MANIFEST.MF", ignoreCase = true)) continue

                    val classMajor = classVersions[e.name]
                    val destName = when {
                        classMajor != null && classMajor > baseline ->
                            "META-INF/versions/${javaMap[classMajor] ?: classMajor}/${e.name}"
                        else -> e.name
                    }

                    out.putNextEntry(JarEntry(destName))
                    jar.getInputStream(e).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }

        // Replace original jar
        file.delete()
        tempJar.renameTo(file)
        logger.lifecycle("Reorganized higher-version classes under META-INF/versions/. Multi-Release manifest added.")
    }
}
