package com.electriccloud.procedures.deployment

import com.electriccloud.client.api.DockerApi
import com.electriccloud.procedures.DockerTestBase
import groovy.json.JsonBuilder
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.DEBUG
import static org.awaitility.Awaitility.await

class DeploymentTests extends DockerTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerApi = new DockerApi(endpointSwarm, certsPath, false)
        dockerClient.deleteConfiguration(configSwarm)
        dockerClient.createConfiguration(configSwarm, endpointSwarm, userName, null, null, null, true, DEBUG)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        dockerClient.createEnvironment(configSwarm)
        dockerClient.createService(2, volumes)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerClient.removeService(configSwarm, serviceName, "local")
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    @Story('Deploy on Docker-Swarm')
    void deployProjectLevelMicroservice(){
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        await('Wait for services to have status: running').until {
            dockerApi.client.tasks().content.each { it.Status.State == "running" }
        }
        def service = dockerApi.client.inspectService(serviceName).content
        def tasks = dockerApi.client.tasks().content
        assert service.Spec.Name == serviceName
        assert service.Spec.TaskTemplate.ContainerSpec.Image == 'nginx:latest'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Type == 'bind'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Source == '/var/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Target == '/usr/share/nginx/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Env.last() == 'NGINX_PORT=80'
        assert service.Spec.Mode.Replicated.Replicas == 2
        assert tasks.size() == 2
        assert tasks.each { assert it.Spec.ContainerSpec.Image == "nginx:latest" }
        assert tasks.each { assert it.Status.State == "running" }
        assert tasks.each { assert it.Status.Message == "started" }
    }



    @Test(groups = "Positive")
    @Story('Deploy on Docker-Swarm')
    @Description("Update Project-level Microservice on Docker-Swarm with same creds")
    void updateProjectLevelMicroserviceWithTheSameCreds(){
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        dockerClient.createService(2, volumes)
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        await('Wait for services to have status: running').until {
            dockerApi.client.tasks().content.each { it.Status.State == "running" }
        }
        def service = dockerApi.client.inspectService(serviceName).content
        def tasks = dockerApi.client.tasks().content
        assert service.Spec.Name == serviceName
        assert service.Spec.TaskTemplate.ContainerSpec.Image == 'nginx:latest'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Type == 'bind'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Source == '/var/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Target == '/usr/share/nginx/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Env.last() == 'NGINX_PORT=80'
        assert service.Spec.Mode.Replicated.Replicas == 2
        assert tasks.size() == 2
        assert tasks.each { assert it.Spec.ContainerSpec.Image == "nginx:latest" }
        assert tasks.each { assert it.Status.State == "running" }
        assert tasks.each { assert it.Status.Message == "started" }
    }



    @Test(groups = "Positive")
    @Story('Deploy on Docker-Swarm')
    @Description("Update Project-level Microservice on Docker-Swarm")
    void updateProjectLevelMicroservice(){
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        dockerClient.createService(3, volumes)
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        await('Wait for services to have status: running').until {
            dockerApi.client.tasks().content.each { it.Status.State == "running" }
        }
        def service = dockerApi.client.inspectService(serviceName).content
        def tasks = dockerApi.client.tasks().content
        assert service.Spec.Name == serviceName
        assert service.Spec.TaskTemplate.ContainerSpec.Image == 'nginx:latest'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Type == 'bind'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Source == '/var/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Mounts.last().Target == '/usr/share/nginx/html'
        assert service.Spec.TaskTemplate.ContainerSpec.Env.last() == 'NGINX_PORT=80'
        assert service.Spec.Mode.Replicated.Replicas == 3
        assert tasks.size() == 3
        assert tasks.each { assert it.Spec.ContainerSpec.Image == "nginx:latest" }
        assert tasks.each { assert it.Status.State == "running" }
        assert tasks.each { assert it.Status.Message == "started" }
    }




    @Test(groups = "Positive")
    @Feature('Undeploy')
    @Story('Undeploy on Docker-Swarm')
    @Description("Undeploy Project-level Microservice on Docker-Swarm")
    void undeployProjcetLevelMicroservice(){
        dockerClient.deploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        dockerClient.undeploy(projectName, environmentProjectName, environmentName, clusterName, serviceName)
        await('Until size of containers equals: 0').until{
            dockerApi.client.services().content.size() == 0
        }
        def containers = dockerApi.client.ps().content
        def tasks = dockerApi.client.tasks().content
        def services = dockerApi.client.services().content
        assert containers.size() == 0
        assert tasks.size() == 0
        assert services.size() == 0
    }


}
