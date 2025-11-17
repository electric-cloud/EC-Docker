package com.cloudbees.pdk.hen.procedures

import com.cloudbees.pdk.hen.*

//generated
class TestConfiguration extends Procedure {

    static TestConfiguration create(Plugin plugin) {
        return new TestConfiguration(procedureName: 'TestConfiguration', plugin: plugin, credentials: [
            
            'credential': null,
            
        ])
    }


    TestConfiguration flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    TestConfiguration withUser(User user) {
        this.contextUser = user
        return this
    }


    TestConfiguration clone() {
        TestConfiguration cloned = new TestConfiguration(procedureName: 'TestConfiguration', plugin: plugin, credentials: [
                    
                    'credential': null,
                    
                ])
        cloned.parameters = this.parameters.clone()
        return cloned
    }

    //Generated
    
    TestConfiguration checkConnection(boolean checkConnection) {
        this.addParam('checkConnection', checkConnection)
        return this
    }
    
    
    TestConfiguration checkConnectionResource(String checkConnectionResource) {
        this.addParam('checkConnectionResource', checkConnectionResource)
        return this
    }
    
    
    TestConfiguration config(String config) {
        this.addParam('config', config)
        return this
    }
    
    
    TestConfiguration debugLevel(String debugLevel) {
        this.addParam('debugLevel', debugLevel)
        return this
    }
    
    TestConfiguration debugLevel(DebugLevelOptions debugLevel) {
        this.addParam('debugLevel', debugLevel.toString())
        return this
    }
    
    
    TestConfiguration desc(String desc) {
        this.addParam('desc', desc)
        return this
    }
    
    
    TestConfiguration registry(String registry) {
        this.addParam('registry', registry)
        return this
    }
    
    
    
    TestConfiguration credential(String user, String password) {
        this.addCredential('credential', user, password)
        return this
    }

    TestConfiguration credentialReference(String path) {
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