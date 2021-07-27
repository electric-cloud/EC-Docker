package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
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

    //Generated
    
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