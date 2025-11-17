package com.cloudbees.pdk.hen.procedures

import com.cloudbees.pdk.hen.*

//generated
class RunDockerBuild extends Procedure {

    static RunDockerBuild create(Plugin plugin) {
        return new RunDockerBuild(procedureName: 'runDockerBuild', plugin: plugin, )
    }


    RunDockerBuild flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RunDockerBuild withUser(User user) {
        this.contextUser = user
        return this
    }


    RunDockerBuild clone() {
        RunDockerBuild cloned = new RunDockerBuild(procedureName: 'runDockerBuild', plugin: plugin, )
        cloned.parameters = this.parameters.clone()
        return cloned
    }

    //Generated
    
    RunDockerBuild buildpath(String buildpath) {
        this.addParam('build_path', buildpath)
        return this
    }
    
    
    RunDockerBuild config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    RunDockerBuild usesudo(boolean usesudo) {
        this.addParam('use_sudo', usesudo)
        return this
    }
    
    
    
    
}