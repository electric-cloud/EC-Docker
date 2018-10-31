package com.electriccloud.procedures.deployment

import com.electriccloud.client.api.DockerApi
import com.electriccloud.procedures.DockerTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.await


class DockerTlsDeploymentTests extends DockerTestBase {



    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerApi = new DockerApi(endpointTls, certsPath, true)
        dockerClient.deleteConfiguration(configTls)
        ectoolApi.runDsl('docker', 'createConfigTls')
    }


    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        dockerClient.createEnvironment(configTls)
        dockerClient.createService(2, volumes)
    }


    @AfterMethod(alwaysRun = true)
    void tearDownTes(){
        dockerApi.client.ps().content.each { dockerApi.client.stop(it.Id) }
        dockerApi.client.pruneContainers()
        dockerClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    @Story("Deploy on Tls Docker")
    @Description("Deploy Project-level microservice on Tls Docker")
    void deployProjcetLevelMicroservice() {
        dockerClient.deployService(projectName, serviceName)
        await('Until service is Up and Running').until {
            dockerApi.client.ps().content
                    .find { it.Names.last() == "/${serviceName}" }
                    .State == "running"
        }
        def containers = dockerApi.client.ps().content
        def container = containers.find { it.Names.last() == "/${serviceName}" }
        assert containers.size() == 1
        assert container.Names.last() == "/${serviceName}"
        assert container.Image == "nginx:latest"
        assert container.Ports.last().PublicPort == 81
        assert container.State == "running"
        assert container.Status.contains("Up")
        assert container.Mounts[0].Type == "bind"
        assert container.Mounts[0].Source == '/var/html'
        assert container.Mounts[0].Destination == '/usr/share/nginx/html'
        assert resp.statusCode() == 200
    }



    @Test(groups = "Positive")
    @Story("Deploy on Tls Docker")
    @Description("Update Project-level microservice on Tls Docker with same credentials")
    void updateProjcetLevelMicroserviceWithSmeCreds() {
        dockerClient.deployService(projectName, serviceName)
        dockerClient.createService(1, volumes)
        dockerClient.deployService(projectName, serviceName)
        await('Until service is Up and Running').until {
            dockerApi.client.ps().content
                    .find { it.Names.last() == "/${serviceName}" }
                    .State == "running"
        }
        def containers = dockerApi.client.ps().content
        def container = containers.find { it.Names.last() == "/${serviceName}" }
        assert containers.size() == 1
        assert container.Names.last() == "/${serviceName}"
        assert container.Image == "nginx:latest"
        assert container.Ports.last().PublicPort == 81
        assert container.State == "running"
        assert container.Status.contains("Up")
        assert container.Mounts[0].Type == "bind"
        assert container.Mounts[0].Source == '/var/html'
        assert container.Mounts[0].Destination == '/usr/share/nginx/html'
        assert resp.statusCode() == 200
    }



    @Test(groups = "Positive")
    @Story("Deploy on Tls Docker")
    @Description("Update Project-level microservice on Tls Docker")
    void updateProjcetLevelMicroservice() {
        dockerClient.deployService(projectName, serviceName)
        dockerClient.createService(2, volumes)
        dockerClient.deployService(projectName, serviceName)
        await('Until service is Up and Running').until {
            dockerApi.client.ps().content
                    .find { it.Names.last() == "/${serviceName}" }
                    .State == "running"
        }
        def containers = dockerApi.client.ps().content
        def container = containers.find { it.Names.last() == "/${serviceName}" }
        assert containers.size() == 1
        assert container.Names.last() == "/${serviceName}"
        assert container.Image == "nginx:latest"
        assert container.Ports.last().PublicPort == 81
        assert container.State == "running"
        assert container.Status.contains("Up")
        assert container.Mounts[0].Type == "bind"
        assert container.Mounts[0].Source == '/var/html'
        assert container.Mounts[0].Destination == '/usr/share/nginx/html'
        assert resp.statusCode() == 200
    }





    @Test(groups = "Positive")
    @Feature('Undeploy')
    @Story('Undeploy on Tls Docker')
    @Description("Undeploy Project-level microservice on Tls Docker")
    void undeployProjcetLevelMicroservice(){
        dockerClient.deployService(projectName, serviceName)
        dockerClient.undeployService(projectName, serviceName)
        await('Until size of containers equals: 0').until{
            dockerApi.client.ps().content.size() == 0
        }
        def containers = dockerApi.client.ps().content
        assert containers.size() == 0
    }





}
