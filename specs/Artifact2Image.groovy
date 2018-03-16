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

    def "war to image"() {
        given:
        def artifactName = 'com.mycompany:HelloWorldWar:1.0.0'
        def imageName = "${getUsername()}/hello-world-war"
        when:
        def result = runProcedureDsl """
runProcedure(
    projectName: '/plugins/EC-Docker/project',
    procedureName: 'Artifact2Image',
    actualParameter: [
        ecp_docker_credential: 'ecp_docker_credential',
        config: '$configName',
        ecp_docker_imageName: '$imageName',
        ecp_docker_artifactName: '$artifactName'
    ],
    credential: [
        credentialName: 'ecp_docker_credential',
        userName: '${getUsername()}',
        password: '${getPassword()}'
    ]
)
"""
        then:
        assert true
        def logs = readJobLogs(result.jobId)

        logger.info(logs)
        assert logs =~ /Image has been built/
        def imageId = getJobProperty("/myJob/parent/${imageName}/imageId", result.jobId)
        assert imageId
        def dockerfile = readDockerfile(result.jobId, artifactName)
        logger.debug(dockerfile)
        assert dockerfile =~ /jetty/
        assert dockerfile =~ /8080/
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
}
