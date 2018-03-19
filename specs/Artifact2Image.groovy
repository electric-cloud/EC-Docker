import spock.lang.*
import com.electriccloud.spec.*

class Artifact2Image extends DockerHelper {
    static def projectName = 'EC-Docker Specs Artifact2Image'
    static def configName = 'EC-Docker Specs'


    def doSetupSpec() {
        createConfig(configName)
        dslFile "dsl/helper.dsl", [
            projectName: projectName
        ]
    }

    @Unroll
    @Ignore
    def "war to image"() {
        given:
        def artifactName = 'ec-specs:HelloWorldWar:1.0.0'
        def imageName = "${getUsername()}/hello-world-war"
        publishArtifact('ec-specs:HelloWorldWar', '1.0.0', 'hello-world.war')
        when:
        def result = runProcedureDsl """
runProcedure(
    projectName: '/plugins/EC-Docker/project',
    procedureName: 'Artifact2Image',
    actualParameter: [
        ecp_docker_credential: 'ecp_docker_credential',
        config: '$configName',
        ecp_docker_imageName: '$imageName',
        ecp_docker_artifactName: '$artifactName',
        ecp_docker_ports: '$ports',
        ecp_docker_env: '$env',
        ecp_docker_command: '$command',
        ecp_docker_baseImage: '$baseImage'
    ],
    credential: [
        credentialName: 'ecp_docker_credential',
        userName: '${getUsername()}',
        password: '${getPassword()}'
    ]
)
"""
        then:
        def logs = readJobLogs(result.jobId)

        logger.info(logs)
        assert logs =~ /Image has been built/
        def imageId = getJobProperty("/myJob/parent/${imageName}/imageId", result.jobId)
        assert imageId
        def dockerfile = readDockerfile(result.jobId, artifactName)
        logger.debug(dockerfile)
        if (ports) {
            assert dockerfile =~ /$ports/
        }
        else {
            assert dockerfile =~ /8080/
        }

        if (baseImage) {
            assert dockerfile =~ /$baseImage/
        }
        else {
            assert dockerfile =~ /jetty/
        }
        if (command) {
            assert dockerfile =~ /$command/
        }
        if (env) {
            assert dockerfile =~ /$env/
        }
        where:
        ports      | baseImage       |  command      | env
        '8081'     | 'tomcat:alpine' | ''            | 'var=value'
        ''         | ''              | 'ls /tmp'     | ''
    }

    @Unroll
    def "jar to image"() {
        given:
        def artifactName = "ec-specs:HelloSpringBoot:1.0.0"
        def imageName = "${getUsername()}/hello-spring-boot"
        publishArtifact("ec-specs:HelloSpringBoot", "1.0.0", "hello-spring-boot.jar")
        def client = new DockerHubClient(this, getUsername(), getPassword())
        try {
            client.deleteRepository("hello-spring-boot")
        } catch (Throwable e) {
            logger.debug(e.getMessage())
        }
        when:
        def result = runProcedureDsl """
runProcedure(
    projectName: '/plugins/EC-Docker/project',
    procedureName: 'Artifact2Image',
    actualParameter: [
        ecp_docker_credential: 'ecp_docker_credential',
        config: '$configName',
        ecp_docker_imageName: '$imageName',
        ecp_docker_artifactName: '$artifactName',
        ecp_docker_ports: '$ports',
        ecp_docker_env: '$env',
        ecp_docker_command: '$command',
        ecp_docker_baseImage: '$baseImage'
    ],
    credential: [
        credentialName: 'ecp_docker_credential',
        userName: '${getUsername()}',
        password: '${getPassword()}'
    ]
)
"""
        then:
        def logs = readJobLogs(result.jobId)
        logger.info(logs)
        assert logs =~ /Image has been built/
        def imageId = getJobProperty("/myJob/parent/${imageName}/imageId", result.jobId)
        assert imageId
        def dockerfile = readDockerfile(result.jobId, artifactName)
        logger.debug(dockerfile)

        def repoData = client.getRepository("hello-spring-boot")
        assert repoData
        if (ports) {
            assert dockerfile =~ /$ports/
        }
        else {
            assert dockerfile =~ /8080/
        }

        if (baseImage) {
            assert dockerfile =~ /$baseImage/
        }
        else {
            assert dockerfile =~ /openjdk/
        }
        if (command) {
            assert dockerfile =~ /$command/
        }
        if (env) {
            assert dockerfile =~ /$env/
        }
        where:
        ports      | baseImage            |  command      | env
        '8081'     | 'openjdk:8-jre-slim' | ''            | 'var=value'
        ''         | ''                   | 'ls /tmp'     | ''
    }

    def getUsername() {
        def username = System.getenv('EC_DOCKERHUB_USER')
        assert username
        username
    }

    def getPassword() {
        def password = System.getenv('EC_DOCKERHUB_PASSWORD')
        assert password
        password
    }

    def readDockerfile(jobId, artifactName) {
        def path = getJobWorkspace(jobId)
        def res = dsl """
runProcedure(
    projectName: '$projectName',
    procedureName: 'Read Dockerfile',
    actualParameter: [
        artifactName: '$artifactName',
        path: '$path',
    ]
)
"""
        waitUntil {
            jobCompleted(res)
        }
        def dockerfile = getJobProperty("/myJob/dockerfile", res.jobId)
        assert dockerfile
        dockerfile
    }

    def runCommand(command) {
        logger.info("Command: $command")
        def process = command.execute()
        process.waitFor()
        String text = process.text
        logger.info(text)
        assert process.exitValue() == 0
        text
    }

    def publishArtifact(String artifactName, String version, String resName) {
        File resource = new File(this.getClass().getResource("/resources/${resName}").toURI())

        String commanderServer = System.getProperty("COMMANDER_SERVER") ?: 'localhost'
        String username = System.getProperty('COMMANDER_USER') ?: 'admin'
        String password = System.getProperty('COMMANDER_PASSWORD') ?: 'changeme'

        runCommand("ectool --server $commanderServer login $username $password")
        runCommand("ectool --server $commanderServer deleteArtifactVersion ${artifactName}:${version}")

        String publishCommand = "ectool --server ${commanderServer} publishArtifactVersion --version $version --artifactName ${artifactName} "
        if (resource.directory) {
            publishCommand += "--fromDirectory ${resource}"
        }
        else {
            publishCommand += "--fromDirectory ${resource.parentFile} --includePatterns $resName"
        }
        runCommand(publishCommand)
    }
}
