package com.cloudbees.pdk.hen.procedures

import com.cloudbees.pdk.hen.*

//generated
class DockerConfig extends Procedure {

    static DockerConfig create(Plugin plugin) {
        return new DockerConfig(procedureName: 'CreateConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    DockerConfig flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    DockerConfig withUser(User user) {
        this.contextUser = user
        return this
    }


    DockerConfig clone() {
        DockerConfig cloned = new DockerConfig(procedureName: 'CreateConfiguration', plugin: plugin, credentials: [
                    
                    'credential': null,
                    
                ])
        cloned.parameters = this.parameters.clone()
        return cloned
    }

    //Generated
    
    DockerConfig checkConnection(boolean checkConnection) {
        this.addParam('checkConnection', checkConnection)
        return this
    }
    
    
    DockerConfig checkConnectionResource(String checkConnectionResource) {
        this.addParam('checkConnectionResource', checkConnectionResource)
        return this
    }
    
    
    DockerConfig config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    DockerConfig debugLevel(String debugLevel) {
        this.addParam('debugLevel', debugLevel)
        return this
    }
    
    DockerConfig debugLevel(DebugLevelOptions debugLevel) {
        this.addParam('debugLevel', debugLevel.toString())
        return this
    }
    
    
    DockerConfig desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    DockerConfig registry(String registry) {
        this.addParam('registry', registry)
        return this
    }
    
    
    
    DockerConfig credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    DockerConfig credentialReference(String path) {
        this.addCredentialReference('credential', path)
        return this
    }
    
    
    enum DebugLevelOptions {
    
    INFO("0"),
    
    DEBUG("1"),
    
    TRACE("2")
    
    private String value
    DebugLevelOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}