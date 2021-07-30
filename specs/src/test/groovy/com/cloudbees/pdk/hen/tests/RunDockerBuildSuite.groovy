package com.cloudbees.pdk.hen.tests

import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.cloudbees.pdk.hen.procedures.DockerConfig
import com.electriccloud.plugins.annotations.Regression
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Specification

class RunDockerBuildSuite extends Specification {

    static final String pluginName = 'EC-Docker'

    static final String DockerfileContent = "FROM alpine\nRUN echo 'hello world'"
    static final String DockerfileContentPrivateImage = "FROM specstests/for_ec_docker_specstests:latest\nRUN echo 'hello world'"

    @Shared
    Docker plugin = Docker.create()
    @Shared
    String defaultProject = 'specs-' + pluginName

    @Sanity
    def 'Sanity RunDockerBuild only required fields'() {
        given:
        def cmd = "mkdir /tmp/specs-dockerfile && echo \"$DockerfileContent\" >> /tmp/specs-dockerfile/Dockerfile"
        ServerHandler.getInstance().runCommand(cmd, 'sh', plugin.defaultResource)
        when:
        def result = plugin.runDockerBuild
                .buildpath('/tmp/specs-dockerfile/')
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
        ServerHandler.getInstance().runCommand('rm /tmp/specs-dockerfile -r', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest || exit 0', 'sh', plugin.defaultResource)
    }

    @Regression
    def 'Regression RunDockerRun private image'() {
        given: "Create plugin config with DockerHub credentials"
        def image = 'specstests/for_ec_docker_specstests:latest'
        Docker p = Docker.createWithoutConfig()
        DockerConfig conf = DockerConfig
                .create(plugin)
                .debugLevel(DockerConfig.DebugLevelOptions.DEBUG)
        conf.addCredential('credential', Docker.DOCKERHUB_SPECS_USERNAME, Docker.DOCKERHUB_SPECS_PASSWORD)
        p.configure(conf)
        def cmd = "mkdir /tmp/specs-dockerfile && echo \"$DockerfileContentPrivateImage\" >> /tmp/specs-dockerfile/Dockerfile"
        ServerHandler.getInstance().runCommand(cmd, 'sh', p.defaultResource)
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
        when: "Run Plugin Procedure - RunDockerBuild"
        def result = p.runDockerBuild
                .buildpath('/tmp/specs-dockerfile/')
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
        ServerHandler.getInstance().runCommand('rm /tmp/specs-dockerfile -r', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', p.defaultResource)
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
    }
}