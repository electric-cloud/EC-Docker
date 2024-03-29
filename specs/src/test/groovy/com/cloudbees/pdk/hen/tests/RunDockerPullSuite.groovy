package com.cloudbees.pdk.hen.tests

import com.cloudbees.pdk.hen.ConfigurationHandling
import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.cloudbees.pdk.hen.procedures.DockerConfig
import com.electriccloud.plugins.annotations.Regression
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.cloudbees.pdk.hen.Utils.*

class RunDockerPullSuite extends Specification {

    static final String pluginName = 'EC-Docker'

    @Shared
    Docker plugin = Docker.create()
    @Shared
    String defaultProject = 'specs-' + pluginName
    def setupSpec() {
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest -f || exit 0', 'sh', plugin.defaultResource)
    }

    @Sanity
    def 'Sanity RunDockerPull only required fields'() {
        given:
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest -f || exit 0', 'sh', plugin.defaultResource)
        when:
        def result = plugin.runDockerPull
                .imagename('alpine')
                .run()

        then:
        assert result.isSuccessful()
        assert result.jobLog =~ 'Status: Downloaded newer image for alpine:latest'
        cleanup:
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest -f || exit 0', 'sh', plugin.defaultResource)
    }

    @Regression
    def 'Regression RunDockerPull private image'() {
        given: "Create plugin config with DockerHub credentials"
        def image = 'specstests/for_ec_docker_specstests:latest'
        Docker p = Docker.createWithoutConfig()
        DockerConfig conf = DockerConfig
                .create(plugin)
                .debugLevel(DockerConfig.DebugLevelOptions.DEBUG)
        conf.addCredential('credential', Docker.DOCKERHUB_SPECS_USERNAME, Docker.DOCKERHUB_SPECS_PASSWORD)
        p.configure(conf)
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
        when: "Run Plugin Procedure - RunDockerPull"
        def result = p.runDockerPull
                .config(p.configName)
                .imagename(image)
                .run()

        then:
        assert result.isSuccessful()
        assert result.jobLog =~ "Status: Downloaded newer image for $image"
        cleanup:
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
    }

    @Unroll
    def 'RunDockerPull #caseId - negative'() {
        when:
        def result = plugin.runDockerPull
                .imagename(imageName)
                .run()

        then:
        assert result.getOutcome().toString() == 'ERROR'
        def jobLog= result.getJobLog()
        assert jobLog.contains(logError)
        assert jobLog.contains(exitCode)

        where: 'The following params will be: '
        caseId                     | imageName             |  logError                                                                                                                    | exitCode
        'with not existing image'  | 'alpinesssx'          |  'Error response from daemon: pull access denied for alpinesssx, repository does not exist or may require \'docker login\''  | 'Exit code: 256'
        'with not existing tag'    | 'alpine:357.331.128'  |  'Error response from daemon: manifest for alpine:357.331.128 not found: manifest unknown: manifest unknown'                 | 'Exit code: 256'
    }
}