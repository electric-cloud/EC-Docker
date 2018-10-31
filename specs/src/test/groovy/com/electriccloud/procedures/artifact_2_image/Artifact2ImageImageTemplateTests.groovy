package com.electriccloud.procedures.artifact_2_image

import com.electriccloud.client.api.DockerApi
import com.electriccloud.helpers.objects.Credential
import com.electriccloud.procedures.DockerTestBase
import com.electriccloud.test_data.Artifact2ImageData
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*
import static org.awaitility.Awaitility.*;

@Feature('Artifact2Image')
class Artifact2ImageImageTemplateTests extends DockerTestBase {

    def dockerHubCreds

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerApi = new DockerApi(endpointCommunity, certsPath, false)

        dockerClient.client.deleteProject(projectName)
        dockerClient.deleteConfiguration(configName)
        dockerClient.createConfiguration(configName, endpointCommunity, userName, null, null, null, true, DEBUG)
        dockerClient.createEnvironment(configName)

        dockerHubCreds = new Credential("DockerHub", dockerHubId, dockerHubPass, "test")
        dockerClient.client.createCredential(projectName, dockerHubCreds)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.client.deleteProject(projectName)
        dockerClient.client.deleteArtifact(jettyArtifact)
        dockerClient.client.deleteArtifact(jarArtifact)
        dockerClient.client.deleteArtifact(netArtifact)
        dockerApi.client.rmi("${dockerHubId}/${jettyRepo}")
        dockerApi.client.rmi("${dockerHubId}/${jarRepo}")
        dockerApi.client.rmi("${dockerHubId}/${netRepo}")
        dockerHub.deleteRepository(jettyRepo).deleteRepository(jarRepo).deleteRepository(netRepo)
    }


    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerApi.client.stop(containerId)
        dockerApi.client.rm(containerId)
        dockerApi.client.pruneContainers()
    }



    @Test(groups = "Positive", dataProvider = 'templateTypeData', dataProviderClass = Artifact2ImageData.class)
    @Story('Pushing to DockerHub from local Artifact repo')
    void publishToDockerHubWithDifferentTemplates(artifactName, artifactVersion, artifactDir, fileName, imageName, repository, artifactory, baseImage, ports, cmd, env, removeAfterPush){
        ectoolApi.publishArtifact(artifactName,
                artifactVersion,
                artifactDir,
                fileName)
        def jobId = dockerClient.pushToRegistry(configName,
                artifactName,
                artifactVersion,
                null,
                dockerHubCreds,
                artifactory,
                imageName,
                null,
                baseImage, cmd, env, removeAfterPush).json.jobId
        await("Wait for image to be created in the DockerHub").until {
            dockerHub.getRepo(repository).statusCode() == 200
        }
        def repo = dockerHub.getRepository(repository).json
        def images = dockerApi.client.images().content
        def image = images.find { it.RepoTags.first() == "${imageName}:latest" }
        containerId = dockerApi.client.run(image.Id, containerConfig(image.Id, ports), '', repository).container.content.Id
        def tasks = dockerApi.client.ps().content
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "success"
        assert image.RepoTags.first() == "${imageName}:latest"
        assert containerId == tasks.first().Id
        assert image.Id == tasks.first().ImageID
        assert tasks.first().Names.first() == "/$repository"
        assert tasks.first().Ports.first().PublicPort == 81
        assert repo.user == dockerHub.username
        assert repo.name == repository


    }


}
