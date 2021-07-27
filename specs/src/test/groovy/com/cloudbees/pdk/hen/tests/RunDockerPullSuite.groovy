package com.cloudbees.pdk.hen.tests

import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.cloudbees.pdk.hen.procedures.DockerConfig
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Specification

import static com.cloudbees.pdk.hen.Utils.*

class RunDockerPullSuite extends Specification {

    static final String pluginName = 'EC-Docker'

    @Shared
    Docker plugin = Docker.create()
    @Shared
    String defaultProject = 'specs-' + pluginName
    def setupSpec() {
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest', 'sh', plugin.defaultResource)
    }

    @Sanity
    def 'Sanity RunDockerPull only required fields'() {
        when:
        def result = plugin.runDockerPull
               // .config(plugin.configName)
                .imagename('alpine')
                .run()

        then:
        assert result.isSuccessful()
        assert result.jobLog =~ 'Status: Downloaded newer image for alpine:latest'
        cleanup:
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest', 'sh', plugin.defaultResource)
    }
}