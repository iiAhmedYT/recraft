package dev.iiahmed.recraft

import dev.iiahmed.recraft.util.MappingFetcher
import org.junit.jupiter.api.Test

class TestClass {

    @Test
    fun test() {
        MappingFetcher.downloadMappings("1.21.7", outputDir = java.io.File("build/mappings/1.21.7/"))
    }

}