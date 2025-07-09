package dev.iiahmed.recraft.util

import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object JarMerger {

    fun merge(firstJar: File, secondJar: File, outputJar: File) {
        require(firstJar.exists()) { "First JAR does not exist: ${firstJar.absolutePath}" }
        require(secondJar.exists()) { "Second JAR does not exist: ${secondJar.absolutePath}" }
        outputJar.parentFile?.mkdirs()

        val addedEntries = mutableSetOf<String>()

        JarOutputStream(outputJar.outputStream()).use { jos ->

            fun copyJarEntries(jar: JarFile) {
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name in addedEntries) continue

                    addedEntries.add(entry.name)
                    jos.putNextEntry(ZipEntry(entry.name))
                    jar.getInputStream(entry).copyTo(jos)
                    jos.closeEntry()
                }
            }

            JarFile(firstJar).use { copyJarEntries(it) }
            JarFile(secondJar).use { copyJarEntries(it) }
        }
    }

}
