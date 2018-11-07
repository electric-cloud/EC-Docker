package dsl.artifactory

def names = args.params,
    pluginName = 'EC-Artifactory',
    configName = names.configName

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: "/plugins/${pluginProjectName}/project",
        procedureName: "DeleteConfiguration",
        actualParameter: [
                config: configName
        ]
)