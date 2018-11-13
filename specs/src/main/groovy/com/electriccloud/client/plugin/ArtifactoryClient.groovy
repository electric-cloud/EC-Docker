package com.electriccloud.client.plugin

import com.electriccloud.client.commander.CommanderClient
import io.qameta.allure.Step

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath

class ArtifactoryClient extends CommanderClient {


    ArtifactoryClient() {
        this.timeout = 200
        this.plugin = 'artifactory'
    }


    @Step("Create configuration: {configurationName}, {artifactoryUrl}")
    def createConfiguration(configName,
                            artifactoryUrl,
                            userName,
                            password,
                            logLevel = DEBUG) {
        message("creating artifactory config")
        def params = [params: [configName: configName, artifactoryUrl: artifactoryUrl, userName: userName, password: password, logLevel: logLevel.getValue()]]
        def response = client.dslFileMap dslPath(plugin, 'config'), params
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configName} with Artifactory URL ${artifactoryUrl} is successfully created.")
        return response
    }

    @Step("Delete configuration: {confName}")
    def deleteConfiguration(confName) {
        message("removing configuration")
        def response = client.dslFileMap(dslPath(plugin, 'deleteConfig'), [params: [configName: confName]])
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${confName} is successfully deleted.")
        return response
    }

    @Step("Push artifact: {0}")
    def publishArtifact(configName,
                        artifactName,
                        artifactDir,
                        artClassifier,
                        repo,
                        repoLayout,
                        repoPath,
                        organization,
                        organizationPath,
                        artifactExtension,
                        repositoryType, artifactType, artifactVersion){
        message("publishing artifact")
        def params = [params: [
                artifact: artifactName,
                artifactPath: artifactDir,
                classifier: artClassifier,
                config: configName,
                extension: artifactExtension,
                org: organization,
                orgPath: organizationPath,
                repository: repo,
                repositoryLayout: repoLayout,
                repositoryPath: repoPath,
                repoType: repositoryType,
                type: artifactType,
                version: artifactVersion
        ]]
        def response = client.dslFileMap dslPath(plugin, 'publishArtifact'), params
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Artifact is .")
        return response
    }


}
