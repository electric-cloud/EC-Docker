package com.electriccloud.procedures.installation

import com.electriccloud.procedures.DockerTestBase
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test


@Feature("Installation")
class InstallationTests extends DockerTestBase {


    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        def plugin = ectoolApi.installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(plugin.pluginName).plugin
    }


    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        ectoolApi.deletePlugin(pluginName, pluginVersion)
    }


    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        ectoolApi.deletePlugin(pluginName, pluginVersion)
    }


    @Test
    @Story('Install plugin')
    void installDockerPlugin(){
        def r = ectoolApi.installPlugin(pluginName).plugin
        assert r.pluginName == "${pluginName}-${pluginVersion}"
        assert r.pluginKey == pluginName
        assert r.pluginVersion == pluginVersion
        assert r.lastModifiedBy == "admin"
    }

    @Test
    @Story('Promote plugin')
    void promoteDockerPlugin(){
        def plugin = ectoolApi.installPlugin(pluginName).plugin
        def r = ectoolApi.promotePlugin(plugin.pluginName).plugin
        assert r.pluginName == "${pluginName}-${pluginVersion}"
        assert r.pluginKey == pluginName
        assert r.pluginVersion == pluginVersion
        assert r.lastModifiedBy == "admin"
    }

    @Test
    @Story('Uninstall plugin')
    void uninstallDockerPlugin(){
        def plugin = ectoolApi.installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(plugin.pluginName)
        def r = ectoolApi.uninstallPlugin(plugin.pluginName).property
        assert r.counter == "0"
        assert r.propertyName == "Default"
        assert r.lastModifiedBy == "admin"
        assert r.owner == "admin"
    }


}