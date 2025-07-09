package dev.iiahmed.recraft.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.objectweb.asm.*
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject

abstract class RemapToPaper @Inject constructor() : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    init {
        group = "recraft"
        description = "Relocates CraftBukkit classes and imports back to unrelocated names."
    }

    @TaskAction
    fun remap() {
        val input = inputJar.get().asFile
        val output = outputJar.get().asFile

        if (!input.exists()) {
            throw IllegalStateException("Input JAR not found: ${input.absolutePath}")
        }

        output.parentFile?.mkdirs()

        logger.lifecycle("Relocating ${input.name} for Minecraft version ${minecraftVersion.get()}...")

        JarFile(input).use { jar ->
            JarOutputStream(output.outputStream()).use { jos ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val newEntry = ZipEntry(entry.name)
                    jos.putNextEntry(newEntry)

                    if (entry.name.endsWith(".class")) {
                        val classBytes = jar.getInputStream(entry).readAllBytes()
                        val remappedBytes = relocateClass(classBytes)
                        jos.write(remappedBytes)
                    } else {
                        // Copy resource files unchanged
                        jar.getInputStream(entry).copyTo(jos)
                    }

                    jos.closeEntry()
                }
            }
        }

        logger.lifecycle("Relocated jar written to: ${output.absolutePath}")
    }

    private fun relocateClass(classBytes: ByteArray): ByteArray {
        val reader = ClassReader(classBytes)
        val writer = ClassWriter(reader, 0)

        val visitor = RelocationClassVisitor(writer)

        reader.accept(visitor, 0)

        return writer.toByteArray()
    }

    inner class RelocationClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            val remappedName = remapName(name)
            val remappedSuper = remapName(superName)
            val remappedInterfaces = interfaces?.map { remapName(it) }?.toTypedArray()

            super.visit(version, access, remappedName, signature, remappedSuper, remappedInterfaces)
        }

        override fun visitField(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            value: Any?
        ): FieldVisitor? {
            val remappedDesc = remapDesc(descriptor)
            val remappedSignature = signature?.let { remapSignature(it) }
            return super.visitField(access, name, remappedDesc, remappedSignature, value)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            val remappedDesc = remapMethodDesc(descriptor)
            val remappedSignature = signature?.let { remapSignature(it) }
            val remappedExceptions = exceptions?.map { remapName(it) }?.toTypedArray()

            val mv = super.visitMethod(access, name, remappedDesc, remappedSignature, remappedExceptions)
            return RemappingMethodVisitor(mv)
        }

        private val versionedPrefixRegex = Regex("""org/bukkit/craftbukkit/v\d+_\d+(_R\d+)?/""")
        private val cleanPrefix = "org/bukkit/craftbukkit/"

        private fun remapName(name: String?): String? {
            if (name == null) return null
            return versionedPrefixRegex.replace(name, cleanPrefix)
        }

        private fun remapDesc(desc: String?): String? {
            if (desc == null) return null
            val type = Type.getType(desc)
            return remapType(type).descriptor
        }

        private fun remapMethodDesc(desc: String?): String? {
            if (desc == null) return null
            val methodType = Type.getMethodType(desc)
            val remappedArgs = methodType.argumentTypes.map { remapType(it) }.toTypedArray()
            val remappedReturn = remapType(methodType.returnType)
            return Type.getMethodDescriptor(remappedReturn, *remappedArgs)
        }

        private fun remapSignature(signature: String): String {
            // Signature remapping can be complicated, here we just return as-is or add proper parsing if needed
            return signature
        }

        private fun remapType(type: Type): Type {
            return when (type.sort) {
                Type.ARRAY -> Type.getType("[" + remapType(type.elementType).descriptor)
                Type.OBJECT -> {
                    val remapped = remapName(type.internalName)
                    if (remapped != null) Type.getObjectType(remapped) else type
                }
                else -> type
            }
        }

        inner class RemappingMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {
            override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
                val remappedOwner = remapName(owner)
                val remappedDesc = remapDesc(descriptor)
                super.visitFieldInsn(opcode, remappedOwner, name, remappedDesc)
            }

            override fun visitMethodInsn(
                opcode: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                val remappedOwner = remapName(owner)
                val remappedDesc = remapMethodDesc(descriptor)
                super.visitMethodInsn(opcode, remappedOwner, name, remappedDesc, isInterface)
            }

            override fun visitTypeInsn(opcode: Int, type: String?) {
                val remappedType = remapName(type)
                super.visitTypeInsn(opcode, remappedType)
            }

            override fun visitLdcInsn(value: Any?) {
                if (value is Type) {
                    val remappedType = remapType(value)
                    super.visitLdcInsn(remappedType)
                } else {
                    super.visitLdcInsn(value)
                }
            }

            override fun visitMultiANewArrayInsn(descriptor: String?, dims: Int) {
                val remappedDesc = remapDesc(descriptor)
                super.visitMultiANewArrayInsn(remappedDesc, dims)
            }
        }
    }
}
