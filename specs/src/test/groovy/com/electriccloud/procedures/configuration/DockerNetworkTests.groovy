package com.electriccloud.procedures.configuration

import com.electriccloud.client.api.DockerApi
import com.electriccloud.procedures.DockerTestBase
import groovy.json.JsonBuilder
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class DockerNetworkTests extends DockerTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerApi = new DockerApi(endpointSwarm, certsPath, false)
        dockerClient.deleteConfiguration(configSwarm)
        dockerClient.createConfiguration(configSwarm, endpointCommunity, userName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerApi.client.pruneNetworks()
    }

    @Test(groups = "Positive")
    void createNetwork() {
        def jobId = dockerClient.createNetwork(configSwarm, "my-network").json.jobId
        def network = dockerApi.client.networks().content.find { it.Name == "my-network" }

        println new JsonBuilder(network).toPrettyString()



    }



}
