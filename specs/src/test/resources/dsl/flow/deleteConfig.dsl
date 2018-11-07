package dsl.flow

def names = args.params,
        pluginName = 'EC-Docker',
        configName = names.configName

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: "/plugins/${pluginProjectName}/project",
        procedureName: "DeleteConfiguration",
        actualParameter: [
                config: configName
        ]
)