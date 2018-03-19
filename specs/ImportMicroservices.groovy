import spock.lang.*
import com.electriccloud.spec.*

class ImportMicroservices extends DockerHelper {
    static def dockerYAMLFile
    /*static def projectName = 'EC-Docker Specs Import Microservices'
    static def envName = 'Docker Spec Env'
    static def serviceName = 'Docker Import Microservices'
    static def clusterName = 'Docker Spec Cluster'*/
    static def projectName = 'Default'
    static def envName = 'Default'
    static def serviceName = 'Default'
    static def clusterName = 'Cluster 1'
    static def envProjectName = 'Default'
    static def applicationName = 'Docker Application Test'
    static def configName

    def doSetupSpec() {
        configName = 'Docker'
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
            def sampleName = 'my-service-nginx-deployment'
            cleanupService(sampleName)
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
            assert result.outcome != 'error'
            assert service.service
            def containers = service.service.container
            assert containers.size() == 1
            assert containers[0].containerName == 'jira'
            assert containers[0].imageName == 'electricflow'
            //assert containers[0].imageVersion == '1.7.9'
            def port = containers[0].port[0]
            assert port
            assert service.service.defaultCapacity == '3'
            //assert port.containerPort == '80'
            //assert service.service.port[0].listenerPort == '80'
    }

    def "servicec application scoped deploy"() {
        given:
        def sampleName = 'my-service-nginx-deployment'
        cleanupService(sampleName)
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
        assert result.outcome != 'error'
        assert service.service
        def containers = service.service.container
        assert containers.size() == 1
        assert containers[0].containerName == 'jira'
        assert containers[0].imageName == 'electricflow'
        //assert containers[0].imageVersion == '1.7.9'
        def port = containers[0].port[0]
        assert port
        assert service.service.defaultCapacity == '3'
    }

}
