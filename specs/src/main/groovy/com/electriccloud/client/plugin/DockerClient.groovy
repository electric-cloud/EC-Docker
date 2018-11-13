package com.electriccloud.client.plugin

import com.electriccloud.client.commander.CommanderClient
import groovy.json.JsonBuilder
import io.qameta.allure.Step

import static com.electriccloud.helpers.config.ConfigHelper.yamlPath
import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath



class DockerClient extends CommanderClient {

    DockerClient() {
        this.timeout = 400
        this.plugin = 'docker'
    }



    @Step("Create configuration: {configurationName}, {dockerEndpoint}")
    def createConfiguration(configurationName, dockerEndpoint, userName, caCert = "", cert = "", key = "", testConnection = true, logLevel = DEBUG) {
        message("creating docker config")
        def params = [params: [config: configurationName,
                               dockerEndpoint: dockerEndpoint,
                               username: userName,
                               caPem: caCert,
                               certPem: cert,
                               keyPem: key,
                               testConnection: testConnection.toString(),
                               logLevel: logLevel]]
        def response = client.dslFileMap(dslPath(plugin, 'createConfig'), params)
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configurationName} with endpoint ${dockerEndpoint} is successfully created.")
        response
    }

    @Step("Delete configuration: {confName}")
    def deleteConfiguration(confName) {
        message("removing configuration")
        def response = client.dslFileMap(dslPath(plugin, 'deleteConfig'), [params: [configName: confName]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${confName} is successfully deleted.")
        return response
    }

    @Step("Create environment: {configurationName}")
    def createEnvironment(configName) {
        message("environment creation")
        def response = client.dslFileMap(dslPath('flow', 'environment'), [params: [configName: configName]])
        client.log.info("Environment for project: ${response.json.project.projectName} is created")
        response
    }

    @Step
    def createService(replicaNum, volumes = [source: null, target: null ]) {
        message("service creation")
        def response = client.dslFileMap dslPath('flow', 'service'),
                [params: [replicas: replicaNum,
                          sourceVolume: volumes.source,
                          targetVolume: volumes.target]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        response
    }

    @Step
    def createApplication(replicaNum, volumes = [source: null, target: null ]) {
        message("application creation")
        def response = client.dslFileMap dslPath('flow', 'application'),
                [params: [replicas: replicaNum,
                          sourceVolume: volumes.source,
                          targetVolume: volumes.target]]
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        response
    }


    @Step
    def checkCluster(configName, resourceName = "local"){
        message("checking cluster")
        def response = client.dslFileMap dslPath(plugin, 'checkCluster'),
                [params: [config: configName,
                          resource: resourceName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Cluster was checked successfully.")
        response
    }

    @Step
    def removeService(configName, serviceName, resourceName = "local"){
        message("removing cluster")
        def response = client.dslFileMap dslPath(plugin, 'removeService'),
                [params: [config: configName,
                          service: serviceName,
                          resource: resourceName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Service: $serviceName was removed successfully.")
        response
    }

    @Step
    def deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName, serviceRevId = "", applicationName = "", applicationRevId = "", resourceName = "local"){
        message("service deployment")
        def response = client.dslFileMap dslPath(plugin, 'deployService'),
                [params: [service: serviceName,
                        serviceRevId: serviceRevId,
                        serviceProject: projectName,
                        application: applicationName,
                        applicationRevId: applicationRevId,
                        cluster: clusterName,
                        envProject: environmentProjectName,
                        envName: environmentName,
                        resource: resourceName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Deployment is successfully completed.")
        response
    }

    @Step
    def undeploy(projectName, environmentProjectName, environmentName, clusterName, serviceName, serviceRevId = "", applicationName = "", applicationRevId = "", resourceName = "local"){
        message("service undeploy")
        def response = client.dslFileMap dslPath(plugin, 'undeployService'),
                [params: [service: serviceName,
                        serviceRevId: serviceRevId,
                        serviceProject: projectName,
                        application: applicationName,
                        applicationRevId: applicationRevId,
                        cluster: clusterName,
                        envProject: environmentProjectName,
                        envName: environmentName,
                        resource: resourceName]]
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Undeploy is successfully completed.")
        response
    }


    @Step("Push to registry: {artifact}:{version}")
    def pushToRegistry(configName, artifactName, artifactVersion, artifactLocation = null, credential, artifactory, imageName, registryUrl = null, baseImage, exposePorts = '8081', command, environments, removeAfter, resourceName = "local"){
        def params = [params: [credentialName: credential.credName,
                configName: configName,
                imageName: imageName,
                artifactName: artifactName,
                artifactLocation: artifactLocation,
                artifactoryArtifactName: artifactory.artifactName,
                artifactoryConfig: artifactory.config,
                artifactoryArtifactExtension: artifactory.artifactExtension,
                artifactoryOrgPath: artifactory.orgPath,
                artifactoryRepoKey: artifactory.repoKey,
                artifactoryRepoType: artifactory.repoType,
                artifactoryArtifactVersion: artifactory.artifactVersion,
                exposePorts: exposePorts,
                env: environments,
                cmd: command,
                baseImage: baseImage,
                registryUrl: registryUrl,
                artifactVersion: artifactVersion,
                removeAfterPush: removeAfter.toString(), userName: credential.userName, password: credential.password, resource: resourceName]]
        message("running artifact2image procedure")
        def response = client.dslFileMap dslPath(plugin, 'artifact2image'), params
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Artifact2Image procedure is successfully completed.")
        response
    }


    @Step("Create Network {networkName}")
    def createNetwork(configName, networkName, subnetList = "", gateways = "", labels = "", mtu = "", enableIpV6 = true, resource = "local"){
        message("creating network: $networkName")
        def params = [params: [config: configName,
                               network: networkName,
                               subnetList: subnetList,
                               eanbleIPV6: enableIpV6.toString(),
                               gateways: gateways,
                               labels: labels,
                               mtu: mtu,
                               resource: resource]]
        def response = client.dslFileMap dslPath(plugin, 'createNetwork'), params
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Netwotk $networkName is successfully created.")
        response
    }

    @Step("Delete Network {networkName}")
    def deleteNetwork(configName, networkName, resource){
        message("removing network: $networkName")
        def params = [params: [config: configName,
                               network: networkName,
                               resource: resource]]
        def response = client.dslFileMap dslPath(plugin, 'deleteNetwork'), params
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Netwotk $networkName is successfully removed.")
        response
    }



}
