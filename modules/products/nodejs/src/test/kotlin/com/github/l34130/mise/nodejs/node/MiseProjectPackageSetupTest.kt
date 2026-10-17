package com.github.l34130.mise.nodejs.node

import junit.framework.TestCase

class MiseProjectPackageSetupTest : TestCase() {
    fun `test local package manager works without a lockfile`() {
        assertEquals("pnpm", selectPackageManager(setOf("node", "pnpm"), "npm", false))
        assertEquals("yarn", selectPackageManager(setOf("node", "yarn"), "npm", false))
    }

    fun `test local configuration takes precedence over stale lockfile`() {
        assertEquals("npm", selectPackageManager(setOf("npm"), "pnpm", true))
    }

    fun `test multiple candidates preserve a matching IDE choice`() {
        assertEquals("pnpm", selectPackageManager(setOf("npm", "pnpm"), "pnpm", false))
        assertEquals("npm", selectPackageManager(setOf("npm", "pnpm"), "npm", true))
    }

    fun `test multiple candidates require a matching choice or lockfile`() {
        assertEquals("pnpm", selectPackageManager(setOf("npm", "pnpm"), null, true))
        assertNull(selectPackageManager(setOf("npm", "pnpm"), null, false))
        assertNull(selectPackageManager(setOf("npm", "yarn"), "pnpm", true))
    }

    fun `test projects without local package managers retain legacy selection`() {
        assertEquals("npm", selectPackageManager(setOf("node"), null, false))
        assertEquals("pnpm", selectPackageManager(emptySet(), null, true))
    }
}
