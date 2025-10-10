package dev.iiahmed.recraft.util

import java.io.File
import java.net.URI

object MappingFetcher {

    private const val MOJANG_BASE_URL = "https://piston-data.mojang.com/v1/objects/%s/server.txt"
    private val versionToHash = mapOf(
        "1.21.10" to "c5440743411a6fd7490fa18a4b6c5d8edf36d88b",
        "1.21.9" to "587e016fe8a876cbc1cc98d73f9d0d79bfb32b2c",
        "1.21.8" to "eb1e1eb47cb740012fc82eacc394859463684132",
        "1.21.7" to "eb1e1eb47cb740012fc82eacc394859463684132",
        "1.21.6" to "94d453080a58875d3acc1a9a249809767c91ed40",
        "1.21.5" to "f4812c1d66d0098a94616b19c21829e591d0af3a",
        "1.21.4" to "0b1e60cc509cfb0172573ae56b436c29febbc187",
        "1.21.3" to "c70e10f72ea65bb97e156143fd97c852dc958325",
        "1.21.2" to "ce1d4ab050af87c41fc3d51050ef6862385da784",
        "1.21.1" to "03f8985492bda0afc0898465341eb0acef35f570",
        "1.21" to "31c77994d96f05ba25a870ada70f47f315330437",
        "1.20.6" to "9e96100f573a46ef44caab3e716d5eb974594bb7",
        "1.20.5" to "14be97974a77b09eb8bca88ed9268445f0fde3e7",
        "1.20.4" to "c1cafe916dd8b58ed1fe0564fc8f786885224e62",
        "1.20.3" to "c1cafe916dd8b58ed1fe0564fc8f786885224e62",
        "1.20.2" to "dced504a9b5df367ddd3477adac084cea8024ba4",
        "1.20.1" to "0b4dba049482496c507b2387a73a913230ebbd76",
        "1.20" to "15e61168fd24c7950b22cd3b9e771a7ce4035b41",
        "1.19.4" to "73c8bb982e420b33aad9632b482608c5c33e2d13",
        "1.19.3" to "bc44f6dd84cd2f3ad8c0caad850eaca9e82067e3",
        "1.19.2" to "ed5e6e8334ad67f5af0150beed0f3d156d74bd57",
        "1.19.1" to "3565648cdd47ae15738fb804a95a659137d7cfd3",
        "1.19" to "1c1cea17d5cd63d68356df2ef31e724dd09f8c26",
        "1.18.2" to "e562f588fea155d96291267465dc3323bfe1551b",
        "1.18.1" to "9717df2acd926bd4a9a7b2ce5f981bb7e4f7f04a",
        "1.18" to "7367ea8b7cad7c7830192441bb2846be0d2ceeac",
        "1.17.1" to "f6cae1c5c1255f68ba4834b16a0da6a09621fe13",
        "1.17" to "84d80036e14bc5c7894a4fad9dd9f367d3000334"
    )

    private const val SPIGOT_BASE_URL = "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings"
    private val versionToCommit = mapOf(
        "1.21.10" to "6cf716da198f9d94f9c36e36d9be6417c5c24824",
        "1.21.9" to "42d18d4c4653ffc549778dbe223f6994a031d69e",
        "1.21.8" to "62f9b85b300b6bbf1c559e379567699a6e281cec",
        "1.21.7" to "436eac9815c211be1a2a6ca0702615f995e81c44",
        "1.21.6" to "281ac0de7a76d808753ede97d11b034bc801b63d",
        "1.21.5" to "702e1a0a5072b2c4082371d5228cb30525687efc",
        "1.21.4" to "3edaf46ec1eed4115ce1b18d2846cded42577e42",
        "1.21.3" to "0c5ebabcb4ce41f69a7d2319b468b6faee434038",
        "1.21.2" to "0ea6fcc9bc8ad9e7c729f5031123bcc69ce2b033",
        "1.21.1" to "533b02cd6ba8dbf8c8607250b02bf2d8c36421e8",
        "1.21" to "ae1e7b1e31cd3a3892bb05a6ccdcecc48c73c455",
        "1.20.6" to "32d1baf2f4e0e7cd1ac22c7b2f6eb4c387e8a343",
        "1.20.5" to "2e2c7cd23daeeddf71226b4e604f8603d71cfca0",
        "1.20.4" to "17eb87e5cd509ff55d691b74902ac19c3b62ca0c",
        "1.20.3" to "58819a3c2a15e6e47ad89d32fb60d44c253491f3",
        "1.20.2" to "172197ceb99364701937947ea7fc424ecf1bb694",
        "1.20.1" to "faff587dcbe915bc565bf01f2d54c6af86039414",
        "1.20" to "2881c6b6dd146126596342d4025d37a9a84a0b03",
        "1.19.4" to "4d9436f7b66190ad21fe4e3975b73a36b7ad2a7e",
        "1.19.3" to "177811e1fa90f674897a302820f3ed84e4d65688",
        "1.19.2" to "d96ad8e1e64b7c35bb632339c23621353be1f028",
        "1.19.1" to "c540b6e228dc33c13c02b2af63a2691cda0cdea8",
        "1.19" to "e6ebde42e39100b18ca0265596b04f557b2b27cc",
        "1.18.2" to "641cb0c939c7c2a3c4b42f2fd7bca7c8b34254ae",
        "1.18.1" to "f31eb04b918592f66c6edd17c9b5998383581ae5",
        "1.18" to "8814411100e91e323a23a9a6355fa7d28e091054",
        "1.17.1" to "a4785704979a469daa2b7f6826c84e7fe886bb03",
        "1.17" to "3cec511b16ffa31cb414997a14be313716882e12",
    )


    fun downloadMappings(
        minecraftVersion: String,
        outputDir: File,
    ) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Fetch Obf mappings
        println("Fetching Obf mappings for Minecraft version $minecraftVersion...")
        val versionHash = versionToHash[minecraftVersion]
            ?: error("Unsupported Minecraft version: $minecraftVersion. Supported versions: ${versionToHash.keys.joinToString(", ")}")
        val obfMappingsUrl = String.format(MOJANG_BASE_URL, versionHash)
        val obfMappingsFile = outputDir.resolve("mojang-mappings.txt")
        if (!obfMappingsFile.exists()) {
            downloadRawFileToDisk(obfMappingsUrl, obfMappingsFile)
        } else {
            println("Obf mappings file already exists: ${obfMappingsFile.absolutePath}")
        }

        // Fetch Spigot mappings
        println("Fetching Spigot mappings for Minecraft version $minecraftVersion...")
        val commitHash = versionToCommit[minecraftVersion]
            ?: error("Unsupported Minecraft version: $minecraftVersion. Supported versions: ${versionToCommit.keys.joinToString(", ")}")
        val mappingsUrl = "$SPIGOT_BASE_URL/bukkit-$minecraftVersion-cl.csrg?at=$commitHash"
        val mappingsFile = outputDir.resolve("spigot-mappings.csrg")
        if (!mappingsFile.exists()) {
            downloadRawFileToDisk(mappingsUrl, mappingsFile)
        } else {
            println("Mappings file already exists: ${mappingsFile.absolutePath}")
        }
    }

    private fun downloadRawFileToDisk(remoteUrl: String, outputFile: File) {
        println("Downloading from $remoteUrl ...")
        val content = URI(remoteUrl).toURL().readText()
        outputFile.writeText(content)
        println("Saved to ${outputFile.absolutePath}")
    }

}
