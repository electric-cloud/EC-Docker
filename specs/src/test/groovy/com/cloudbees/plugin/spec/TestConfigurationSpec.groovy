package com.cloudbees.plugin.spec

import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.spec.PluginSpockTestSupport
import spock.lang.Shared
import spock.lang.Unroll

class TestConfigurationSpec extends PluginSpockTestSupport{

    @Shared
    Docker plugin


    def setupSpec() {
        plugin = Docker.createWithoutConfig()
        ServerHandler.getInstance().setupResource("docker-resource", "127.0.0.1", 7800)
    }

    def cleanupSpec() {
        deleteProject("specs-EC-Docker")
        deleteProject("spec-configs-project-EC-Docker")
    }


    @NewFeature(pluginVersion = '2.2.1')
    @Unroll
    def 'Test connection with #Des resource'() {
        when:
        def r = plugin
                .testConfiguration.flush()
                .checkConnectionResource(rsrce)
                .runNaked()
        then:
        assert r.successful
        assert r.getJobProperties().get('checkConnectionResource').value  =~ rsrce
        where:
        Des                 | rsrce
        "default resource"  | "local"
        "specific resource" | "docker-resource"
    }

    @NewFeature(pluginVersion = '2.2.1')
    def 'Negative: test config with invalid resource'() {
        when:
        def r = plugin
                .testConfiguration.flush()
                .checkConnectionResource("wrong-resource")
                .runNaked()
        then:
        assert !r.successful
        assert r.jobLog =~ "AGENT ERROR: NONEXISTENT_RESOURCE"
    }
}
