# Recraft

Recraft is a Gradle Plugin I made especially for ModernDisguise my library.
It allows you to easily create 2 classes of the same class, once remapped for paper and once for spigot.
Also adds a prefix before the class name to avoid conflicts.

## ➕ Add to your project
Add this repo to your plugin repositories:
```kt
pluginManagement {
    repositories {
        gradlePluginPortal() // Incase you use other plugins
        maven("https://repo.gravemc.net/releases") // GraveMC's Maven Repository
    }
}
```

and then add this to your plugins block:
```kt
plugins {
    id("dev.iiahmed.recraft") version "1.0.0"
}
```

## 🧑‍💻 Usage
In your `build.gradle.kts` file, After you add the plugin, you can use the `recraft` block to configure it:

```kt
recraft {
    // Set The Minecraft version you are targeting
    minecraftVersion.set("1.20.4")

    // OPTIONAL:
    // The prefix to use for the remapped classes
    paperPrefix.set("S")
    spigotPrefix.set("S")

    // Set the pattern for the jar file (starting from the `build` directory)
    jarFilePattern.set("libs/${project.name}-${project.version}.jar") // The default is "libs/${project.name}.jar"
}
```
## Supported Versions
The plugin supports Minecraft versions from 1.17.1 to 1.21.7, and it will automatically download the necessary mappings for you.

To add support for a new version, it's not as complicated.
You need to add the mappings to the `util/MappingFetcher` object, and then update 2 maps.

#### Mojang Mappings
First Map is the `versionToHash` map, you can get the hash by visitng the [Mojang Versions](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
page, and then find the version you want to add, and open the URL from 'url' field.

then in that page you can find the hash in the 'downloads' field under 'server_mappings'.

The url should look like this: `https://piston-data.mojang.com/v1/objects/<hash>/server.txt`
Just get the hash and add it to the map along with the version you want to add.

#### Spigot Mappings
The second map is the `versionToCommit` map, you can get the hash by visiting the [SpigotMC Versions](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits) page,
and find a commit that matches the version you want to add. after you find the commit, just copy the hash and add it to the map along with the version you want to add.

## License
This plugin is licensed under the [MIT License]()