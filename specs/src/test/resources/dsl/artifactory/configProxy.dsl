package dsl.artifactory

def configName             = "artConfigProxy",
    pluginName             = 'EC-Artifactory',
    artifactoryUrl         = 'http://10.200.1.56:8081/artifactory',
    logLevel               = '1',
    desc                   = 'EC-Artifactory Config via Proxy',
    userName               = 'admin',
    password               = 'changeme',
    testConnection         = '1',
    proxyUrl               = 'http://10.200.1.56:3128',
    proxyUsername          = 'user1',
    proxyPassword          = 'password1'

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        credential: [
                [
                        credentialName: configName,
                        userName: userName,
                        password: password
                ],
                [
                        credentialName: 'proxy_credential',
                        userName: proxyUsername,
                        password: proxyPassword
                ]

        ],
        actualParameter: [
                checkConnection: testConnection,
                config: configName,
                credential: configName,
                debugLevel: logLevel,
                desc: desc,
                http_proxy: proxyUrl,
                instance: artifactoryUrl,
                layouts: '',
                proxy_credential: 'proxy_credential',

        ]
)