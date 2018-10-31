package com.electriccloud.client.plugin

import com.electriccloud.client.commander.CommanderClient
import com.electriccloud.helpers.enums.LogLevels
import io.qameta.allure.Step

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath

class ArtifactoryClient extends CommanderClient{


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
        def json = jsonHelper.artifactoryConfigJson(configName, artifactoryUrl, userName, password, logLevel.getValue())
        def response = client.dslFile(dslPath(plugin, 'config'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configName} with Artifactory URL ${artifactoryUrl} is successfully created.")
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
        def json = jsonHelper.artifactoryPushJson(configName,
                artifactName,
                artifactDir,
                artClassifier,
                repo,
                repoLayout,
                repoPath,
                organization,
                organizationPath,
                artifactExtension,
                repositoryType, artifactType, artifactVersion)
        def response = client.dslFile(dslPath(plugin, 'publishArtifact'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Artifact is .")
        return response
    }


}
