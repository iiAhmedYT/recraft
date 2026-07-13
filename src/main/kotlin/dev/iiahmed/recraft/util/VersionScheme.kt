package dev.iiahmed.recraft.util

object VersionScheme {

    /**
     * True when the Minecraft version uses the new (26.1+) scheme, where the
     * server is Mojang-mapped and CraftBukkit is unrelocated by default.
     *
     * The old scheme is always "1.x.y" (major == 1). The new scheme is "YY.N"
     * (year.minor), so major >= 26. Blank or non-numeric versions (e.g. custom
     * snapshots) return false to keep the legacy remap path.
     */
    fun isMojangOnly(version: String): Boolean {
        val major = version.trim().substringBefore('.').toIntOrNull() ?: return false
        return major >= 26
    }
}
