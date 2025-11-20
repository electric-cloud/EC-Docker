package com.cloudbees.pdk.hen.procedures

import com.cloudbees.pdk.hen.*

//generated
class RunDockerPull extends Procedure {

    static RunDockerPull create(Plugin plugin) {
        return new RunDockerPull(procedureName: 'runDockerPull', plugin: plugin, )
    }


    RunDockerPull flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RunDockerPull withUser(User user) {
        this.contextUser = user
        return this
    }


    RunDockerPull clone() {
        RunDockerPull cloned = new RunDockerPull(procedureName: 'runDockerPull', plugin: plugin, )
        cloned.parameters = this.parameters.clone()
        return cloned
    }

    //Generated
    
    RunDockerPull config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    RunDockerPull imagename(String imagename) {
        this.addParam('image_name', imagename)
        return this
    }
    
    
    RunDockerPull tag(String tag) {
        this.addParam('tag', tag)
        return this
    }
    
    
    RunDockerPull usesudo(boolean usesudo) {
        this.addParam('use_sudo', usesudo)
        return this
    }
    
    
    
    
}