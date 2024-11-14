package com.github.l34130.mise.core.commands

data class MiseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<MiseVersion> {
    companion object {
        fun from(str: String): MiseVersion {
            val versionStr = str.split(" ").firstOrNull()

            requireNotNull(versionStr) { "Invalid version string: $str" }

            val split = versionStr.split(".")
            require(split.size == 3) { "Invalid version string: $str" }

            return MiseVersion(split[0].toInt(), split[1].toInt(), split[2].toInt())
        }
    }

    override fun compareTo(other: MiseVersion): Int {
        if (major > other.major) {
            return 1
        } else if (major < other.major) {
            return -1
        }

        if (minor > other.minor) {
            return 1
        } else if (minor < other.minor) {
            return -1
        }

        if (patch > other.patch) {
            return 1
        } else if (patch < other.patch) {
            return -1
        }

        return 0
    }
}
