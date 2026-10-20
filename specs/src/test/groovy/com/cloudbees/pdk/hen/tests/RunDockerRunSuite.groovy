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
        def image = 'eftestuser/for-ec-docker-specstests:latest'
        Docker p = Docker.createWithoutConfig()
        DockerConfig conf = DockerConfig
                .create(plugin)
                .debugLevel(DockerConfig.DebugLevelOptions.DEBUG)
        conf.addCredential('credential', Docker.EC_DOCKERHUB_USER, Docker.EC_DOCKERHUB_PASSWORD)
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
        // Previously this relied on the step's shell expanding "$(pwd)" via command
        // substitution. The shell was removed from runDockerRun (no shell = no
        // command injection), so "$(pwd)" is now passed to docker literally - the
        // literal path is resolved here instead, in the test, before being passed in.
        given:
        def pwd = ServerHandler.getInstance().runCommand('pwd', 'sh', plugin.defaultResource).jobLog.trim()
        when:
        def result = plugin.runDockerRun.flush()
                .imagename('alpine')
                .additionaloptions("-v ${pwd}:${pwd} -w ${pwd}")
                .run()

        then:
        assert result.isSuccessful()
        cleanup:
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest || exit 0', 'sh', plugin.defaultResource)
    }

    def 'RunDockerRun with a quoted value in additional_options'() {
        // Proves a quoted value is kept together as one value instead of
        // being torn apart on every space - '-e "MSG=hello world"' must
        // set one env var whose value is "hello world", not two broken
        // tokens. Asserts the container's own printed env line, not the
        // "Step parameters are:" debug dump of the raw input.
        when:
        def result = plugin.runDockerRun.flush()
                .imagename('alpine')
                .additionaloptions('-e "MSG=hello world"')
                .commandwithargs('env')
                .run()

        then:
        assert result.isSuccessful()
        assert result.getJobLog() =~ /(?m)^MSG=hello world$/
        cleanup:
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest || exit 0', 'sh', plugin.defaultResource)
    }

    def 'RunDockerRun with multi-token command_with_args parameter'() {
        when:
        def result = plugin.runDockerRun.flush()
                .imagename('alpine')
                .commandwithargs('echo hello world')
                .run()

        then:
        assert result.isSuccessful()
        // .contains('hello world') would also match the "Step parameters
        // are:" debug dump of the command_with_args input value itself,
        // regardless of whether the container ever ran anything - assert
        // a whole output line instead, which only the container's own
        // "echo hello world" can produce.
        assert result.getJobLog() =~ /(?m)^hello world$/
        cleanup:
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=exited -q)', 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rmi alpine:latest || exit 0', 'sh', plugin.defaultResource)
    }

    @Unroll
    def 'Security - #paramName command injection is neutralized'() {
        given:
        def marker = "/tmp/specs-cdro18006-run-${paramName}-pwned"
        ServerHandler.getInstance().runCommand("rm -f $marker", 'sh', plugin.defaultResource)
        when:
        def proc = plugin.runDockerRun.flush().imagename('alpine')
        proc.addParam(paramName, payload.replace('MARKER', marker))
        def result = proc.run()

        then:
        assert result.getOutcome().toString() == 'ERROR'
        def check = ServerHandler.getInstance().runCommand("test -f $marker && echo FOUND || echo NOTFOUND", 'sh', plugin.defaultResource)
        assert check.jobLog.contains('NOTFOUND')

        cleanup:
        ServerHandler.getInstance().runCommand("rm -f $marker", 'sh', plugin.defaultResource)
        ServerHandler.getInstance().runCommand('docker rm $(docker ps --filter status=created -q) 2>/dev/null || exit 0', 'sh', plugin.defaultResource)

        where: 'The following params will be injected: '
        paramName             | payload
        'container_name'      | 'x"; touch MARKER; echo "'
        'entrypoint'           | 'x"; touch MARKER; echo "'
        'working_dir'          | 'x"; touch MARKER; echo "'
        'published_ports'      | '8080:80$(touch${IFS}MARKER)'
        'container_links'      | 'x; touch MARKER'
        'additional_options'   | '--rm; touch MARKER'
        'command_with_args'    | '; touch MARKER #'
        'image_name'           | 'alpine; touch MARKER'
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
        assert jobLog =~ log_error
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
        'with wrong published ports'         | ''          | 'hello-world'  |  ':8080:'       | 'no port specified|invalid proto'                                                                                                                            | 'Exit code: 32000'  | false
    }
}