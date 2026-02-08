package com.github.l34130.mise.core

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MiseTrackedConfigServiceTest : BasePlatformTestCase() {
    
    fun `test service initialization`() {
        // Simply verify that the service can be instantiated without errors
        val service = project.service<MiseTrackedConfigService>()
        assertNotNull("MiseTrackedConfigService should be available", service)
        
        // Initially the tracked configs should be empty or loading
        val configs = service.getTrackedConfigs()
        assertNotNull("Tracked configs should not be null", configs)
    }
    
    fun `test isTrackedConfig with non-existent file`() {
        val service = project.service<MiseTrackedConfigService>()
        
        // A non-existent file should not be tracked
        val isTracked = service.isTrackedConfig("/non/existent/file.env")
        assertFalse("Non-existent file should not be tracked", isTracked)
    }
    
    fun `test getTrackedConfigs returns set`() {
        val service = project.service<MiseTrackedConfigService>()
        
        // getTrackedConfigs should return a Set
        val configs = service.getTrackedConfigs()
        assertNotNull("getTrackedConfigs should return a non-null set", configs)
        assertTrue("getTrackedConfigs should return a Set", configs is Set<String>)
    }
}
