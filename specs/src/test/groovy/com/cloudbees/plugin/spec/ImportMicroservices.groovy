package com.cloudbees.plugin.spec

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
        createConfig(configName)
        createCluster(projectName, envName, clusterName, configName)
        dslFile 'dsl/importMicroservices.dsl', [
            projectName: projectName,
            params     : [
                docker_compose_file_content: '',
                docker_project             : '',
                docker_application_scoped  : '',
                docker_application         : '',
                docker_environment_project : '',
                docker_environment         : '',
                docker_cluster             : ''
            ]
        ]

    }

    def doCleanupSpec() {
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def "service deploy"() {
        given:
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
                        docker_compose_file_content: '''$dockerYAMLFile''',
                        docker_project: '$projectName',
                        docker_environment_project: '$projectName',
                        docker_environment: '$envName',
                        docker_cluster: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome in ['success', 'warning']
        def service = dsl "getService(projectName: '$projectName', serviceName: 'jira')"
        assert service
        def container = dsl "getContainer(projectName: '$projectName', serviceName: 'jira', containerName: 'jira')"
        logger.debug(objectToJson(container))
        assert container.container.imageName == 'electricflow/jira6'
    }

    def "service application scoped deploy"() {
        given:
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
                        docker_compose_file_content: '''$dockerYAMLFile''',
                        docker_project: '$projectName',
                        docker_application_scoped: '1',
                        docker_application: '$applicationName',
                        docker_environment_project: '$projectName',
                        docker_environment: '$envName',
                        docker_cluster: '$clusterName'
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome in ['success','warning']
        def service = dsl "getService(projectName: '$projectName', serviceName: 'jira', applicationName: '$applicationName')"
        logger.debug(objectToJson(service))
        assert service
        cleanup:
        dsl "deleteApplication(projectName: '$projectName', applicationName: '$applicationName')"
    }

    def "dependent steps"() {
        given:
        def appName = "Dependent Steps"
        dockerYAMLFile = """
version: '3.3'

services:
   db:
     image: mysql:5.7
     volumes:
       - db_data:/var/lib/mysql
     restart: always
     environment:
       MYSQL_ROOT_PASSWORD: somewordpress
       MYSQL_DATABASE: wordpress
       MYSQL_USER: wordpress
       MYSQL_PASSWORD: wordpress

   wordpress:
     depends_on:
       - db
     image: wordpress:latest
     ports:
       - "8000:80"
     restart: always
     environment:
       WORDPRESS_DB_HOST: db:3306
       WORDPRESS_DB_USER: wordpress
       WORDPRESS_DB_PASSWORD: wordpress
volumes:
    db_data:

"""
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        docker_compose_file_content: '''$dockerYAMLFile''',
                        docker_project: '$projectName',
                        docker_application_scoped: '1',
                        docker_application: '$appName',
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome == 'success'
        def processSteps = dsl """
            getProcessSteps(
                applicationName: '$appName',
                projectName: '$projectName',
                processName: 'Deploy',
            )
        """
        logger.debug(objectToJson(processSteps))
        def dependencies = dsl """
            getProcessDependencies(
                projectName: '$projectName',
                applicationName: '$appName',
                processName: 'Deploy',
            )
        """
        logger.debug(objectToJson(dependencies))
        assert dependencies.processDependency.size() == 1
        assert dependencies.processDependency.getAt(0).sourceProcessStepName == 'deploy-db'
        assert dependencies.processDependency.getAt(0).targetProcessStepName == 'deploy-wordpress'
        cleanup:
        dsl """
            deleteApplication(projectName: '$projectName', applicationName: '$appName')
        """
    }

    def "multi-level dependencies"() {
        given:
        def compose = '''
version: '3.3'

services:
   db:
     image: mysql:5.7
     restart: always
   redis:
     image: 'redis'
   backend:
     depends_on:
       - db
       - redis
     image: backend_image
     ports:
       - "8000:80"
     restart: always
   frontend:
     depends_on:
       - backend
     image: 'frontend'
'''
        def appName = 'Multi-Level Dependencies Spec'
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        docker_compose_file_content: '''$compose''',
                        docker_project: '$projectName',
                        docker_application_scoped: '1',
                        docker_application: '$appName',
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        def dependencies = dsl """
            getProcessDependencies(
                projectName: '$projectName',
                applicationName: '$appName',
                processName: 'Deploy',
            )
        """
        logger.debug(objectToJson(dependencies))
        assert dependencies.processDependency.find {
            it.sourceProcessStepName == 'deploy-redis' && it.targetProcessStepName == 'deploy-db'
        }
        assert dependencies.processDependency.find {
            it.sourceProcessStepName == 'deploy-db' && it.targetProcessStepName == 'deploy-backend'
        }
        assert dependencies.processDependency.find {
            it.sourceProcessStepName == 'deploy-backend' && it.targetProcessStepName == 'deploy-frontend'
        }
        cleanup:
        dsl "deleteApplication(projectName: '$projectName', applicationName: '$appName')"
    }


    def "non-existing dependency"() {
        given:
        def compose = '''
version: '3.3'

services:
   db:
     image: mysql:5.7
     restart: always
     depends_on:
      - no_such_service
'''
        def appName = 'Non-existing Dependencies Spec'
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        docker_compose_file_content: '''$compose''',
                        docker_project: '$projectName',
                        docker_application_scoped: '1',
                        docker_application: '$appName',
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome == 'success'
        def dependencies = dsl """
            getProcessDependencies(
                projectName: '$projectName',
                applicationName: '$appName',
                processName: 'Deploy',
            )
        """
        logger.debug(objectToJson(dependencies))
        cleanup:
        dsl "deleteApplication(projectName: '$projectName', applicationName: '$appName')"

    }

    def "circular dependency"() {
        given:
        def compose = '''
version: '3.3'

services:
   frontend:
     image: mysql:5.7
     restart: always
     depends_on:
      - backend
   backend:
     image: backend
     depends_on:
       - redis
   redis:
     image: redis
     depends_on:
       - frontend
'''
        def appName = 'Circular Dependencies Spec'
        when:
        def result = runProcedureDsl """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: 'Import Microservices',
                    actualParameter: [
                        docker_compose_file_content: '''$compose''',
                        docker_project: '$projectName',
                        docker_application_scoped: '1',
                        docker_application: '$appName',
                    ]
                )
            """
        then:
        logger.debug(result.logs)
        assert result.outcome == 'error'
        assert result.logs =~ /Circular dependency found/
        cleanup:
        dsl "deleteApplication(projectName: '$projectName', applicationName: '$appName')"

    }

}
