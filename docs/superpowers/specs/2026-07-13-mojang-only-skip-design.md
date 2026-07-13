# Design: Version-Aware Skip of Spigot Remap & CraftBukkit Unrelocation

Date: 2026-07-13

## Problem

Starting with Minecraft 26.1+ (the version scheme change after 1.21.11), server
mappings are always Mojang mappings by default. Two of Recraft's build steps
become pointless for these versions:

- `remapToSpigot` — remaps Mojang-mapped classes to Spigot mappings. Obsolete
  because the runtime is already Mojang-mapped.
- `remapToPaper` — unrelocates versioned CraftBukkit package names
  (`org/bukkit/craftbukkit/vX_Y_RZ/`) back to clean `org/bukkit/craftbukkit/`.
  Obsolete because CraftBukkit is unrelocated by default on 26.1+.

For 26.1+ the plain plugin jar already works everywhere, so no remap/merge is
needed.

Relatedly, the `remapped-mojang` classifier on the compileOnly Spigot dependency
is obsolete for 26.1+ — Spigot's default artifact is already Mojang-mapped — so
the classifier is dropped for the new scheme.

## Version Detection

New helper `dev.iiahmed.recraft.util.VersionScheme`:

```kotlin
object VersionScheme {
    /**
     * True when the Minecraft version uses the new (26.1+) scheme, where the
     * server is Mojang-mapped and CraftBukkit is unrelocated by default.
     *
     * Old scheme is always "1.x.y" (major == 1). New scheme is "YY.N"
     * (year.minor), so major >= 26.
     */
    fun isMojangOnly(version: String): Boolean {
        val major = version.trim().substringBefore('.').toIntOrNull() ?: return false
        return major >= 26
    }
}
```

- Old scheme (`1.21.11`, `1.20.4`, ...) → major `1` → `false`.
- New scheme (`26.1`, `27.3`, ...) → major `>= 26` → `true`.
- Blank / non-numeric (e.g. snapshots) → `false` (safe: keeps the current remap
  path).

## Plugin Changes (`RecraftPlugin`)

Task registration is unchanged: `remapToSpigot`, `remapToPaper`, and
`mergeBothJars` stay registered for all versions (visible in the task list and
still runnable manually). Only the build graph wiring and the published artifact
become version-aware.

### New passthrough task

Register a Gradle `Copy` task `recraftPassthrough`:

- `dependsOn("jar")`
- from the plain jar (`libs/<name>.jar`, honoring `extension.jarFilePattern`)
- into `libs/`, renamed to `<name>-merged.jar`

This keeps the output filename consistent with the old-scheme merge output so
downstream consumers of the `recraft` configuration see the same artifact name.

### afterEvaluate wiring

Inside the existing `afterEvaluate` block (where `minecraftVersion` is known),
after the blank-version guard. The repositories (CodeMC, Minecraft-libraries)
stay added for both schemes; only the compileOnly Spigot dependency classifier,
the published artifact, and the build dependency become version-aware:

```kotlin
val newScheme = VersionScheme.isMojangOnly(version.get())

// Spigot API dependency: 26.1+ ships Mojang-mapped by default, so the
// `remapped-mojang` classifier is obsolete — use the default artifact.
project.dependencies.add(
    "compileOnly",
    if (newScheme) "org.spigotmc:spigot:${version.get()}-R0.1-SNAPSHOT"
    else           "org.spigotmc:spigot:${version.get()}-R0.1-SNAPSHOT:remapped-mojang"
)

if (newScheme) {
    project.logger.lifecycle(
        "Recraft: MC ${version.get()} is Mojang-mapped by default; " +
        "skipping Spigot remap and CraftBukkit unrelocation."
    )
    // recraft artifact = passthrough copy of the plain jar. A Gradle `Copy` task
    // has no single-file output provider, so point the artifact at the known
    // output path and mark it built by the copy task.
    project.artifacts.add(
        "recraft",
        project.layout.buildDirectory.file("libs/${project.name}-merged.jar")
    ) {
        type = "jar"
        builtBy(recraftPassthrough)
    }
    project.tasks.named("build") { dependsOn(recraftPassthrough) }
} else {
    // current behavior: artifact = mergeBothJars output
    project.artifacts.add("recraft", mergeBothJars.flatMap { it.outputJar }) {
        type = "jar"
        builtBy(mergeBothJars)
    }
    project.tasks.named("build") { dependsOn(mergeBothJars) }
}
```

The unconditional `project.tasks.named("build") { dependsOn(mergeBothJars) }`, the
unconditional `artifacts.add(...)`, and the unconditional `compileOnly` spigot
`remapped-mojang` dependency currently at the end of `apply()` / `afterEvaluate`
move into this branch so the new scheme neither drags in the remap/merge chain nor
requests the obsolete classifier.

### Left unchanged (out of scope)

- The CodeMC / Minecraft-libraries repositories stay added for both schemes.
- `RemapToSpigot`, `RemapToPaper`, `MergeJars`, `MultiRelease` task
  implementations are untouched.

## Testing / Verification

- `isMojangOnly` unit-level checks: `1.21.11` → false, `1.20.4` → false,
  `26.1` → true, `27.0` → true, `""` → false, `"snapshot"` → false.
- Manual: a consuming build with `minecraftVersion = "26.1"` produces
  `<name>-merged.jar` equal to the plain jar and does not run `remapToSpigot` /
  `remapToPaper` as part of `build`.
- Regression: a build with `minecraftVersion = "1.21.11"` still runs the full
  remap + merge chain and produces the merged artifact as before.
