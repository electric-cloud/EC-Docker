package com.cloudbees.pdk.hen.tests

import com.cloudbees.pdk.hen.Docker
import com.cloudbees.pdk.hen.ServerHandler
import com.cloudbees.pdk.hen.procedures.DockerConfig
import com.electriccloud.plugins.annotations.Regression
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RunDockerRunSuite extends Specification {

    static final String pluginName = 'EC-Docker'

    @Shared
    Docker plugin = Docker.create()
    @Shared
    String defaultProject = 'specs-' + pluginName

    @Sanity
    def 'Sanity RunDockerRun only required fields'() {
        when:
        def result = plugin.runDockerRun
               // .config(plugin.configName)
                .imagename('alpine')
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
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
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
        when: "Run Plugin Procedure - RunDockerRun"
        def result = p.runDockerRun
            //    .config(p.configName)
                .imagename(image)
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand("docker rmi $image || exit 0", 'sh', p.defaultResource)
    }

    @Unroll
    def 'RunDockerRun with additional options parameter'() {
        when:
        def result = plugin.runDockerRun
                .imagename('alpine')
                .additionaloptions('-v $(pwd):$(pwd) -w $(pwd)')
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest || exit 0', 'sh', plugin.defaultResource)
    }

    @Unroll
    def ' RunDockerRun #caseId - negative'() {
        when:
        def result = plugin.runDockerRun
                .imagename(image_name)
                .entrypoint(entry_point)
                .publishedports(published_ports)
                .run()

        then:
        assert result.getOutcome().toString() == 'ERROR'
        def jobLog= result.getJobLog()
        assert jobLog.contains(log_error)
        assert jobLog.contains(exit_code)

        cleanup:
        if(perform_cleanup) {
            ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=created -q)', 'sh', plugin.defaultResource)
         }

        where: 'The following params will be: '
        caseId                               |entry_point  | image_name     |  published_ports| log_error                                                                                                                                                     | exit_code           | perform_cleanup
        'empty image name'                   | ''          | ''             |  ''             | 'Parameter \'image_name\' of procedure \'runDockerRun\' is marked as required, but it does not have a value. Aborting with fatal error.'                      | ''                  | false
        'with wrong alpine image name'       | ''          | 'alpinee'      |  ''             | 'Unable to find image \'alpinee:latest\' locally'                                                                                                             | 'Exit code: 32000'  | false
        'with wrong hello-world image name'  | ''          | 'helllo-world' |  ''             | 'Unable to find image \'helllo-world:latest\' locally'                                                                                                        | 'Exit code: 32000'  | false
        'with wrong entry point'             | '/hellllo'  | 'hello-world'  |  ''             | 'OCI runtime create failed:'                                                                                                                                  | 'Exit code: 32512'  | true
        'with wrong published ports'         | ''          | 'hello-world'  |  ':8080:'       | 'No port specified: :8080:<empty>'                                                                                                                            | 'Exit code: 32000'  | false
    }
}