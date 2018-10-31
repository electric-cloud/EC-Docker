package com.electriccloud.procedures

import com.electriccloud.client.api.DockerApi
import com.electriccloud.client.api.DockerHubApi
import com.electriccloud.client.api.JfrogArtifactoryApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.ArtifactoryClient
import com.electriccloud.client.plugin.DockerClient
import com.electriccloud.helpers.objects.Credential

class NamingTestBase {

    // Plugin and Commander variables
    public static String pluginName
    public static String pluginVersion
    public static String userName
    public static String configName
    public static String configSwarm
    public static String configTls
    public static String configCommunity
    public static String projectName
    public static String environmentProjectName
    public static String environmentName
    public static String clusterName
    public static String serviceName
    public static String applicationName
    public static String containerName
    // Docker Environment variables
    public static String endpointSwarm
    public static String nodeSwarm
    public static String endpointTls
    public static String endpointCommunity
    public static String dockerHubId
    public static String dockerHubPass
    public static String caCert
    public static String cert
    public static String key
    public static String certsPath
    // Artifactory settings
    public static String artifactsDir
    public static String artifactoryUrl
    public static String artifactoryUsername
    public static String artifactoryPassword
    public static String artifactoryConfig
    public static String jarArtifact     = 'hello-world:jar'
    public static String jarRepo         = 'hello-world-jar'
    public static String jettyArtifact   = 'hello-world:jetty'
    public static String jettyRepo       = 'hello-world-jetty'
    public static String netArtifact     = 'hello-world:net'
    public static String netRepo         = 'hello-world-net'
    public static String containerId
    DockerClient dockerClient
    DockerApi dockerApi
    DockerHubApi dockerHub
    EctoolApi ectoolApi
    JfrogArtifactoryApi artifactoryApi
    ArtifactoryClient artifactoryClient

}
