package com.electriccloud.procedures

import com.electriccloud.client.api.DockerHubApi
import com.electriccloud.client.api.JfrogArtifactoryApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.ArtifactoryClient
import com.electriccloud.client.plugin.DockerClient
import com.electriccloud.listeners.TestListener
import io.qameta.allure.Epic
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Listeners

import java.util.concurrent.TimeUnit
import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.setDefaultTimeout


@Epic('EC-Docker')
@Listeners(TestListener.class)
class DockerTestBase extends NamingTestBase {



    def getHost = { uri -> new URL(uri).host }

    def req = given().relaxedHTTPSValidation()
                    .filters(new RequestLoggingFilter(), new ResponseLoggingFilter())
                    .when()

    def volumes = [ source: '[{"name": "html-content","hostPath": "/var/html"}]',
                    target: '[{"name": "html-content","mountPath": "/usr/share/nginx/html"}]' ]

    def containerConfig = { imageId, port ->
        return ["Image": imageId,
                ExposedPorts: ["${port}/tcp": [:]],
                "HostConfig": ["PortBindings": [
                        "${port}/tcp": [
                                ["HostIp"  : "0.0.0.0",
                                 "HostPort": "81"]]
                ]]]
    }


    @BeforeSuite(alwaysRun = true)
    void setUpCerts(){
        certsPath              = 'src/test/resources/certs'
        caCert                 = System.getenv("DOCKER_CA_CERT").split("\\\\n").join('\n')
        cert                   = System.getenv("DOCKER_CERT").split("\\\\n").join('\n')
        key                    = System.getenv("DOCKER_KEY").split("\\\\n").join('\n')
        def cert1 = new File("${certsPath}/ca.pem")
        def cert2 = new File("${certsPath}/cert.pem")
        def cert3 = new File("${certsPath}/key.pem")
        cert1.write(caCert)
        cert2.write(cert)
        cert3.write(key)
        println "Created Certificates: \n$cert1.text \n$cert2.text \n $cert3.text"
    }

    @BeforeClass(alwaysRun = true)
    void setUpEnv(){
        setDefaultTimeout(20, TimeUnit.SECONDS)
        configName             = 'dockerConfig'
        configSwarm            = 'dockerConfigSwarm'
        configTls              = 'dockerConfigTls'
        configCommunity        = 'dockerConfigCommunity'
        projectName            = 'dockerProj'
        environmentProjectName = 'dockerProj'
        environmentName        = 'docker-environment'
        clusterName            = 'docker-cluster'
        serviceName            = 'nginx-service'
        applicationName        = 'nginx-application'
        containerName          = 'nginx-service'
        userName               = 'flowqe'
        artifactsDir           = 'src/test/resources/artifacts/resources'
        pluginName             = System.getenv("PLUGIN_NAME")
        pluginVersion          = System.getenv("PLUGIN_BUILD_VERSION")
        dockerHubId            = System.getenv("DOCKER_HUB_ID")
        dockerHubPass          = System.getenv("DOCKER_HUB_PASSWORD")
        endpointSwarm          = System.getenv("DOCKER_SWARM_ENDPOINT")
        nodeSwarm              = System.getenv("DOCKER_SWARM_NODE_ENDPOINT")
        endpointTls            = System.getenv("DOCKER_TLS_ENDPOINT")
        endpointCommunity      = System.getenv("DOCKER_COMMUNITY_ENDPOINT")
        artifactoryConfig      = 'artConfig'
        artifactoryUrl         = System.getenv("ARTIFACTORY_URL")
        artifactoryUsername    = System.getenv("ARTIFACTORY_ADMIN_USERNAME")
        artifactoryPassword    = System.getenv("ARTIFACTORY_ADMIN_PASSWORD")

        dockerClient = new DockerClient()
        artifactoryClient = new ArtifactoryClient()
        dockerHub = new DockerHubApi(dockerHubId, dockerHubPass)
        ectoolApi = new EctoolApi(true)
        artifactoryApi = new JfrogArtifactoryApi(artifactoryUrl, artifactoryUsername, artifactoryPassword)


        ectoolApi.ectoolLogin()
    }


}
