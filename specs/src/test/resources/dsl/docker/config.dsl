package dsl.docker


def names = args.names,
        config = names.config,
        desc = 'EC-Docker Swarm Configuration',
        dockerEndpoint = names.dockerEndpoint,
        username = names.username,
        caPem = names.caPem,
        certPem = names.certPem,
        keyPem = names.keyPem,
        testConnection = names.testConnection,
        logLevel = '1'


def configName 	   = config,
    pluginName 	   = 'EC-Docker',
    endpoint 	   = dockerEndpoint,
    userName 	   = username,
// ca.pem
    cacert     	   = caPem,
// cert.pem
    cert	   	   = certPem,
// key.pem
    credential 	   = keyPem

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