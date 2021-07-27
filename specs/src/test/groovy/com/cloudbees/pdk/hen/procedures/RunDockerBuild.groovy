package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
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

    //Generated
    
    RunDockerBuild buildpath(String buildpath) {
        this.addParam('build_path', buildpath)
        return this
    }
    
    
    RunDockerBuild usesudo(boolean usesudo) {
        this.addParam('use_sudo', usesudo)
        return this
    }
    
    
    
    
}