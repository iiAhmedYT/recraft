package dev.iiahmed.recraft.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object ClassPrefixer {

    fun prefix(inputJar: File, outputJar: File, prefix: String, targetedPackages: List<String>) {
        require(inputJar.exists()) { "Input JAR does not exist: ${inputJar.absolutePath}" }
        outputJar.parentFile?.mkdirs()

        JarFile(inputJar).use { jar ->
            JarOutputStream(outputJar.outputStream()).use { jos ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()

                    if (entry.isDirectory) {
                        jos.putNextEntry(ZipEntry(entry.name))
                        jos.closeEntry()
                        continue
                    }

                    if (entry.name.endsWith(".class")) {
                        val classBytes = jar.getInputStream(entry).readAllBytes()
                        val reader = ClassReader(classBytes)
                        val internalName = reader.className

                        val shouldPrefix = targetedPackages.any { internalName.startsWith(it) }

                        if (shouldPrefix) {
                            val (newName, newBytes) = renameClassFully(classBytes, prefix, targetedPackages)
                            jos.putNextEntry(ZipEntry("$newName.class"))
                            jos.write(newBytes)
                            jos.closeEntry()
                        } else {
                            jos.putNextEntry(ZipEntry(entry.name))
                            jos.write(classBytes)
                            jos.closeEntry()
                        }
                    } else {
                        jos.putNextEntry(ZipEntry(entry.name))
                        jar.getInputStream(entry).copyTo(jos)
                        jos.closeEntry()
                    }
                }
            }
        }
    }

    private fun renameClassFully(
        classBytes: ByteArray,
        prefix: String,
        targetedPackages: List<String>
    ): Pair<String, ByteArray> {
        val reader = ClassReader(classBytes)
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val remapper = PrefixingRemapper(prefix, targetedPackages)
        val visitor = ClassRemapper(writer, remapper)
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)

        val newName = remapper.map(reader.className)
        return newName to writer.toByteArray()
    }

    private class PrefixingRemapper(
        private val prefix: String,
        private val targetedPackages: List<String>
    ) : Remapper() {

        override fun map(internalName: String): String {
            return if (shouldPrefix(internalName)) {
                val simpleName = internalName.substringAfterLast('/')
                val packagePath = internalName.substringBeforeLast('/', missingDelimiterValue = "")
                if (packagePath.isNotEmpty()) "$packagePath/$prefix$simpleName" else "$prefix$simpleName"
            } else internalName
        }

        private fun shouldPrefix(name: String): Boolean {
            return targetedPackages.any { name.startsWith(it) }
        }
    }
}
