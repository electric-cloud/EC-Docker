package com.cloudbees.pdk.hen

import com.cloudbees.pdk.hen.procedures.*
import com.cloudbees.pdk.hen.Plugin

import static com.cloudbees.pdk.hen.Utils.env

class Docker extends Plugin {

    static Docker create() {
        Docker plugin = new Docker(name: 'EC-Docker')
        plugin.configure(plugin.config)
        return plugin
    }
    static Docker createWithoutConfig() {
        Docker plugin = new Docker(name: 'EC-Docker')
        return plugin
    }

    //user-defined after boilerplate was generated, default parameters setup
    DockerConfig config = DockerConfig
        .create(this)
        //.parameter(value) add parameters here


    EditConfiguration editConfiguration = EditConfiguration.create(this)

    RunDockerBuild runDockerBuild = RunDockerBuild.create(this)

    RunDockerPull runDockerPull = RunDockerPull.create(this)

    RunDockerRun runDockerRun = RunDockerRun.create(this)

}