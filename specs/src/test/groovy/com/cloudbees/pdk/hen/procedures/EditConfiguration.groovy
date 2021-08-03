package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class EditConfiguration extends Procedure {

    static EditConfiguration create(Plugin plugin) {
        return new EditConfiguration(procedureName: 'EditConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    EditConfiguration flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    EditConfiguration withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    EditConfiguration config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    EditConfiguration debugLevel(String debugLevel) {
        this.addParam('debugLevel', debugLevel)
        return this
    }
    
    EditConfiguration debugLevel(DebugLevelOptions debugLevel) {
        this.addParam('debugLevel', debugLevel.toString())
        return this
    }
    
    
    EditConfiguration desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    EditConfiguration registry(String registry) {
        this.addParam('registry', registry)
        return this
    }
    
    
    
    EditConfiguration credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    EditConfiguration credentialReference(String path) {
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