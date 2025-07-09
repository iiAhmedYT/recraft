package dev.iiahmed.recraft.util

import java.io.File

object MojangToSrgConverter {

    private val primitives = mapOf(
        "void" to "V",
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "short" to "S",
        "int" to "I",
        "long" to "J",
        "float" to "F",
        "double" to "D"
    )

    private fun toDescriptor(typeStr: String): String {
        val t = typeStr.trim()
        return when {
            primitives.containsKey(t) -> primitives[t]!!
            t.endsWith("[]") -> "[" + toDescriptor(t.removeSuffix("[]"))
            else -> "L" + t.replace('.', '/') + ";"
        }
    }

    /**
     * Converts the input Mojang mapping file to SRG format, writing the output to the output file.
     */
    fun convert(inputPath: String, outputPath: String) {
        var currentClass: String? = null

        File(outputPath).printWriter().use { writer ->
            File(inputPath).forEachLine { lineRaw ->
                val line = lineRaw.trimEnd()
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine

                // Class line: com.mojang.math.Constants -> a:
                val classMatch = Regex("""^([\w.]+) -> (\w+):$""").matchEntire(line)
                if (classMatch != null) {
                    val (className, mappedName) = classMatch.destructured
                    currentClass = className.replace('.', '/')
                    writer.println("CL: $currentClass $mappedName")
                    return@forEachLine
                }

                if (currentClass == null) return@forEachLine

                // Field line: float PI -> a
                val fieldMatch = Regex("""^\s*[\w.$]+ (\w+) -> (\w+)$""").matchEntire(line)
                if (fieldMatch != null) {
                    val (fieldName, mappedName) = fieldMatch.destructured
                    writer.println("FD: $currentClass/$fieldName $mappedName")
                    return@forEachLine
                }

                // Method line: 3:3:void <init>() -> <init>
                val methodMatch = Regex("""^\s*\d+:\d+:(.+) (\S+)\((.*)\) -> (\S+)$""").matchEntire(line)
                if (methodMatch != null) {
                    val (returnType, methodName, params, mappedName) = methodMatch.destructured

                    val paramList = if (params.isBlank()) emptyList() else params.split(",")
                    val paramDesc = paramList.joinToString("") { toDescriptor(it) }
                    val retDesc = toDescriptor(returnType)

                    val methodDesc = "($paramDesc)$retDesc"
                    writer.println("MD: $currentClass/$methodName $methodDesc $mappedName")
                    return@forEachLine
                }
            }
        }
    }

}
