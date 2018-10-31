package com.electriccloud.procedures.artifact_2_image

import com.electriccloud.client.api.DockerApi
import com.electriccloud.helpers.objects.Artifactory
import com.electriccloud.helpers.objects.Credential
import com.electriccloud.procedures.DockerTestBase
import com.electriccloud.test_data.Artifact2ImageData
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*

class Artifact2ImageArtifactoryTypeTests extends DockerTestBase {

    def dockerHubCreds

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerApi = new DockerApi(endpointCommunity, certsPath, false)
        dockerHubCreds = new Credential("DockerHub", dockerHubId, dockerHubPass, "test")

        dockerClient.client.deleteProject(projectName)
        dockerClient.deleteConfiguration(configName)
        artifactoryClient.deleteConfiguration(artifactoryConfig)
        dockerClient.createConfiguration(configName, endpointCommunity, userName, null, null, null, true, DEBUG)
        artifactoryClient.createConfiguration(artifactoryConfig, artifactoryUrl, artifactoryUsername, artifactoryPassword, DEBUG)

        dockerClient.createEnvironment(configName)
        dockerClient.client.createCredential(projectName, dockerHubCreds)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.client.deleteProject(projectName)
        dockerApi.client.rmi("${dockerHubId}/${jettyRepo}")
        dockerApi.client.rmi("${dockerHubId}/${jarRepo}")
        dockerApi.client.rmi("${dockerHubId}/${netRepo}")
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        dockerApi.client.pruneContainers()
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerApi.client.stop(containerId)
        dockerApi.client.rm(containerId)
        dockerApi.client.pruneContainers()
    }


    @Test(groups = "Positive", dataProvider = 'artifactoryTypeData', dataProviderClass = Artifact2ImageData.class)
    @Story('Pushing to DockerHub from local Artifact repo')
    void publishToDockerHubWithDifferentArtifactoryTypes(artifactFile, artifactory, imageName, repository, baseImage, ports, removeAfterPush){
        artifactoryApi.uploadArtifact(new File("$artifactsDir/$artifactFile"),
                artifactory.repoType,
                artifactory.repoKey,
                artifactory.orgPath,
                artifactory.artifactName,
                artifactory.artifactVersion,
                artifactory.artifactExtension)
        def jobId = dockerClient.pushToRegistry(configName,
                null,
                null,
                null,
                dockerHubCreds,
                artifactory,
                imageName,
                null,
                baseImage, null, null, removeAfterPush).json.jobId
        def repo = dockerHub.getRepository(repository).json
        def images = dockerApi.client.images().content
        def image = images.find { it.RepoTags.first() == "${imageName}:latest" }
        def containerId = dockerApi.client.run(image.Id, containerConfig(image.Id, ports),
                '', 'hello-world-app').container.content.Id
        def tasks = dockerApi.client.ps().content
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "success"
        assert image.RepoTags.first() == "${imageName}:latest"
        assert containerId == tasks.first().Id
        assert image.Id == tasks.first().ImageID
        assert tasks.first().Names.first() == '/hello-world-app'
        assert repo.user == dockerHub.username
        assert repo.name == repository
        dockerHub.deleteRepository(repository)
    }



}
