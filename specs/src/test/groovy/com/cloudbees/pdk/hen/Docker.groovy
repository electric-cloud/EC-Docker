package com.cloudbees.pdk.hen

import com.cloudbees.pdk.hen.procedures.*
import com.cloudbees.pdk.hen.Plugin

import static com.cloudbees.pdk.hen.Utils.env

class Docker extends Plugin {

    static String DOCKER_AGENT_HOST = env('DOCKER_AGENT_HOST', 'docker')
    static int DOCKER_AGENT_PORT = env('KUBECTL_AGENT_PORT', '7808').toInteger()
    static String DOCKERHUB_SPECS_USERNAME = env('DOCKERHUB_SPECS_USERNAME')
    static String DOCKERHUB_SPECS_PASSWORD = env('DOCKERHUB_SPECS_PASSWORD')
    static String resourceName = 'docker'


    static Docker create() {
        ServerHandler.getInstance().setupResource(resourceName, DOCKER_AGENT_HOST, DOCKER_AGENT_PORT as int)
        Docker plugin = new Docker(name: 'EC-Docker', defaultResource: resourceName)
        plugin.configure(plugin.config)
        return plugin
    }
    static Docker createWithoutConfig() {
        Docker plugin = new Docker(name: 'EC-Docker', defaultResource: resourceName)
        return plugin
    }

    //user-defined after boilerplate was generated, default parameters setup
    DockerConfig config = DockerConfig
        .create(this)
        .debugLevel(DockerConfig.DebugLevelOptions.DEBUG)


    EditConfiguration editConfiguration = EditConfiguration.create(this)

    RunDockerBuild runDockerBuild = RunDockerBuild.create(this)

    RunDockerPull runDockerPull = RunDockerPull.create(this)

    RunDockerRun runDockerRun = RunDockerRun.create(this)

    TestConfiguration testConfiguration = TestConfiguration.create(this)

}