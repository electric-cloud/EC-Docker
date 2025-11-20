package com.cloudbees.plugin.spec

import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.electriccloud.plugins.annotations.NewFeature
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

class TestConfigurationSpec extends PluginHelper {

    @Shared
    Docker plugin

    @Shared
    def version

    def setupSpec() {
        plugin = Docker.createWithoutConfig()
        ServerHandler.getInstance().setupResource("docker-resource", "docker", 7808)
        version = pluginVersion
    }

    def cleanupSpec() {
        deleteProject("specs-${PLUGIN_NAME}")
        deleteProject("spec-configs-project-${PLUGIN_NAME}")
    }

    @Requires({instance.isVersionAtLeast(instance.version, '2.2.1')})
    def 'test config with credentials and check connection resource - #des'() {
        when:
        def r = plugin
                .testConfiguration.flush()
                .credential(DOCKERHUB_SPECS_USERNAME, DOCKERHUB_SPECS_PASSWORD)
                .registry(DOCKER_HUB_HOST)
                .checkConnectionResource('docker-resource')
                .run()
        then:
        assert r.successful

        where:
        des                                | host
        "empty registry should hit docker" | ''
        "default docker hub registry"      | 'https://registry-1.docker.io/v2/'
    }

    @Requires({instance.isVersionAtLeast(instance.version, '2.2.1')})
    @Unroll
    def 'negative. test config with - #des'() {
        when:
        def r = plugin
                .testConfiguration.flush()
                .credential(user, password)
                .registry(host)
                .checkConnectionResource('docker-resource')
                .run()
        then:
        assert !r.successful

        and:
        assert r.jobLog.contains(resultSummary)

        where:
        des                             | user    | password | host                 | resultSummary
        "invalid username and password" | "wrong" | "wrong"  | 'https://docker.io/' | "unauthorized: incorrect username or password"
    }


    @Requires({instance.isVersionAtLeast(instance.version, '2.2.1')})
    def 'Negative: test config with invalid resource'() {
        when:
        def r = plugin
                .testConfiguration.flush()
                .credential(DOCKERHUB_SPECS_USERNAME, DOCKERHUB_SPECS_PASSWORD)
                .registry(DOCKER_HUB_HOST)
                .checkConnectionResource("wrong-resource")
                .runNaked()
        then:
        assert !r.successful
        assert r.jobLog =~ "NONEXISTENT_RESOURCE"
    }
}
