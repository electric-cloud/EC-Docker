package com.cloudbees.pdk.hen.procedures

import com.cloudbees.pdk.hen.*

//generated
class RunDockerRun extends Procedure {

    static RunDockerRun create(Plugin plugin) {
        return new RunDockerRun(procedureName: 'runDockerRun', plugin: plugin, )
    }


    RunDockerRun flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RunDockerRun withUser(User user) {
        this.contextUser = user
        return this
    }


    RunDockerRun clone() {
        RunDockerRun cloned = new RunDockerRun(procedureName: 'runDockerRun', plugin: plugin, )
        cloned.parameters = this.parameters.clone()
        return cloned
    }

    //Generated
    
    RunDockerRun additionaloptions(String additionaloptions) {
        this.addParam('additional_options', additionaloptions)
        return this
    }
    
    
    RunDockerRun commandwithargs(String commandwithargs) {
        this.addParam('command_with_args', commandwithargs)
        return this
    }
    
    
    RunDockerRun config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    RunDockerRun containerlinks(String containerlinks) {
        this.addParam('container_links', containerlinks)
        return this
    }
    
    
    RunDockerRun containername(String containername) {
        this.addParam('container_name', containername)
        return this
    }
    
    
    RunDockerRun detachedmode(boolean detachedmode) {
        this.addParam('detached_mode', detachedmode)
        return this
    }
    
    
    RunDockerRun entrypoint(String entrypoint) {
        this.addParam('entrypoint', entrypoint)
        return this
    }
    
    
    RunDockerRun imagename(String imagename) {
        this.addParam('image_name', imagename)
        return this
    }
    
    
    RunDockerRun privileged(boolean privileged) {
        this.addParam('privileged', privileged)
        return this
    }
    
    
    RunDockerRun publishallports(boolean publishallports) {
        this.addParam('publish_all_ports', publishallports)
        return this
    }
    
    
    RunDockerRun publishedports(String publishedports) {
        this.addParam('published_ports', publishedports)
        return this
    }
    
    
    RunDockerRun usesudo(boolean usesudo) {
        this.addParam('use_sudo', usesudo)
        return this
    }
    
    
    RunDockerRun workingdir(String workingdir) {
        this.addParam('working_dir', workingdir)
        return this
    }
    
    
    
    
}