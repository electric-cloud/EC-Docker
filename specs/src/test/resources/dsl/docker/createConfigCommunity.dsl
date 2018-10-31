package dsl.docker

def configName 	   = "dockerConfigCommunity",
    pluginName 	   = 'EC-Docker',
    endpoint 	   = 'tcp://10.200.1.118:2376',
// ca.pem
    cacert     	   = "",
// cert.pem
    cert	   	   = "",
// key.pem
    credential 	   = "",
    desc 	       = 'EC-Docker Configuration without SSL',
    logLevel 	   = '2',
    userName 	   = '',
    testConnection = 'true'

// Create plugin configuration

def keyCred = new RuntimeCredentialImpl()
keyCred.name 		   = 'credential'
keyCred.userName 	   = userName
keyCred.password       = credential

def pluginProjectName = getPlugin(pluginName: pluginName).projectName
runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        actualParameter: [
                config: configName,
                desc: desc,
                endpoint: endpoint,
                logLevel: logLevel,
                testConnection: testConnection,
                cacert: cacert,
                cert: cert,
                credential: 'credential'
        ],
        credential: [
                keyCred
        ]
)