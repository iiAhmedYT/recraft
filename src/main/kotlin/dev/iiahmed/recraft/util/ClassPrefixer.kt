package dev.iiahmed.recraft.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

object ClassPrefixer {

    fun prefix(inputJar: File, outputJar: File, prefix: String) {
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
                        val (newName, newBytes) = renameClassOnly(classBytes, prefix)
                        jos.putNextEntry(ZipEntry("$newName.class"))
                        jos.write(newBytes)
                        jos.closeEntry()
                    } else {
                        jos.putNextEntry(ZipEntry(entry.name))
                        jar.getInputStream(entry).copyTo(jos)
                        jos.closeEntry()
                    }
                }
            }
        }
    }

    private fun renameClassOnly(classBytes: ByteArray, prefix: String): Pair<String, ByteArray> {
        val reader = ClassReader(classBytes)
        val writer = ClassWriter(0)

        var newName: String? = null

        val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                val simpleName = name.substringAfterLast('/')
                val packagePath = name.substringBeforeLast('/', missingDelimiterValue = "")
                newName = if (packagePath.isNotEmpty()) "$packagePath/$prefix$simpleName" else "$prefix$simpleName"
                super.visit(version, access, newName, signature, superName, interfaces)
            }
        }

        reader.accept(visitor, 0)
        return newName!! to writer.toByteArray()
    }

}
