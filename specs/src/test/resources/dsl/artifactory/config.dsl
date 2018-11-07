package dsl.artifactory

def names = args.params,
    configName             = names.configName,
    pluginName             = 'EC-Artifactory',
    artifactoryUrl         = names.artifactoryUrl,
    desc                   = 'EC-Arttifactory Configuration without Proxy',
    userName               = names.userName,
    password               = names.password,
    logLevel               = names.logLevel,
    testConnection         = '1'

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        credential: [
                credentialName: configName,
                userName: userName,
                password: password
        ],
        actualParameter: [
                instance: artifactoryUrl,
                config: configName,
                desc: desc,
                credential: configName,
                checkConnection: testConnection,
                debugLevel: logLevel,
                layouts: ''
        ]
)