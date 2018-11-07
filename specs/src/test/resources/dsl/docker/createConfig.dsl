package dsl.docker


def params = args.params,
        config = params.config,
        desc = 'EC-Docker Swarm Configuration',
        dockerEndpoint = params.dockerEndpoint,
        username = params.username,
        caPem = params.caPem,
        certPem = params.certPem,
        keyPem = params.keyPem,
        testConnection = params.testConnection,
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