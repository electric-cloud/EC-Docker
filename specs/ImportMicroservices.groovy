import spock.lang.*
import com.electriccloud.spec.*

class ImportMicroservices extends DockerHelper {
    static def dockerYAMLFile
    /*static def projectName = 'EC-Docker Specs Import Microservices'
    static def envName = 'Docker Spec Env'
    static def serviceName = 'Docker Import Microservices'
    static def clusterName = 'Docker Spec Cluster'*/
    static def projectName = 'EC-Docker Specs Import Microservices'
    static def envName = 'Default'
    static def serviceName = 'Default'
    static def clusterName = 'Cluster 1'
    static def envProjectName = 'Default'
    static def applicationName = 'Docker Application Test'
    static def configName = 'Docker'

    def doSetupSpec() {
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/importMicroservices.dsl', [
                projectName: projectName,
                params: [
                        ec_docker_compose_file_content: '',
                        ec_docker_project: '',
                        ec_docker_application_scoped: '',
                        ec_docker_application: '',
                        ec_docker_environment_project: '',
                        ec_docker_environment: '',
                        ec_docker_cluster: ''
                ]
        ]

    }

    def doCleanupSpec() {
        cleanupCluster(configName)
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def "servicec deploy"() {
        given:
            def sampleName = 'my-service--deploy'
            dockerYAMLFile = 
'''
version: '3\'
#
services:
  electric-flow:
    image: "ecdocker/eflow-ce-server"
    
    ports:
      - "8000:8000"
      - "8443:8443"
      - "443:443"
      - "80:80"
    networks:
      - internal
    container_name: electricflow
    tty: true
  jira:
    image: electricflow/jira6
    ports:
      - "8080:8080"
    networks:
      - internal
    container_name: jira
    tty: true
#
networks:
  internal:
    driver: bridge
'''
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        ec_docker_compose_file_content: '$dockerYAMLFile',
                        ec_docker_project: '$projectName',
                        ec_docker_environment_project: '$envProjectName',
                        ec_docker_environment: '$envName',
                        ec_docker_cluster: '$clusterName'
                    ]
                )
            """
        then:
            logger.debug(result.logs)
            def service = getService(
                projectName,
                sampleName,
                clusterName,
                envName
            )
    }

    def "servicec application scoped deploy"() {
        given:
        def sampleName = 'my-service-application-deploy'
        dockerYAMLFile =
                '''
version: '3\'
#
services:
  electric-flow:
    image: "ecdocker/eflow-ce-server"
    
    ports:
      - "8000:8000"
      - "8443:8443"
      - "443:443"
      - "80:80"
    networks:
      - internal
    container_name: electricflow
    tty: true
  jira:
    image: electricflow/jira6
    ports:
      - "8080:8080"
    networks:
      - internal
    container_name: jira
    tty: true
#
networks:
  internal:
    driver: bridge
'''
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        ec_docker_compose_file_content: '$dockerYAMLFile',
                        ec_docker_project: '$projectName',
                        ec_docker_application_scoped: '1',
                        ec_docker_application: '$applicationName',
                        ec_docker_environment_project: '$envProjectName',
                        ec_docker_environment: '$envName',
                        ec_docker_cluster: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def service = getService(
                projectName,
                sampleName,
                clusterName,
                envName
        )
    }

}
