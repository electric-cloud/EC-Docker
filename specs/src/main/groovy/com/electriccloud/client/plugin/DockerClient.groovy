package com.electriccloud.client.plugin

import com.electriccloud.client.commander.CommanderClient
import io.qameta.allure.Step

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath



class DockerClient extends CommanderClient {


    DockerClient() {
        this.timeout = 400
        this.plugin = 'docker'
    }


    @Step("Create configuration: {configurationName}, {dockerEndpoint}")
    def createConfiguration(configurationName,
                            dockerEndpoint,
                            userName,
                            caCert = "",
                            cert = "",
                            key = "",
                            testConnection = true, logLevel = DEBUG) {
        message("creating docker config")
        def json = jsonHelper.configJson(configurationName, dockerEndpoint, userName, caCert, cert, key, testConnection.toString(), logLevel)
        def response = client.dslFile(dslPath(plugin, 'config'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configurationName} with endpoint ${dockerEndpoint} is successfully created.")
        return response
    }

    @Step("Create environment: {configurationName}")
    def createEnvironment(configName) {
        message("environment creation")
        def response = client.dslFile(dslPath(plugin, 'environment'), client.encode(jsonHelper.confJson(configName).toString()))
        client.log.info("Environment for project: ${response.json.project.projectName} is created")
        response
    }

    @Step
    def createService(replicaNum,
                      volumes = [source: null, target: null ]) {
        message("service creation")
        def json = jsonHelper.serviceJson(replicaNum, volumes)
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        response
    }

    @Step
    def updateService(replicaNum,
                      volumes = [source: null, target: null ]) {
        message("service update")
        def json = jsonHelper.serviceJson(replicaNum, volumes)
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        response
    }

    @Step("Push to registry: {artifact}:{version}")
    def pushToRegistry(configName,
                       artifactName,
                       artifactVersion,
                       artifactLocation = null,
                       credential,
                       artifactory,
                       imageName,
                       registryUrl = null,
                       baseImage,
                       exposePorts = '8081',
                       command, environments, removeAfter){
        message("running artifact2image procedure")
        def json = jsonHelper.artifact2ImageJson(configName, artifactName, artifactVersion, artifactLocation, credential, artifactory, imageName, registryUrl, baseImage, exposePorts, command, environments, removeAfter.toString())
        def response = client.dslFile dslPath(plugin, 'artifact2image'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Artifact2Image procedure is successfully completed.")
        response
    }




}
