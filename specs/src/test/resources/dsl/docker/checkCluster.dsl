package dsl.docker

def names = args.params,
    config = names.config,
    resource = names.resource


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Check Cluster',
        resourceName: resource,
        actualParameter: [
                config: config
        ]
)
